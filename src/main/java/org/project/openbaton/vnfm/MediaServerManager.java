package org.project.openbaton.vnfm;

import javassist.NotFoundException;
import org.project.openbaton.catalogue.mano.common.Event;
import org.project.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.Action;
import org.project.openbaton.catalogue.nfvo.CoreMessage;
import org.project.openbaton.clients.exceptions.VimDriverException;
import org.project.openbaton.clients.interfaces.ClientInterfaces;
import org.project.openbaton.common.vnfm_sdk.jms.AbstractVnfmSpringJMS;
import org.project.openbaton.vnfm.core.ElasticityManagement;
import org.project.openbaton.vnfm.core.LifecycleManagement;
import org.project.openbaton.vnfm.core.ResourceManagement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.ClassUtils;

import javax.jms.JMSException;
import javax.naming.NamingException;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by lto on 27/05/15.
 */
@EnableAsync
public class MediaServerManager extends AbstractVnfmSpringJMS {

    @Autowired
    ResourceManagement resourceManagement;

    @Autowired
    ElasticityManagement elasticityManagement;

    @Autowired
    LifecycleManagement lifecycleManagement;

    @Override
    public VirtualNetworkFunctionRecord instantiate(VirtualNetworkFunctionRecord vnfr) {
        log.info("Instantiation of VirtualNetworkFunctionRecord " + vnfr.getName());
        log.trace("Instantiation of VirtualNetworkFunctionRecord " + vnfr);
        log.debug("Number of events: " + vnfr.getLifecycle_event().size());

        Set<Event> events = lifecycleManagement.listEvents(vnfr);
        Set<Event> historyEvents = lifecycleManagement.listHistoryEvents(vnfr);
        if (events.contains(Event.ALLOCATE)) {
            log.debug("Processing event ALLOCATE");
            List<Future<String>> ids = new ArrayList<>();
            try {
                //GrantingLifecycleOperation for initial Allocation
                if (!historyEvents.contains(Event.GRANTED)) {
                    resourceManagement.grantLifecycleOperation(vnfr);
                    return vnfr;
                }
                //Allocate Resources
                for(VirtualDeploymentUnit vdu : vnfr.getVdu()) {
                    Future<String> allocate = resourceManagement.allocate(vnfr, vdu);
                    ids.add(allocate);
                }
                //Print ids of deployed VDUs
                for(Future<String> id : ids) {
                    try {
                        log.debug("Created VDU with id: " + id.get());
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                    } catch (ExecutionException e) {
                        log.error(e.getMessage(), e);
                    }
                }
                //Put EVENT ALLOCATE to history
                lifecycleManagement.removeEvent(vnfr, Event.ALLOCATE);
            } catch (NotFoundException e) {
                log.error(e.getMessage(), e);
            } catch (VimDriverException e) {
                log.error(e.getMessage(), e);
                CoreMessage coreMessage = new CoreMessage();
                coreMessage.setAction(Action.ERROR);
                coreMessage.setPayload(vnfr);
                this.sendMessageToQueue("vnfm-core-actions", coreMessage);
            }
        }
        if (events.contains(Event.SCALE)) {
            log.debug("Processing event SCALE");
            elasticityManagement.activate(vnfr);
            try {
                //Put EVENT SCALE to history
                lifecycleManagement.removeEvent(vnfr, Event.SCALE);
            } catch (NotFoundException e) {
                log.error(e.getMessage(), e);
            }
        }
        log.trace("I've finished initialization of vnf " + vnfr.getName() + " in facts there are only " + vnfr.getLifecycle_event().size() + " events");
//        CoreMessage coreMessage = new CoreMessage();
//        coreMessage.setAction(Action.INSTANTIATE_FINISH);
//        coreMessage.setPayload(vnfr);
//        this.sendMessageToQueue("vnfm-core-actions", coreMessage);
        return vnfr;
    }

    @Override
    public void query() {

    }

    @Override
    public void scale(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
    }

    @Override
    public void checkInstantiationFeasibility() {

    }

    @Override
    public void heal() {

    }

    @Override
    public void updateSoftware() {

    }

    @Override
    public VirtualNetworkFunctionRecord modify(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        log.trace("Adding relation with VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord);
        log.debug("Adding relation with VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord.getName());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return virtualNetworkFunctionRecord;
    }


    @Override
    public void upgradeSoftware() {

    }

    @Override
    public VirtualNetworkFunctionRecord terminate(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        log.info("Terminating vnfr with id " + virtualNetworkFunctionRecord.getId());
        for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
            try {
                log.debug("Releasing resources for vdu with id " + vdu.getId());
                resourceManagement.release(virtualNetworkFunctionRecord, vdu);
                log.debug("Released resources for vdu with id " + vdu.getId());
            } catch (NotFoundException e) {
                log.error(e.getMessage(), e);
            }
        }
        log.info("Terminated vnfr with id " + virtualNetworkFunctionRecord.getId());
//        CoreMessage coreMessage = new CoreMessage();
//        coreMessage.setAction(Action.RELEASE_RESOURCES);
//        coreMessage.setPayload(virtualNetworkFunctionRecord);
//        this.sendMessageToQueue("vnfm-core-actions", coreMessage);
        return virtualNetworkFunctionRecord;
    }

    @Override
    public void handleError(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        log.error("Error arrised.");
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(MediaServerManager.class, args);
    }
}
