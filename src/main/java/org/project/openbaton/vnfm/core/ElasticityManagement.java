package org.project.openbaton.vnfm.core;

import javassist.NotFoundException;
import org.project.openbaton.catalogue.mano.common.AutoScalePolicy;
import org.project.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.project.openbaton.catalogue.mano.record.Status;
import org.project.openbaton.catalogue.mano.record.VNFCInstance;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.Action;
import org.project.openbaton.catalogue.nfvo.Item;
import org.project.openbaton.clients.exceptions.VimDriverException;
import org.project.openbaton.exceptions.VimException;
import org.project.openbaton.monitoring.interfaces.ResourcePerformanceManagement;
import org.project.openbaton.nfvo.plugin.utils.PluginBroker;
import org.project.openbaton.nfvo.vim_interfaces.resource_management.ResourceManagement;
import org.project.openbaton.vnfm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

/**
 * Created by mpa on 07.07.15.
 */
@Service
@Scope
public class ElasticityManagement {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    protected ResourceManagement resourceManagement;

    protected ResourcePerformanceManagement monitor;

    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    private Map<String, Set<ScheduledFuture>> tasks;

    @Autowired
    private ConfigurableApplicationContext context;

    /**
     * Vim must be initialized only after the registry is up and plugin registered
     */
    public void initilizeVim(){
        PluginBroker<ResourcePerformanceManagement> pluginBroker = new PluginBroker<>();
        try {
            this.monitor = pluginBroker.getPlugin("monitoring-plugin", 19345);
        } catch (RemoteException e) {
            log.error(e.getLocalizedMessage());
        } catch (NotBoundException e) {
            log.error(e.getLocalizedMessage());
        }
        resourceManagement = (ResourceManagement) context.getBean("openstackVIM", "openstack-media-server", 19345);
    }

    @PostConstruct
    private void init() {
        tasks = new HashMap<>();
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.setPoolSize(10);
        this.taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        this.taskScheduler.initialize();
    }

    public void activate(VirtualNetworkFunctionRecord vnfr) {
        log.debug("Activating Elasticity for vnfr " + vnfr.getId());
        tasks.put(vnfr.getId(), new HashSet<ScheduledFuture>());
        for (AutoScalePolicy policy : vnfr.getAuto_scale_policy()) {
            ElasticityTask elasticityTask = new ElasticityTask();
            elasticityTask.init(vnfr, policy);
            beanFactory.autowireBean(elasticityTask);
            //taskExecutor.execute(elasticityTask);
            ScheduledFuture scheduledFuture = taskScheduler.scheduleAtFixedRate(elasticityTask, policy.getPeriod() * 1000);
            tasks.get(vnfr.getId()).add(scheduledFuture);
        }
        log.debug("Activated Elasticity for vnfr " + vnfr.getId());
    }

    public void deactivate(VirtualNetworkFunctionRecord vnfr) {
        log.debug("Deactivating Elasticity for vnfr " + vnfr.getId());
        for (ScheduledFuture scheduledFuture : tasks.get(vnfr.getId())) {
            scheduledFuture.cancel(false);
        }
        log.debug("Deactivated Elasticity for vnfr " + vnfr.getId());
    }

    public void scaleUp(VirtualNetworkFunctionRecord vnfr, AutoScalePolicy autoScalePolicy) {
        log.debug("Scaling up vnfr " + vnfr.getId());
        for (VirtualDeploymentUnit vdu : vnfr.getVdu()) {
            if (vdu.getVnfc_instance().size() < vdu.getScale_in_out()) {
                if (vdu.getVnfc().iterator().hasNext()) {
                    try {
                        resourceManagement.allocate(vdu, vnfr, vdu.getVnfc().iterator().next());
                        log.debug("Scaled up vnfr " + vnfr.getId());
                    } catch (VimDriverException e) {
                        log.error(e.getMessage(), e);
                    } catch (VimException e) {
                        log.error(e.getMessage(), e);
                    }
                    return;
                }
            } else {
                continue;
            }
//        Set<VirtualDeploymentUnit> consideredVDUs = new HashSet<VirtualDeploymentUnit>();
//        for (VirtualDeploymentUnit vdu : vnfr.getVdu()) {
//            if (consideredVDUs.contains(vdu)) {
//                continue;
//            }
//            int leftInstances = vdu.getScale_in_out();
//            for (VirtualDeploymentUnit tmpVDU : vnfr.getVdu()) {
//                if (tmpVDU.getVimInstance().getName().equals(vdu.getVimInstance().getName())) {
//                    consideredVDUs.add(tmpVDU);
//                    leftInstances--;
//                }
//            }
//            if (leftInstances <= 0) {
//                log.debug("Maximum number of instances are reached on VimInstance " + vdu.getVimInstance());
//                break;
//            }
//            VirtualDeploymentUnit newVDU = new VirtualDeploymentUnit();
//            newVDU.setVimInstance(vdu.getVimInstance());
//            newVDU.setVm_image(vdu.getVm_image());
//            newVDU.setVnfc(new HashSet<VNFComponent>());
//            for (VNFComponent vnfc : vdu.getVnfc()) {
//                VNFComponent newVnfc = new VNFComponent();
//                newVnfc.setConnection_point(new HashSet<VNFDConnectionPoint>());
//                for (VNFDConnectionPoint vnfdCP : vnfc.getConnection_point()) {
//                    VNFDConnectionPoint newVnfdCP = new VNFDConnectionPoint();
//                    newVnfdCP.setName(vnfdCP.getName());
//                    newVnfdCP.setType(vnfdCP.getType());
//                    newVnfdCP.setExtId(vnfdCP.getExtId());
//                    newVnfdCP.setVirtual_link_reference(vnfdCP.getVirtual_link_reference());
//                    newVnfc.getConnection_point().add(newVnfdCP);
//                }
//                newVDU.getVnfc().add(newVnfc);
//            }
//            newVDU.setComputation_requirement(vdu.getComputation_requirement());
//            newVDU.setHigh_availability(vdu.getHigh_availability());
//            newVDU.setMonitoring_parameter(vdu.getMonitoring_parameter());
//            newVDU.setLifecycle_event(vdu.getLifecycle_event());
//            newVDU.setScale_in_out(vdu.getScale_in_out());
//            newVDU.setVdu_constraint(vdu.getVdu_constraint());
//            newVDU.setVirtual_memory_resource_element(vdu.getVirtual_memory_resource_element());
//            newVDU.setVirtual_network_bandwidth_resource(vdu.getVirtual_network_bandwidth_resource());
//            try {
//                try {
//                    //TODO Wait until launching is finished
//                    resourceManagement.allocate(vnfr, newVDU, false).get();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                } catch (ExecutionException e) {
//                    e.printStackTrace();
//                }
//            } catch (VimDriverException e) {
//                log.error("Cannot launch VDU on cloud environment", e);
//                try {
//                    CoreMessage coreMessage = new CoreMessage();
//                    coreMessage.setAction(Action.ERROR);
//                    coreMessage.setVirtualNetworkFunctionRecord(vnfr);
//                    UtilsJMS.sendToQueue(coreMessage, "vnfm-core-actions");
//                } catch (NamingException exc) {
//                    log.error(exc.getMessage(), exc);
//                } catch (JMSException exc) {
//                    log.error(exc.getMessage(), exc);
//                }
//            } catch (NotFoundException e) {
//                e.printStackTrace();
//            }
//            vnfr.getVdu().add(newVDU);
//            log.debug("Scaled up vnfr " + vnfr.getId());
//            return;
        }
    }

    public void scaleDown(VirtualNetworkFunctionRecord vnfr, AutoScalePolicy autoScalePolicy) throws NotFoundException, VimException {
        log.debug("Scaling down vnfr " + vnfr.getId());
        for (VirtualDeploymentUnit vdu : vnfr.getVdu()) {
            if (vdu.getVnfc_instance().size() > 1 && vdu.getVnfc_instance().iterator().hasNext()) {
                resourceManagement.release(vdu.getVnfc_instance().iterator().next(), vdu.getVimInstance());
                vnfr.getVdu().remove(vdu);
                log.debug("Scaled down vnfr " + vnfr.getId());
            } else {
                log.warn("Cannot terminate the last VDU.");
            }
        }
    }

    public List<Item> getRawMeasurementResults(VirtualNetworkFunctionRecord vnfr, String metric, String period) {
        List<Item> measurementResults = new ArrayList<Item>();
        log.debug("Getting all measurement results for vnfr " + vnfr.getId() + " on metric " + metric + ".");
        for (VirtualDeploymentUnit vdu : vnfr.getVdu()) {
            for (VNFCInstance vnfcInstance : vdu.getVnfc_instance()) {
                log.debug("Getting measurement result for VNFCInstance " + vdu.getId() + " on metric " + metric + ".");
                Item measurementResult = monitor.getMeasurementResults(vnfcInstance, metric, period);
                measurementResults.add(measurementResult);
                log.debug("Got measurement result for VNFCInstance " + vnfcInstance.getId() + " on metric " + metric + " -> " + measurementResult + ".");
            }
        }
        log.debug("Got all measurement results for vnfr " + vnfr.getId() + " on metric " + metric + " -> " + measurementResults + ".");
        return measurementResults;
    }

    public double calculateMeasurementResult(AutoScalePolicy autoScalePolicy, List<Item> measurementResults) {
        double result;
        List<Double> consideredResults = new ArrayList<>();
        for (Item measurementResult : measurementResults) {
            consideredResults.add(Double.parseDouble(measurementResult.getValue()));
        }
        switch (autoScalePolicy.getStatistic()) {
            case "avg":
                double sum = 0;
                for (Double consideredResult : consideredResults) {
                    sum += consideredResult;
                }
                result = sum / measurementResults.size();
                break;
            case "min":
                result = Collections.min(consideredResults);
                break;
            case "max":
                result = Collections.max(consideredResults);
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
        } catch (VimException e) {
            log.error(e.getMessage());
        }
    }

    public boolean checkFeasibility(VirtualNetworkFunctionRecord vnfr, AutoScalePolicy autoScalePolicy) {
        if (autoScalePolicy.getAction().equals("scaleup")) {
            for (VirtualDeploymentUnit vdu : vnfr.getVdu()) {
                if (vdu.getVnfc_instance().size() < vdu.getScale_in_out()) {
                    return true;
                }
            }
            log.debug("Maximum number of instances are reached on all VimInstances");
            return false;
        } else if (autoScalePolicy.getAction().equals("scaledown")) {
            for (VirtualDeploymentUnit vdu : vnfr.getVdu()) {
                if (vdu.getVnfc_instance().size() > 1) {
                    return true;
                }
            }
            log.warn("Cannot terminate the last VDU.");
            return false;
        }
        return true;
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
            List<Item> measurementResults = elasticityManagement.getRawMeasurementResults(vnfr, autoScalePolicy.getMetric(), Integer.toString(autoScalePolicy.getPeriod()));
            double finalResult = elasticityManagement.calculateMeasurementResult(autoScalePolicy, measurementResults);
            log.debug("Final measurement result on vnfr " + vnfr.getId() + " on metric " + autoScalePolicy.getMetric() + " with statistic " + autoScalePolicy.getStatistic() + " is " + finalResult + " " + measurementResults);
            if (elasticityManagement.triggerAction(autoScalePolicy, finalResult) && elasticityManagement.checkFeasibility(vnfr, autoScalePolicy) && setStatus(Status.SCALING) == true) {
                //setStatus(Status.SCALING);
                Utils.sendToCore(vnfr, Action.SCALING);
                log.debug("Executing scaling action of AutoScalePolicy with id " + autoScalePolicy.getId());
                elasticityManagement.executeAction(vnfr, autoScalePolicy);
                if (autoScalePolicy.getAction().equals("scaleup")) {
                    Utils.sendToCore(vnfr, Action.SCALE_OUT_FINISHED);
                } else if (autoScalePolicy.getAction().equals("scaledown")) {
                    Utils.sendToCore(vnfr, Action.SCALE_IN_FINISHED);
                }
                log.debug("Starting cooldown period (" + autoScalePolicy.getCooldown() + "s) for AutoScalePolicy with id: " + autoScalePolicy.getId());
                //TODO Launching a new instance should not be part of the cooldown
                Thread.sleep(autoScalePolicy.getCooldown() * 1000);
                log.debug("Finished cooldown period (" + autoScalePolicy.getCooldown() + "s) for AutoScalePolicy with id: " + autoScalePolicy.getId());
                setStatus(Status.ACTIVE);
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
        if (nonBlockingStatus.contains(this.vnfr.getStatus())) {
            return true;
        } else {
            return false;
        }
    }

    private synchronized boolean setStatus(Status status) {
        log.debug("Set status of vnfr " + vnfr.getId() + " to " + status.name());
        Collection<Status> nonBlockingStatus = new HashSet<Status>();
        nonBlockingStatus.add(Status.ACTIVE);
        if (nonBlockingStatus.contains(this.vnfr.getStatus()) || status.equals(Status.ACTIVE)) {
            vnfr.setStatus(status);
            return true;
        } else {
            return false;
        }
    }
}
