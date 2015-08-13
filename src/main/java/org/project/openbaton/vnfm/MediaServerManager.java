package org.project.openbaton.vnfm;

import javassist.NotFoundException;
import org.project.openbaton.catalogue.mano.common.Event;
import org.project.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.Action;
import org.project.openbaton.catalogue.nfvo.CoreMessage;
import org.project.openbaton.clients.exceptions.VimDriverException;
import org.project.openbaton.common.vnfm_sdk.jms.AbstractVnfmSpringJMS;
import org.project.openbaton.vnfm.core.ElasticityManagement;
import org.project.openbaton.vnfm.core.LifecycleManagement;
import org.project.openbaton.vnfm.core.ResourceManagement;
import org.project.openbaton.vnfm.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.EnableAsync;

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
    private ResourceManagement resourceManagement;

    @Autowired
    private ElasticityManagement elasticityManagement;

    @Autowired
    private LifecycleManagement lifecycleManagement;

    @Override
    public CoreMessage instantiate(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        log.info("Instantiation of VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord.getName());
        log.trace("Instantiation of VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord);

        Set<Event> events = lifecycleManagement.listEvents(virtualNetworkFunctionRecord);
        Set<Event> historyEvents = lifecycleManagement.listHistoryEvents(virtualNetworkFunctionRecord);
        if (events.contains(Event.ALLOCATE) && !historyEvents.contains(Event.ALLOCATE)) {
            log.debug("Processing event ALLOCATE");
            List<Future<String>> ids = new ArrayList<>();
            try {
                //GrantingLifecycleOperation for initial Allocation
                if (!historyEvents.contains(Event.GRANTED)) {
                    //resourceManagement.grantLifecycleOperation(vnfr);
                    CoreMessage coreMessage = new CoreMessage();
                    coreMessage.setAction(Action.GRANT_OPERATION);
                    coreMessage.setPayload(virtualNetworkFunctionRecord);
                    return coreMessage;
                }
                //Allocate Resources
                for(VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
                    Future<String> allocate = resourceManagement.allocate(virtualNetworkFunctionRecord, vdu);
                    ids.add(allocate);
                }
                //Print ids of deployed VDUs
                for(Future<String> id : ids) {
                    try {
                        log.debug("Created VDU with id: " + id.get());
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                        CoreMessage coreMessage = new CoreMessage();
                        coreMessage.setAction(Action.ERROR);
                        coreMessage.setPayload(virtualNetworkFunctionRecord);
                        return coreMessage;
                    } catch (ExecutionException e) {
                        log.error(e.getMessage(), e);
                        CoreMessage coreMessage = new CoreMessage();
                        coreMessage.setAction(Action.ERROR);
                        coreMessage.setPayload(virtualNetworkFunctionRecord);
                        return coreMessage;
                    }
                }
                //Put EVENT ALLOCATE to history
//                lifecycleManagement.removeEvent(vnfr, Event.ALLOCATE);
                updateVnfr(virtualNetworkFunctionRecord,Event.ALLOCATE);
            } catch (NotFoundException e) {
                log.error(e.getMessage(), e);
            } catch (VimDriverException e) {
                log.error(e.getMessage(), e);
                CoreMessage coreMessage = new CoreMessage();
                coreMessage.setAction(Action.ERROR);
                coreMessage.setPayload(virtualNetworkFunctionRecord);
                return coreMessage;
            }
        }
        if (events.contains(Event.SCALE)) {
            log.debug("Processing event SCALE");
            elasticityManagement.activate(virtualNetworkFunctionRecord);
            //Put EVENT SCALE to history
//          lifecycleManagement.removeEvent(vnfr, Event.SCALE);
            updateVnfr(virtualNetworkFunctionRecord, Event.ALLOCATE);
        }
        log.trace("I've finished initialization of vnf " + virtualNetworkFunctionRecord.getName() + " in facts there are only " + virtualNetworkFunctionRecord.getLifecycle_event().size() + " events");
        CoreMessage coreMessage = new CoreMessage();
        coreMessage.setAction(Action.INSTANTIATE);
        coreMessage.setPayload(virtualNetworkFunctionRecord);
        return coreMessage;
    }

    @Override
    public void query() {

    }

    @Override
    public void scale() {
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
    public CoreMessage modify(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        //TODO implement modify

        CoreMessage coreMessage = new CoreMessage();
        coreMessage.setAction(Action.MODIFY);
        coreMessage.setPayload(virtualNetworkFunctionRecord);
        updateVnfr(virtualNetworkFunctionRecord,Event.CONFIGURE);
        return coreMessage;
    }


    @Override
    public void upgradeSoftware() {

    }

    @Override
    public CoreMessage terminate(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        log.info("Terminating vnfr with id " + virtualNetworkFunctionRecord.getId());
        Set<Event> events = lifecycleManagement.listEvents(virtualNetworkFunctionRecord);
        if (events.contains(Event.SCALE))
            elasticityManagement.deactivate(virtualNetworkFunctionRecord);

        for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
            try {
                log.debug("Releasing resources for vdu with id " + vdu.getId());
                resourceManagement.release(virtualNetworkFunctionRecord, vdu);
                log.debug("Released resources for vdu with id " + vdu.getId());
            } catch (NotFoundException e) {
                log.error(e.getMessage(), e);
            }
        }
        //TODO remove VDU from the vnfr
        log.info("Terminated vnfr with id " + virtualNetworkFunctionRecord.getId());
        CoreMessage coreMessage = new CoreMessage();
        coreMessage.setAction(Action.RELEASE_RESOURCES);
        coreMessage.setPayload(virtualNetworkFunctionRecord);
        return coreMessage;
    }

    @Override
    public CoreMessage handleError(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        log.error("Error arrised.");
        CoreMessage coreMessage = new CoreMessage();
        coreMessage.setAction(Action.ERROR);
        coreMessage.setPayload(virtualNetworkFunctionRecord);
        return coreMessage;
    }

    @Override
    protected CoreMessage start(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        CoreMessage coreMessage = new CoreMessage();
        coreMessage.setAction(Action.START);
        coreMessage.setPayload(virtualNetworkFunctionRecord);
        updateVnfr(virtualNetworkFunctionRecord, Event.START);
        return coreMessage;
    }

    @Override
    protected CoreMessage configure(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        /**
         * This message should never arrive!
         */
        CoreMessage coreMessage = new CoreMessage();
        coreMessage.setAction(Action.CONFIGURE);
        coreMessage.setPayload(virtualNetworkFunctionRecord);

        updateVnfr(virtualNetworkFunctionRecord,Event.CONFIGURE);

        return coreMessage;
    }

    @Override
    protected void setup() {
        super.setup();
        try {
            resourceManagement.setClientInterfaces(Utils.getVimDriverPlugin(properties));
            resourceManagement.setResourcePerformanceManagement(Utils.getMonitoringPlugin(properties));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(123);
        }

    }

    public static void main(String[] args) {

        SpringApplication.run(MediaServerManager.class, args);

    }
}
