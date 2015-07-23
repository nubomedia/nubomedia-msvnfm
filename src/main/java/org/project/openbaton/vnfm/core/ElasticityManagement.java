package org.project.openbaton.vnfm.core;

import javassist.NotFoundException;
import org.project.openbaton.catalogue.mano.common.AutoScalePolicy;
import org.project.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.Action;
import org.project.openbaton.catalogue.nfvo.CoreMessage;
import org.project.openbaton.common.vnfm_sdk.utils.UtilsJMS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.jms.JMSException;
import javax.naming.NamingException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by mpa on 07.07.15.
 */

@SpringBootApplication
public class ElasticityManagement implements Runnable{

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected ResourceManagement resourceManagement;

    private VirtualNetworkFunctionRecord vnfr;

    private boolean activated;

    private Thread elasticityThread;

    public void init(VirtualNetworkFunctionRecord vnfr) {
        this.vnfr = vnfr;
        this.activated = false;
    }

    @Override
    public void run() {
        while (activated == true) {
            for (AutoScalePolicy autoScalePolicy : vnfr.getAuto_scale_policy()) {
                Set<Integer> measurementResults = new HashSet<Integer>();
                boolean triggerAction = false;
                log.debug("Getting all measurement results for vnfr " + vnfr.getId() + " on metric " + autoScalePolicy.getMetric() + ".");
                for (VirtualDeploymentUnit vdu : vnfr.getVdu()) {
                    log.debug("Getting measurement result for vdu " + vdu.getId() + " on metric " + autoScalePolicy.getMetric() + ".");
                    int measurementResult = resourceManagement.getMeasurementResults(vdu, autoScalePolicy.getMetric());
                    measurementResults.add(measurementResult);
                    log.debug("Got measurement result for vdu " + vdu.getId() + " on metric " + autoScalePolicy.getMetric() + " -> " + measurementResult + ".");
                }
                log.debug("Got all measurement results for vnfr " + vnfr.getId() + " on metric " + autoScalePolicy.getMetric() + " -> " + measurementResults + ".");

                log.debug("Check if scaling is needed.");
                if (triggerAction(autoScalePolicy, calculateMeasurementResult(autoScalePolicy, measurementResults))) {
                    executeAction(autoScalePolicy);
                    log.debug("Execute scaling action of AutoScalePolicy with id " + autoScalePolicy.getId() + ".");
                } else {
                    log.debug("Scaling of AutoScalePolicy with id " + autoScalePolicy.getId() + "is not executed.");
                }
                try {
                    log.debug("Starting sleeping period (" + autoScalePolicy.getPeriod() + "s) for AutoScalePolicy with id: " + autoScalePolicy.getId() + ".");
                    Thread.sleep(autoScalePolicy.getPeriod() * 1000);
                    log.debug("Finished sleeping period (" + autoScalePolicy.getPeriod() + "s) for AutoScalePolicy with id: " + autoScalePolicy.getId() + ".");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void activate() {
        if (this.activated == false) {
            this.activated = true;
            elasticityThread = new Thread(this);
            elasticityThread.start();
            log.info("ElasticityManagement is activated.");
        } else {
            log.warn("ElasticityManagement is already activated.");
        }
    }

    public void deactivate() {
        if (this.activated == true) {
            this.activated = false;
            while (elasticityThread.isAlive())
                log.debug("Waiting until ElasticityThread is deactivated.");
            log.warn("ElasticityManagement is deactivated.");
        } else {
            log.warn("ElasticityManagement is not running.");
        }
    }

    public void scaleUp() throws NotFoundException{
        if (vnfr.getVdu().iterator().hasNext()) {
            VirtualDeploymentUnit vdu = vnfr.getVdu().iterator().next();
            VirtualDeploymentUnit newVDU = new VirtualDeploymentUnit();
            newVDU.setVimInstance(vdu.getVimInstance());
            newVDU.setVm_image(vdu.getVm_image());
            newVDU.setVnfc(vdu.getVnfc());
            newVDU.setComputation_requirement(vdu.getComputation_requirement());
            newVDU.setHigh_availability(vdu.getHigh_availability());
            newVDU.setMonitoring_parameter(vdu.getMonitoring_parameter());
            newVDU.setLifecycle_event(vdu.getLifecycle_event());
            newVDU.setScale_in_out(vdu.getScale_in_out());
            newVDU.setVdu_constraint(vdu.getVdu_constraint());
            newVDU.setVirtual_memory_resource_element(vdu.getVirtual_memory_resource_element());
            newVDU.setVirtual_network_bandwidth_resource(vdu.getVirtual_network_bandwidth_resource());
            vnfr.getVdu().add(newVDU);
            resourceManagement.allocate(vnfr, newVDU);
            try {
                CoreMessage coreMessage = new CoreMessage();
                coreMessage.setAction(Action.INSTANTIATE_FINISH);
                coreMessage.setPayload(vnfr);
                UtilsJMS.sendToQueue(coreMessage, "vnfm-core-actions");
            } catch (NamingException e) {
                e.printStackTrace();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        } else {
            log.warn("No VDU to copy from while upscaling.");
        }
    }

    public void scaleDown() throws NotFoundException {
        if (vnfr.getVdu().size() > 1 && vnfr.getVdu().iterator().hasNext()) {
            VirtualDeploymentUnit vdu = vnfr.getVdu().iterator().next();
            resourceManagement.release(vnfr, vdu);
            try {
                CoreMessage coreMessage = new CoreMessage();
                coreMessage.setAction(Action.INSTANTIATE_FINISH);
                coreMessage.setPayload(vnfr);
                UtilsJMS.sendToQueue(coreMessage, "vnfm-core-actions");
            } catch (NamingException e) {
                e.printStackTrace();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        } else {
            log.warn("Cannot terminate the last VDU.");
        }
    }

    public double calculateMeasurementResult(AutoScalePolicy autoScalePolicy, Set<Integer> measurementResults) {
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

    public void executeAction(AutoScalePolicy autoScalePolicy) {
        try {
            switch (autoScalePolicy.getAction()) {
                case "scaleup":
                    scaleUp();
                    break;
                case "scaledown":
                    scaleDown();
                    break;
            }
            log.debug("Starting cooldown period (" + autoScalePolicy.getCooldown() + "s) for AutoScalePolicy with id: " + autoScalePolicy.getId() + ".");
            Thread.sleep(autoScalePolicy.getCooldown() * 1000);
            log.debug("Finished cooldown period (" + autoScalePolicy.getCooldown() + "s) for AutoScalePolicy with id: " + autoScalePolicy.getId() + ".");
        } catch (NotFoundException e) {
            log.error(e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
