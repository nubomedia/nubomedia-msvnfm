package org.project.openbaton.vnfm;

import org.project.openbaton.catalogue.mano.common.Event;
import org.project.openbaton.catalogue.mano.descriptor.InternalVirtualLink;
import org.project.openbaton.catalogue.mano.descriptor.VNFComponent;
import org.project.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.project.openbaton.catalogue.mano.descriptor.VirtualNetworkFunctionDescriptor;
import org.project.openbaton.catalogue.mano.record.*;
import org.project.openbaton.catalogue.nfvo.Action;
import org.project.openbaton.catalogue.nfvo.CoreMessage;
import org.project.openbaton.catalogue.nfvo.messages.Interfaces.NFVMessage;
import org.project.openbaton.catalogue.nfvo.messages.VnfmOrGenericMessage;
import org.project.openbaton.clients.exceptions.VimDriverException;
import org.project.openbaton.common.vnfm_sdk.exception.BadFormatException;
import org.project.openbaton.common.vnfm_sdk.exception.NotFoundException;
import org.project.openbaton.common.vnfm_sdk.exception.VnfmSdkException;
import org.project.openbaton.common.vnfm_sdk.jms.AbstractVnfmSpringJMS;
import org.project.openbaton.exceptions.VimException;
import org.project.openbaton.nfvo.plugin.utils.PluginStartup;
import org.project.openbaton.nfvo.vim_interfaces.resource_management.ResourceManagement;
import org.project.openbaton.vnfm.core.ElasticityManagement;
import org.project.openbaton.vnfm.core.LifecycleManagement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by lto on 27/05/15.
 */
@EnableAsync
public class MediaServerManager extends AbstractVnfmSpringJMS {

    @Autowired
    private ElasticityManagement elasticityManagement;

    @Autowired
    private ConfigurableApplicationContext context;

    private ResourceManagement resourceManagement;

    @Autowired
    private LifecycleManagement lifecycleManagement;

    /**
     * Vim must be initialized only after the registry is up and plugin registered
     */
    private void initilizeVim(){
        resourceManagement = (ResourceManagement) context.getBean("openstackVIM", "openstack", 19345);
    }

    @Override
    public VirtualNetworkFunctionRecord instantiate(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws Exception{
        log.info("Instantiation of VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord.getName());
        log.trace("Instantiation of VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord);

        Set<Event> events = lifecycleManagement.listEvents(virtualNetworkFunctionRecord);
        Set<Event> historyEvents = lifecycleManagement.listHistoryEvents(virtualNetworkFunctionRecord);
        if (events.contains(Event.ALLOCATE) && !historyEvents.contains(Event.ALLOCATE)) {
            log.debug("Processing event ALLOCATE");
            List<Future<String>> ids = new ArrayList<>();

            try {
                virtualNetworkFunctionRecord = grantLifecycleOperation(virtualNetworkFunctionRecord);
//                if(grantLifecycleOperation(virtualNetworkFunctionRecord)) {
                    try {
                        //Allocate Resources
                        for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
                            log.debug("Creating " + vdu.getVnfc().size() + " VMs");
                            for (VNFComponent vnfComponent : vdu.getVnfc()) {
                                Future<String> allocate = resourceManagement.allocate(vdu, virtualNetworkFunctionRecord, vnfComponent);
                                ids.add(allocate);
                            }
                        }
                        //Print ids of deployed VDUs
                        for (Future<String> id : ids) {
                            try {
                                log.debug("Created VDU with id: " + id.get());
                            } catch (InterruptedException e) {
                                log.error(e.getMessage(), e);
                                throw new RuntimeException(e.getMessage(), e);
                            } catch (ExecutionException e) {
                                log.error(e.getMessage(), e);
                                throw new RuntimeException(e.getMessage(), e);
                            }
                        }
                    } catch (VimDriverException e) {
                        log.error(e.getMessage(), e);
                        throw new RuntimeException(e.getMessage(), e);
                    } catch (VimException e) {
                        log.error(e.getMessage(), e);
                        throw new RuntimeException(e.getMessage(), e);
                    }
//                }
            } catch (VnfmSdkException e) {
                e.printStackTrace();
                throw e;
            }
        }
        log.trace("I've finished initialization of vnf " + virtualNetworkFunctionRecord.getName() + " in facts there are only " + virtualNetworkFunctionRecord.getLifecycle_event().size() + " events");
        return virtualNetworkFunctionRecord;
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
    public VirtualNetworkFunctionRecord modify(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFRecordDependency dependency) {
        return virtualNetworkFunctionRecord;
    }

    @Override
    public void upgradeSoftware() {

    }

    @Override
    public VirtualNetworkFunctionRecord terminate(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        log.info("Terminating vnfr with id " + virtualNetworkFunctionRecord.getId());
        Set<Event> events = lifecycleManagement.listEvents(virtualNetworkFunctionRecord);
        if (events.contains(Event.SCALE))
            elasticityManagement.deactivate(virtualNetworkFunctionRecord);

        for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
            for (VNFCInstance vnfcInstance : vdu.getVnfc_instance()) {
                log.debug("Releasing resources for vdu with id " + vdu.getId());
                try {
                    resourceManagement.release(vnfcInstance, vdu.getVimInstance());
                } catch (VimException e) {
                    log.error(e.getMessage(), e);
                    throw new RuntimeException(e.getMessage(), e);
                }
                for (VNFComponent vnfComponent : vdu.getVnfc()) {
                    if (vnfcInstance.getVnfc_reference().equals(vnfComponent.getId())) {
                        vdu.getVnfc().remove(vnfComponent);
                        break;
                    }
                }
                log.debug("Released resources for vdu with id " + vdu.getId());
            }
        }
        //TODO remove VDU from the vnfr
        log.info("Terminated vnfr with id " + virtualNetworkFunctionRecord.getId());
        return virtualNetworkFunctionRecord;
    }

    @Override
    public CoreMessage handleError(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        log.error("Error arrised.");
        CoreMessage coreMessage = new CoreMessage();
        coreMessage.setAction(Action.ERROR);
        coreMessage.setVirtualNetworkFunctionRecord(virtualNetworkFunctionRecord);
        return coreMessage;
    }

    @Override
    protected VirtualNetworkFunctionRecord start(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        //TODO where to set it to active?
        virtualNetworkFunctionRecord.setStatus(Status.ACTIVE);
        Set<Event> events = lifecycleManagement.listEvents(virtualNetworkFunctionRecord);
        if (virtualNetworkFunctionRecord.getStatus().equals(Status.ACTIVE) && events.contains(Event.SCALE)) {
            log.debug("Processing event SCALE");
            elasticityManagement.activate(virtualNetworkFunctionRecord);
        }
        return virtualNetworkFunctionRecord;
    }

    @Override
    protected VirtualNetworkFunctionRecord configure(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        /**
         * This message should never arrive!
         */
        CoreMessage coreMessage = new CoreMessage();
        coreMessage.setAction(Action.CONFIGURE);
        coreMessage.setVirtualNetworkFunctionRecord(virtualNetworkFunctionRecord);

        updateVnfr(virtualNetworkFunctionRecord,Event.CONFIGURE);

        return virtualNetworkFunctionRecord;
    }

    @Override
    protected void setup() {
        super.setup();
        try {
            int registryport = 19345;
            Registry registry = LocateRegistry.createRegistry(registryport);
            log.debug("Registry created: ");
            log.debug(registry.toString() + " has: " + registry.list().length + " entries");
            PluginStartup.startPluginRecursive("./plugins", true,"localhost","" + registryport);
        } catch (IOException e) {
            e.printStackTrace();
        }
        elasticityManagement.initilizeVim();
        this.initilizeVim();
    }

    public static void main(String[] args) {
        SpringApplication.run(MediaServerManager.class, args);
    }

    @Override
    public void NotifyChange() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void checkEmsStarted(String hostname) {

    }

    @Override
    protected VirtualNetworkFunctionRecord createVirtualNetworkFunctionRecord(VirtualNetworkFunctionDescriptor virtualNetworkFunctionDescriptor, String flavourId, String vnfInstanceName, Set<VirtualLinkRecord> virtualLink, Map<String, String> extension ) throws BadFormatException, NotFoundException {
        VirtualNetworkFunctionRecord virtualNetworkFunctionRecord = super.createVirtualNetworkFunctionRecord(virtualNetworkFunctionDescriptor, flavourId, vnfInstanceName, virtualLink, extension);
        for (InternalVirtualLink internalVirtualLink : virtualNetworkFunctionRecord.getVirtual_link()) {
            for (VirtualLinkRecord virtualLinkRecord : virtualLink) {
                if (internalVirtualLink.getName().equals(virtualLinkRecord.getName())) {
                    internalVirtualLink.setExtId(virtualLinkRecord.getExtId());
                    internalVirtualLink.setConnectivity_type(virtualLinkRecord.getConnectivity_type());
                }
            }
        }
        return virtualNetworkFunctionRecord;
    }
}
