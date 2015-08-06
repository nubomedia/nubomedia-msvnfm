package org.project.openbaton.vnfm.core;

import javassist.NotFoundException;
import org.project.openbaton.catalogue.mano.common.AutoScalePolicy;
import org.project.openbaton.catalogue.mano.common.ConnectionPoint;
import org.project.openbaton.catalogue.mano.descriptor.VNFComponent;
import org.project.openbaton.catalogue.mano.descriptor.VNFDConnectionPoint;
import org.project.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.project.openbaton.catalogue.mano.record.Status;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.Action;
import org.project.openbaton.catalogue.nfvo.CoreMessage;
import org.project.openbaton.clients.exceptions.VimDriverException;
import org.project.openbaton.clients.interfaces.ClientInterfaces;
import org.project.openbaton.common.vnfm_sdk.utils.UtilsJMS;
import org.project.openbaton.vnfm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.naming.NamingException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Created by mpa on 07.07.15.
 */
@Service
@Scope
public class ElasticityManagement {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected ResourceManagement resourceManagement;

    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    Map<VirtualNetworkFunctionRecord, Set<ScheduledFuture>> tasks;

    @PostConstruct
    private void init() {
        tasks = new HashMap<VirtualNetworkFunctionRecord, Set<ScheduledFuture>>();
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.setPoolSize(10);
        this.taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        this.taskScheduler.initialize();
    }

    public void activate(VirtualNetworkFunctionRecord vnfr) {
        log.debug("Activating Elasticity for vnfr " + vnfr.getId());
        tasks.put(vnfr, new HashSet<ScheduledFuture>());
        for (AutoScalePolicy policy : vnfr.getAuto_scale_policy()) {
            ElasticityTask elasticityTask = new ElasticityTask();
            elasticityTask.init(vnfr, policy);
            beanFactory.autowireBean(elasticityTask);
            //taskExecutor.execute(elasticityTask);
            ScheduledFuture scheduledFuture = taskScheduler.scheduleAtFixedRate(elasticityTask, policy.getPeriod() * 1000);
            tasks.get(vnfr).add(scheduledFuture);
        }
        log.debug("Activated Elasticity for vnfr " + vnfr.getId());
    }

    public void deactivate(VirtualNetworkFunctionRecord vnfr) {
        log.debug("Deactivating Elasticity for vnfr " + vnfr.getId());
        for (ScheduledFuture scheduledFuture : tasks.get(vnfr)) {
            scheduledFuture.cancel(false);
        }
        log.debug("Deactivated Elasticity for vnfr " + vnfr.getId());
        vnfr.setStatus(Status.ACTIVE);
    }

    public void scaleUp(VirtualNetworkFunctionRecord vnfr, AutoScalePolicy autoScalePolicy) {
        log.debug("Scaling up vnfr " + vnfr.getId());
        Set<VirtualDeploymentUnit> consideredVDUs = new HashSet<VirtualDeploymentUnit>();
        for (VirtualDeploymentUnit vdu : vnfr.getVdu()) {
            if (consideredVDUs.contains(vdu)) {
                continue;
            }
            int leftInstances = vdu.getScale_in_out();
            for (VirtualDeploymentUnit tmpVDU : vnfr.getVdu()) {
                if (tmpVDU.getVimInstance().getName().equals(vdu.getVimInstance().getName())) {
                    consideredVDUs.add(tmpVDU);
                    leftInstances--;
                }
            }
            if (leftInstances<=0) {
                log.debug("Maximum number of instances are reached on VimInstance " + vdu.getVimInstance());
                break;
            }
            VirtualDeploymentUnit newVDU = new VirtualDeploymentUnit();
            newVDU.setVimInstance(vdu.getVimInstance());
            newVDU.setVm_image(vdu.getVm_image());
            newVDU.setVnfc(new HashSet<VNFComponent>());
            for (VNFComponent vnfc : vdu.getVnfc()) {
                VNFComponent newVnfc = new VNFComponent();
                newVnfc.setConnection_point(new HashSet<VNFDConnectionPoint>());
                for (VNFDConnectionPoint vnfdCP : vnfc.getConnection_point()) {
                    VNFDConnectionPoint newVnfdCP = new VNFDConnectionPoint();
                    newVnfdCP.setName(vnfdCP.getName());
                    newVnfdCP.setType(vnfdCP.getType());
                    newVnfdCP.setExtId(vnfdCP.getExtId());
                    newVnfdCP.setVirtual_link_reference(vnfdCP.getVirtual_link_reference());
                    newVnfc.getConnection_point().add(newVnfdCP);
                }
                newVDU.getVnfc().add(newVnfc);
            }
            newVDU.setComputation_requirement(vdu.getComputation_requirement());
            newVDU.setHigh_availability(vdu.getHigh_availability());
            newVDU.setMonitoring_parameter(vdu.getMonitoring_parameter());
            newVDU.setLifecycle_event(vdu.getLifecycle_event());
            newVDU.setScale_in_out(vdu.getScale_in_out());
            newVDU.setVdu_constraint(vdu.getVdu_constraint());
            newVDU.setVirtual_memory_resource_element(vdu.getVirtual_memory_resource_element());
            newVDU.setVirtual_network_bandwidth_resource(vdu.getVirtual_network_bandwidth_resource());
            try {
                resourceManagement.allocate(vnfr, newVDU);
            } catch (VimDriverException e) {
                log.error("Cannot launch VDU on cloud environment", e);
                try {
                    CoreMessage coreMessage = new CoreMessage();
                    coreMessage.setAction(Action.ERROR);
                    coreMessage.setPayload(vnfr);
                    UtilsJMS.sendToQueue(coreMessage, "vnfm-core-actions");
                } catch (NamingException exc) {
                    log.error(exc.getMessage(), exc);
                } catch (JMSException exc) {
                    log.error(exc.getMessage(), exc);
                }
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            vnfr.getVdu().add(newVDU);
            log.debug("Scaled up vnfr " + vnfr.getId());
            return;
        }
    }

    public void scaleDown(VirtualNetworkFunctionRecord vnfr, AutoScalePolicy autoScalePolicy) throws NotFoundException {
        log.debug("Scaling down vnfr " + vnfr.getId());
        if (vnfr.getVdu().size() > 1 && vnfr.getVdu().iterator().hasNext()) {
            VirtualDeploymentUnit vdu = vnfr.getVdu().iterator().next();
            resourceManagement.release(vnfr, vdu);
            vnfr.getVdu().remove(vdu);
        } else {
            log.warn("Cannot terminate the last VDU.");
        }
    }

    public List<Integer> getRawMeasurementResults(VirtualNetworkFunctionRecord vnfr, String metric) {
        List<Integer> measurementResults = new ArrayList<Integer>();
        log.debug("Getting all measurement results for vnfr " + vnfr.getId() + " on metric " + metric + ".");
        for (VirtualDeploymentUnit vdu : vnfr.getVdu()) {
            log.debug("Getting measurement result for vdu " + vdu.getId() + " on metric " + metric + ".");
            int measurementResult = resourceManagement.getMeasurementResults(vdu, metric);
            measurementResults.add(measurementResult);
            log.debug("Got measurement result for vdu " + vdu.getId() + " on metric " + metric + " -> " + measurementResult + ".");
        }
        log.debug("Got all measurement results for vnfr " + vnfr.getId() + " on metric " + metric + " -> " + measurementResults + ".");
        return measurementResults;
    }

    public double calculateMeasurementResult(AutoScalePolicy autoScalePolicy, List<Integer> measurementResults) {
        double result;
        switch (autoScalePolicy.getStatistic()) {
            case "avg":
                int sum = 0;
                for (Integer measurementResult : measurementResults) {
                    sum += measurementResult;
                }
                result = sum / measurementResults.size();
                break;
            case "min":
                result = Collections.min(measurementResults);
                break;
            case "max":
                result = Collections.max(measurementResults);
                break;
            default:
                result = -1;
                break;
        }
        return result;
    }

    public boolean triggerAction(AutoScalePolicy autoScalePolicy, double result) {
        switch (autoScalePolicy.getComparisonOperator()) {
            case ">":
                if (result > autoScalePolicy.getThreshold()) {
                    return true;
                }
                break;
            case "<":
                if (result < autoScalePolicy.getThreshold()) {
                    return true;
                }
                break;
            case "=":
                if (result == autoScalePolicy.getThreshold()) {
                    return true;
                }
                break;
            default:
                return false;
        }
        return false;
    }

    public void executeAction(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, AutoScalePolicy autoScalePolicy) {
        try {
            switch (autoScalePolicy.getAction()) {
                case "scaleup":
                    scaleUp(virtualNetworkFunctionRecord, autoScalePolicy);
                    break;
                case "scaledown":
                    scaleDown(virtualNetworkFunctionRecord, autoScalePolicy);
                    break;
            }
        } catch (NotFoundException e) {
            log.error(e.getMessage());
        }
    }
}

class ElasticityTask implements Runnable {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ElasticityManagement elasticityManagement;

    private static VirtualNetworkFunctionRecord vnfr;

    private AutoScalePolicy autoScalePolicy;

    private String name;

    public void init(VirtualNetworkFunctionRecord vnfr, AutoScalePolicy autoScalePolicy) {
        this.vnfr = vnfr;
        this.autoScalePolicy = autoScalePolicy;
        this.name = "ElasticityTask#" + vnfr.getId();
    }

    @Override
    public void run() {
        log.debug("Check if scaling is needed.");
        try {
            List<Integer> measurementResults = elasticityManagement.getRawMeasurementResults(vnfr, autoScalePolicy.getMetric());
            double finalResult = elasticityManagement.calculateMeasurementResult(autoScalePolicy, measurementResults);
            log.debug("Final measurement result on vnfr " + vnfr.getId() + " on metric " + autoScalePolicy.getMetric() + " with statistic " + autoScalePolicy.getStatistic() + " is " + finalResult + " " + measurementResults );
            if (checkStatus() == true && elasticityManagement.triggerAction(autoScalePolicy, finalResult)) {
                setStatus(Status.SCALING);
                Utils.sendToCore(vnfr, Action.SCALING);
                log.debug("Executing scaling action of AutoScalePolicy with id " + autoScalePolicy.getId());
                elasticityManagement.executeAction(vnfr, autoScalePolicy);
                log.debug("Starting cooldown period (" + autoScalePolicy.getCooldown() + "s) for AutoScalePolicy with id: " + autoScalePolicy.getId());
                Thread.sleep(autoScalePolicy.getCooldown() * 1000);
                log.debug("Finished cooldown period (" + autoScalePolicy.getCooldown() + "s) for AutoScalePolicy with id: " + autoScalePolicy.getId());
                setStatus(Status.ACTIVE);
                if (autoScalePolicy.getAction().equals("scaleup")) {
                    Utils.sendToCore(vnfr, Action.SCALE_UP_FINISHED);
                } else if (autoScalePolicy.getAction().equals("scaledown")) {
                    Utils.sendToCore(vnfr, Action.SCALE_DOWN_FINISHED);
                }
            } else {
                log.debug("Scaling of AutoScalePolicy with id " + autoScalePolicy.getId() + " is not executed");
            }
            log.debug("Starting sleeping period (" + autoScalePolicy.getPeriod() + "s) for AutoScalePolicy with id: " + autoScalePolicy.getId());
        } catch (InterruptedException e) {
            log.warn("ElasticityTask was interrupted");
        }
    }

    private synchronized boolean checkStatus() {
        Collection<Status> nonBlockingStatus = new HashSet<Status>();
        nonBlockingStatus.add(Status.ACTIVE);
        nonBlockingStatus.add(Status.INITIAILZED);
        if (nonBlockingStatus.contains(this.vnfr.getStatus())) {
            return true;
        } else {
            return false;
        }
    }

    private synchronized boolean setStatus(Status status) {
        log.debug("Set status of vnfr " + vnfr.getId() + " to " + status.name());
        vnfr.setStatus(status);
        return true;
    }
}
