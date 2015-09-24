package org.project.openbaton.vnfm.core;

import javassist.NotFoundException;
import org.project.openbaton.catalogue.mano.common.AutoScalePolicy;
import org.project.openbaton.catalogue.mano.descriptor.VNFComponent;
import org.project.openbaton.catalogue.mano.descriptor.VNFDConnectionPoint;
import org.project.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.project.openbaton.catalogue.mano.record.Status;
import org.project.openbaton.catalogue.mano.record.VNFCInstance;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.Action;
import org.project.openbaton.catalogue.nfvo.CoreMessage;
import org.project.openbaton.catalogue.nfvo.Item;
import org.project.openbaton.catalogue.nfvo.messages.Interfaces.NFVMessage;
import org.project.openbaton.catalogue.nfvo.messages.OrVnfmGenericMessage;
import org.project.openbaton.catalogue.nfvo.messages.VnfmOrGenericMessage;
import org.project.openbaton.catalogue.nfvo.messages.VnfmOrInstantiateMessage;
import org.project.openbaton.clients.exceptions.VimDriverException;
import org.project.openbaton.common.vnfm_sdk.VnfmHelper;
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
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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

    @Autowired
    private VnfrMonitor vnfrMonitor;

    /**
     * Vim must be initialized only after the registry is up and plugin registered
     */
    public void initilizeVim() {
        PluginBroker<ResourcePerformanceManagement> pluginBroker = new PluginBroker<>();
        try {
            this.monitor = pluginBroker.getPlugin("monitor", "dummy", 19345);
        } catch (RemoteException e) {
            log.error(e.getLocalizedMessage());
        } catch (NotBoundException e) {
            log.error(e.getLocalizedMessage());
        }
        resourceManagement = (ResourceManagement) context.getBean("openstackVIM", "openstack", 19345);
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
            ElasticityTask elasticityTask = (ElasticityTask) context.getBean("elasticityTask");
            elasticityTask.init(vnfr, policy);
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
            //((ElasticityTask) scheduledFuture).removeVNFR(vnfr);
        }
        vnfrMonitor.removeVNFR(vnfr.getId());
        log.debug("Deactivated Elasticity for vnfr " + vnfr.getId());
    }

    public void scaleVNFComponents(VirtualNetworkFunctionRecord vnfr, AutoScalePolicy autoScalePolicy) {
        if (autoScalePolicy.getAction().equals("scaleup")) {
            for (VirtualDeploymentUnit vdu : vnfr.getVdu()) {
                if (vdu.getVnfc().size() < vdu.getScale_in_out() && (vdu.getVnfc().iterator().hasNext())) {
                    VNFComponent vnfComponent_copy = vdu.getVnfc().iterator().next();
                    VNFComponent vnfComponent_new = new VNFComponent();
                    vnfComponent_new.setConnection_point(new HashSet<VNFDConnectionPoint>());
                    for (VNFDConnectionPoint vnfdConnectionPoint_copy : vnfComponent_copy.getConnection_point()) {
                        VNFDConnectionPoint vnfdConnectionPoint_new = new VNFDConnectionPoint();
                        vnfdConnectionPoint_new.setVirtual_link_reference(vnfdConnectionPoint_copy.getVirtual_link_reference());
                        vnfdConnectionPoint_new.setType(vnfdConnectionPoint_copy.getType());
                        vnfComponent_new.getConnection_point().add(vnfdConnectionPoint_new);
                    }
                    vdu.getVnfc().add(vnfComponent_new);
                    log.debug("SCALING: Added new Component to VDU " + vdu.getId());
                    return;
                } else {
                    continue;
                }
            }
            log.debug("Not found any VDU to scale out a VNFComponent. Limits are reached.");
        } else if (autoScalePolicy.getAction().equals("scaledown")) {
            for (VirtualDeploymentUnit vdu : vnfr.getVdu()) {
                if (vdu.getVnfc().size() > 1 && vdu.getVnfc().iterator().hasNext()) {
                    VNFComponent vnfComponent_remove = vdu.getVnfc().iterator().next();
                    vdu.getVnfc().remove(vnfComponent_remove);
                    log.debug("SCALING: Removed Component " + vnfComponent_remove.getId() + " from VDU " + vdu.getId());
                    return;
                } else {
                    continue;
                }
            }
            log.debug("Not found any VDU to scale in a VNFComponent. Limits are reached.");
        }
    }

    public void scaleVNFCInstances(VirtualNetworkFunctionRecord vnfr) {
        List<Future<String>> ids = new ArrayList<>();
        for (VirtualDeploymentUnit vdu : vnfr.getVdu()) {
            //Check for additional components for scaling out
            for (VNFComponent vnfComponent : vdu.getVnfc()) {
                //VNFComponent ID is null -> NEW
                boolean found = false;
                //Check if VNFCInstance for VNFComponent already exists
                for (VNFCInstance vnfcInstance : vdu.getVnfc_instance()) {
                    if (vnfComponent.getId().equals(vnfcInstance.getVnfc_reference())) {
                        found = true;
                        break;
                    }
                }
                //If the Instance doesn't exists, allocate a new one
                if (!found) {
                    try {
                        String userdata = Utils.getUserdata();
                        Future<String> allocate = resourceManagement.allocate(vdu, vnfr, vnfComponent, userdata, vnfComponent.isExposed());
                        ids.add(allocate);
                        continue;
                    } catch (VimException e) {
                        log.error(e.getMessage(), e);
                        throw new RuntimeException();
                    } catch (VimDriverException e) {
                        log.error(e.getMessage(), e);
                        throw new RuntimeException();
                    }
                }
            }
            //Check for removed Components to scale in
            Set<VNFCInstance> removed_instances = new HashSet<>();
            for (VNFCInstance vnfcInstance : vdu.getVnfc_instance()) {
                boolean found = false;
                for (VNFComponent vnfComponent : vdu.getVnfc()) {
                    if (vnfcInstance.getVnfc_reference().equals(vnfComponent.getId())) {
                        log.debug("VNCInstance: " + vnfcInstance.toString() + " stays");
                        found = true;
                        //VNFComponent is still existing
                        break;
                    }
                }
                //VNFComponent is not exsting anymore -> Remove VNFCInstance
                if (!found) {
                    try {
                        log.debug("VNCInstance: " + vnfcInstance.toString() + " removing");
                        resourceManagement.release(vnfcInstance, vdu.getVimInstance());
                        removed_instances.add(vnfcInstance);
                    } catch (VimException e) {
                        log.error(e.getMessage(), e);
                        throw new RuntimeException();
                    }
                }
            }
            //Remove terminated VNFCInstances
            vdu.getVnfc_instance().removeAll(removed_instances);
        }
        //Print ids of deployed VDUs
        for (Future<String> id : ids) {
            try {
                log.debug("Created VNFCInstance with id: " + id.get());
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e.getMessage(), e);
            } catch (ExecutionException e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    public synchronized List<Item> getRawMeasurementResults(VirtualNetworkFunctionRecord vnfr, String metric, String period) {
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

    public boolean checkFeasibility(VirtualNetworkFunctionRecord vnfr, AutoScalePolicy autoScalePolicy) {
        if (autoScalePolicy.getAction().equals("scaleup")) {
            for (VirtualDeploymentUnit vdu : vnfr.getVdu()) {
                if (vdu.getVnfc().size() < vdu.getScale_in_out()) {
                    return true;
                }
            }
            log.debug("Maximum number of instances are reached on all VimInstances");
            return false;
        } else if (autoScalePolicy.getAction().equals("scaledown")) {
            for (VirtualDeploymentUnit vdu : vnfr.getVdu()) {
                if (vdu.getVnfc().size() > 1) {
                    return true;
                }
            }
            log.warn("Cannot terminate the last VDU.");
            return false;
        }
        return true;
    }
}

@Service
@Scope("prototype")
class ElasticityTask implements Runnable {

    protected static final String nfvoQueue = "vnfm-core-actions";

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private VnfmHelper vnfmHelper;

    @Autowired
    private ElasticityManagement elasticityManagement;

    @Autowired
    protected JmsTemplate jmsTemplate;

    @Autowired
    private VnfrMonitor monitor;

    private String vnfr_id;

    private AutoScalePolicy autoScalePolicy;

    private String name;

    public void init(VirtualNetworkFunctionRecord vnfr, AutoScalePolicy autoScalePolicy) {
        this.monitor.addVNFR(vnfr);
        this.vnfr_id = vnfr.getId();
        this.autoScalePolicy = autoScalePolicy;
        this.name = "ElasticityTask#" + vnfr.getId();
    }

    @Override
    public void run() {
        log.debug("Check if scaling is needed.");
        try {
            VirtualNetworkFunctionRecord vnfr = monitor.getVNFR(vnfr_id);
            List<Item> measurementResults = elasticityManagement.getRawMeasurementResults(vnfr, autoScalePolicy.getMetric(), Integer.toString(autoScalePolicy.getPeriod()));
            double finalResult = elasticityManagement.calculateMeasurementResult(autoScalePolicy, measurementResults);
            log.debug("Final measurement result on vnfr " + vnfr.getId() + " on metric " + autoScalePolicy.getMetric() + " with statistic " + autoScalePolicy.getStatistic() + " is " + finalResult + " " + measurementResults);
            if (vnfr.getStatus().equals(Status.ACTIVE)) {
                if (elasticityManagement.triggerAction(autoScalePolicy, finalResult) && elasticityManagement.checkFeasibility(vnfr, autoScalePolicy) && setStatus(Status.SCALING) == true) {
                    log.debug("Executing scaling action of AutoScalePolicy with id " + autoScalePolicy.getId());
                    elasticityManagement.scaleVNFComponents(vnfr, autoScalePolicy);
                    vnfr = updateOnNFVO(vnfr, Action.SCALING);
                    elasticityManagement.scaleVNFCInstances(vnfr);
                    log.debug("Starting cooldown period (" + autoScalePolicy.getCooldown() + "s) for AutoScalePolicy with id: " + autoScalePolicy.getId());
                    Thread.sleep(autoScalePolicy.getCooldown() * 1000);
                    log.debug("Finished cooldown period (" + autoScalePolicy.getCooldown() + "s) for AutoScalePolicy with id: " + autoScalePolicy.getId());
                    vnfr = updateOnNFVO(vnfr, Action.SCALED);
                } else {
                    log.debug("Scaling of AutoScalePolicy with id " + autoScalePolicy.getId() + " is not executed");
                }
            } else {
                log.debug("ElasticityTask: In State Scaling -> waiting until finished");
            }
            log.debug("Starting sleeping period (" + autoScalePolicy.getPeriod() + "s) for AutoScalePolicy with id: " + autoScalePolicy.getId());
        } catch (InterruptedException e) {
            log.warn("ElasticityTask was interrupted");
        }
    }

    private synchronized boolean setStatus(Status status) {
        log.debug("Set status of vnfr " + monitor.getVNFR(vnfr_id).getId() + " to " + status.name());
        Collection<Status> nonBlockingStatus = new HashSet<Status>();
        nonBlockingStatus.add(Status.ACTIVE);
        if (nonBlockingStatus.contains(this.monitor.getVNFR(vnfr_id).getStatus()) || status.equals(Status.ACTIVE)) {
            monitor.getVNFR(vnfr_id).setStatus(status);
            return true;
        } else {
            return false;
        }
    }

    protected VirtualNetworkFunctionRecord updateOnNFVO(VirtualNetworkFunctionRecord vnfr, Action action) {
        NFVMessage response = null;
        try {
            response = vnfmHelper.sendAndReceive(action, vnfr);
        } catch (JMSException e) {
            log.error("" + e.getMessage());
            vnfr.setStatus(Status.ERROR);
            return vnfr;
        } catch (Exception e) {
            vnfr.setStatus(Status.ERROR);
            log.error("" + e.getMessage());
            return vnfr;
        }
        log.debug("" + response);
        if (response.getAction().ordinal() == Action.ERROR.ordinal()) {
            vnfr.setStatus(Status.ERROR);
            return vnfr;
        }
        OrVnfmGenericMessage orVnfmGenericMessage = (OrVnfmGenericMessage) response;
        vnfr = orVnfmGenericMessage.getVnfr();
        this.monitor.addVNFR(vnfr);
        return vnfr;
    }

}
