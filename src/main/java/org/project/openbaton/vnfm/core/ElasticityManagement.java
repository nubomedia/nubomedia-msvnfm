package org.project.openbaton.vnfm.core;

import javassist.NotFoundException;
import org.project.openbaton.catalogue.mano.common.AutoScalePolicy;
import org.project.openbaton.catalogue.mano.common.ConnectionPoint;
import org.project.openbaton.catalogue.mano.descriptor.VNFComponent;
import org.project.openbaton.catalogue.mano.descriptor.VNFDConnectionPoint;
import org.project.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.Action;
import org.project.openbaton.catalogue.nfvo.CoreMessage;
import org.project.openbaton.clients.exceptions.VimDriverException;
import org.project.openbaton.clients.interfaces.ClientInterfaces;
import org.project.openbaton.common.vnfm_sdk.utils.UtilsJMS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.SpringApplicationContextLoader;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.naming.NamingException;
import java.util.*;

/**
 * Created by mpa on 07.07.15.
 */
@Service
@Scope("prototype")
public class ElasticityManagement {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected ResourceManagement resourceManagement;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    public ElasticityThread getElasticityThread(VirtualNetworkFunctionRecord vnfr) throws NotFoundException {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread thread : threadSet) {
            if (thread.getName().equals("ElasticityThread#" + vnfr.getId())) {
                return (ElasticityThread) thread;
            }
        }
        throw new NotFoundException("Not found ElasticityThread with name: " + "ElasticityThread#" + vnfr.getId());
    }

    public void activate(VirtualNetworkFunctionRecord vnfr) {
        try {
            ElasticityThread elasticityThread = getElasticityThread(vnfr);
            if (elasticityThread.isActivated() == false) {
                log.debug("ElasticityTrhead is restarting.");
                elasticityThread.activate();
                log.info("ElasticityTrhead is started.");
            } else {
                log.warn("ElasticityThread is already running.");
            }
        } catch (NotFoundException e) {
            log.debug("ElasticityThread is starting.");
            ElasticityThread elasticityThread = new ElasticityThread(vnfr);
            beanFactory.autowireBean(elasticityThread);
            elasticityThread.activate();
            log.info("ElasticityTrhead is started.");
        }
    }

    public void deactivate(VirtualNetworkFunctionRecord vnfr) {
        try {
            ElasticityThread elasticityThread = getElasticityThread(vnfr);
            if (elasticityThread.isActivated() == true) {
                log.debug("ElasticityThread is stopping.");
                elasticityThread.deactivate();
            } else {
                log.warn("ElasticityThread is not running.");
            }
        } catch (NotFoundException e) {
            log.warn("ElasticityThread is not running.");
        }
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
            try {
                CoreMessage coreMessage = new CoreMessage();
                coreMessage.setAction(Action.SCALE_UP_FINISHED);
                coreMessage.setPayload(vnfr);
                UtilsJMS.sendToQueue(coreMessage, "vnfm-core-actions");
            } catch (NamingException e) {
                log.error(e.getMessage(), e);
            } catch (JMSException e) {
                log.error(e.getMessage(), e);
            }
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
            try {
                CoreMessage coreMessage = new CoreMessage();
                coreMessage.setAction(Action.SCALE_DOWN_FINISHED);
                log.debug("Scaled down vnfr " + vnfr.getId());
                coreMessage.setPayload(vnfr);
                UtilsJMS.sendToQueue(coreMessage, "vnfm-core-actions");
            } catch (NamingException e) {
                log.error(e.getMessage(), e);
            } catch (JMSException e) {
                log.error(e.getMessage(), e);
            }
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

@Service
@Scope("prototype")
class ElasticityThread extends Thread{

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ElasticityManagement elasticityManagement;

    private VirtualNetworkFunctionRecord vnfr;

    private boolean activated;

    public ElasticityThread(VirtualNetworkFunctionRecord vnfr) {
        super();
        this.vnfr = vnfr;
        this.setName("ElasticityThread#" + vnfr.getId());
        this.activated = false;
    }

    @Override
    public void run() {
        activated = true;
        while (activated == true) {
            for (AutoScalePolicy autoScalePolicy : vnfr.getAuto_scale_policy()) {
                boolean triggerAction = false;
                log.debug("Check if scaling is needed.");
                try {
                    List<Integer> measurementResults = elasticityManagement.getRawMeasurementResults(vnfr, autoScalePolicy.getMetric());
                    double finalResult = elasticityManagement.calculateMeasurementResult(autoScalePolicy, measurementResults);
                    log.debug("Final measurement result on vnfr " + vnfr.getId() + " on metric " + autoScalePolicy.getMetric() + " with statistic " + autoScalePolicy.getStatistic() + " is " + finalResult + " " + measurementResults );
                    if (elasticityManagement.triggerAction(autoScalePolicy, finalResult)) {
                        log.debug("Executing scaling action of AutoScalePolicy with id " + autoScalePolicy.getId());
                        elasticityManagement.executeAction(vnfr, autoScalePolicy);
                        log.debug("Starting cooldown period (" + autoScalePolicy.getCooldown() + "s) for AutoScalePolicy with id: " + autoScalePolicy.getId());
                        Thread.sleep(autoScalePolicy.getCooldown() * 1000);
                        log.debug("Finished cooldown period (" + autoScalePolicy.getCooldown() + "s) for AutoScalePolicy with id: " + autoScalePolicy.getId());
                    } else {
                        log.debug("Scaling of AutoScalePolicy with id " + autoScalePolicy.getId() + " is not executed");
                    }
                    log.debug("Starting sleeping period (" + autoScalePolicy.getPeriod() + "s) for AutoScalePolicy with id: " + autoScalePolicy.getId());
                    Thread.sleep(autoScalePolicy.getPeriod() * 1000);
                    log.debug("Finished sleeping period (" + autoScalePolicy.getPeriod() + "s) for AutoScalePolicy with id: " + autoScalePolicy.getId());
                } catch (InterruptedException e) {
                    log.warn("ElasticityThread was interrupted to deactivate autoscaling");
                }
            }
        }
    }

    public boolean isActivated() {
        return this.activated && this.isAlive();
    }

    public void activate() {
        log.debug("Activating ElasticityThread for vnfr " + vnfr.getId());
        if (!this.isAlive()) {
            this.start();
        } else {
            log.warn("ElasticityThread for vnfr " + vnfr.getId() + "is already/still running");
        }
        log.debug("Activated ElasticityThread for vnfr " + vnfr.getId());
    }

    public void deactivate() {
        log.debug("Deactivating ElasticityThread for vnfr " + vnfr.getId());
        this.activated = false;
        this.interrupt();
        while (this.isAlive() == true) {
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.debug("Deactivated ElasticityThread for vnfr " + vnfr.getId());
    }
}
