package org.openbaton.vnfm;

import org.openbaton.autoscaling.core.management.ElasticityManagement;
import org.openbaton.catalogue.mano.common.Event;
import org.openbaton.catalogue.mano.descriptor.VNFComponent;
import org.openbaton.catalogue.mano.descriptor.VNFDConnectionPoint;
import org.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.openbaton.catalogue.mano.record.Status;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VNFRecordDependency;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.catalogue.nfvo.Action;
import org.openbaton.common.vnfm_sdk.amqp.AbstractVnfmSpringAmqp;
import org.openbaton.exceptions.NotFoundException;
import org.openbaton.exceptions.VimException;
import org.openbaton.nfvo.vim_interfaces.resource_management.ResourceManagement;
import org.openbaton.plugin.utils.PluginStartup;
import org.openbaton.vim.drivers.exceptions.VimDriverException;
import org.openbaton.vnfm.catalogue.ManagedVNFR;
import org.openbaton.vnfm.catalogue.MediaServer;
//import org.openbaton.vnfm.core.ElasticityManagement;
import org.openbaton.vnfm.core.LifecycleManagement;
import org.openbaton.vnfm.core.interfaces.ApplicationManagement;
import org.openbaton.vnfm.core.interfaces.MediaServerManagement;
import org.openbaton.vnfm.repositories.ManagedVNFRRepository;
import org.openbaton.vnfm.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by lto on 27/05/15.
 */
@SpringBootApplication
@EntityScan("org.openbaton.vnfm.catalogue")
@ComponentScan({"org.openbaton.vnfm.api", "org.openbaton.autoscaling.api", "org.openbaton.autoscaling"})
@EnableJpaRepositories("org.openbaton.vnfm")
public class MediaServerManager extends AbstractVnfmSpringAmqp {

    @Autowired
    private ElasticityManagement elasticityManagement;

    @Autowired
    private ConfigurableApplicationContext context;

    private ResourceManagement resourceManagement;

    @Autowired
    private LifecycleManagement lifecycleManagement;

    @Autowired
    private ApplicationManagement applicationManagement;

    @Autowired
    private MediaServerManagement mediaServerManagement;

    @Autowired
    private ManagedVNFRRepository managedVnfrRepository;

    /**
     * Vim must be initialized only after the registry is up and plugin registered
     */
    private void initilizeVim() {
        resourceManagement = (ResourceManagement) context.getBean("openstackVIM", "15672");
    }

    @Override
    public VirtualNetworkFunctionRecord instantiate(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, Object object) {
        ManagedVNFR managedVNFR = new ManagedVNFR();
        managedVNFR.setNsrId(virtualNetworkFunctionRecord.getParent_ns_id());
        managedVNFR.setVnfrId(virtualNetworkFunctionRecord.getId());
        managedVnfrRepository.save(managedVNFR);

        log.info("Instantiation of VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord.getName());
        log.trace("Instantiation of VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord);
        /**
         * Allocation of Resources
         *  the grant operation is already done before this method
         */
        log.debug("Processing allocation of Recources for vnfr: " + virtualNetworkFunctionRecord);
            for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
                List<Future<VNFCInstance>> vnfcInstancesFuturePerVDU = new ArrayList<>();
                log.debug("Creating " + vdu.getVnfc().size() + " VMs");
                String userdata = Utils.getUserdata();
                for (VNFComponent vnfComponent : vdu.getVnfc()) {
                    Map<String, String> floatgingIps = new HashMap<>();
                    for (VNFDConnectionPoint connectionPoint : vnfComponent.getConnection_point()){
                        if (connectionPoint.getFloatingIp() != null && !connectionPoint.getFloatingIp().equals(""))
                            floatgingIps.put(connectionPoint.getVirtual_link_reference(),connectionPoint.getFloatingIp());
                    }
                    Future<VNFCInstance> allocate = null;
                    try {
                        allocate = resourceManagement.allocate(vdu, virtualNetworkFunctionRecord, vnfComponent, userdata, floatgingIps);
                    } catch (VimException e) {
                        log.error(e.getMessage());
                        if (log.isDebugEnabled())
                            log.error(e.getMessage(), e);
                    } catch (VimDriverException e) {
                        log.error(e.getMessage());
                        if (log.isDebugEnabled())
                            log.error(e.getMessage(), e);
                    }
                    vnfcInstancesFuturePerVDU.add(allocate);
                }
                //Print ids of deployed VNFCInstances
                for (Future<VNFCInstance> vnfcInstanceFuture : vnfcInstancesFuturePerVDU) {
                    try {
                        VNFCInstance vnfcInstance = vnfcInstanceFuture.get();
                        vdu.getVnfc_instance().add(vnfcInstance);
                        log.debug("Created VNFCInstance with id: " + vnfcInstance);
                    } catch (InterruptedException e) {
                        log.error(e.getMessage());
                        if (log.isDebugEnabled())
                            log.error(e.getMessage(), e);
                        //throw new RuntimeException(e.getMessage(), e);
                    } catch (ExecutionException e) {
                        log.error(e.getMessage());
                        if (log.isDebugEnabled())
                            log.error(e.getMessage(), e);
                        //throw new RuntimeException(e.getMessage(), e);
                    }
                }
            }
        log.trace("I've finished initialization of vnfr " + virtualNetworkFunctionRecord.getName() + " in facts there are only " + virtualNetworkFunctionRecord.getLifecycle_event().size() + " events");
        return virtualNetworkFunctionRecord;
    }

    @Override
    public void query() {
        throw new UnsupportedOperationException();
    }

    @Override
    public VirtualNetworkFunctionRecord scale(Action scaleInOrOut, VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFCInstance component, Object scripts, VNFRecordDependency dependency) throws Exception {
        //TODO implement scale
        return virtualNetworkFunctionRecord;
    }

    @Override
    public void checkInstantiationFeasibility() {
        throw new UnsupportedOperationException();
    }

    @Override
    public VirtualNetworkFunctionRecord heal(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFCInstance component, String cause) throws Exception{
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateSoftware() {
        throw new UnsupportedOperationException();
    }

    @Override
    public VirtualNetworkFunctionRecord modify(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFRecordDependency dependency) {
        return virtualNetworkFunctionRecord;
    }

    @Override
    public void upgradeSoftware() {
        throw new UnsupportedOperationException();
    }

    @Override
    public VirtualNetworkFunctionRecord terminate(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        log.info("Terminating vnfr with id " + virtualNetworkFunctionRecord.getId());
        Set<Event> events = lifecycleManagement.listEvents(virtualNetworkFunctionRecord);
        //if (events.contains(Event.SCALE))
        try {
            elasticityManagement.deactivate(virtualNetworkFunctionRecord.getParent_ns_id(), virtualNetworkFunctionRecord.getId());
        } catch (NotFoundException e) {
            log.warn(e.getMessage());
            if (log.isDebugEnabled())
                log.error(e.getMessage(), e);
        } catch (VimException e) {
            log.warn(e.getMessage());
            if (log.isDebugEnabled())
                log.error(e.getMessage(), e);
        }

        for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
            Set<VNFCInstance> vnfciToRem = new HashSet<>();
            for (VNFCInstance vnfcInstance : vdu.getVnfc_instance()) {
                log.debug("Releasing resources for vdu with id " + vdu.getId());
                try {
                    resourceManagement.release(vnfcInstance, vdu.getVimInstance());
                    log.debug("Removed VNFCinstance: " + vnfcInstance);
                } catch (VimException e) {
                    log.error(e.getMessage(), e);
                    throw new RuntimeException(e.getMessage(), e);
                }
                vnfciToRem.add(vnfcInstance);
                log.debug("Released resources for vdu with id " + vdu.getId());
            }
            vdu.getVnfc_instance().removeAll(vnfciToRem);
        }
        log.info("Terminated vnfr with id " + virtualNetworkFunctionRecord.getId());
        try {
            applicationManagement.deleteByVnfrId(virtualNetworkFunctionRecord.getId());
            mediaServerManagement.deleteByVnfrId(virtualNetworkFunctionRecord.getId());
            managedVnfrRepository.deleteByVnfrId(virtualNetworkFunctionRecord.getId());
        } catch (NotFoundException e) {
            log.warn(e.getMessage());
        }
        return virtualNetworkFunctionRecord;
    }

    @Override
    public void handleError(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        log.error("Error arrised.");
    }

    @Override
    public VirtualNetworkFunctionRecord start(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws VimException, NotFoundException, VimDriverException {
        log.debug("Initializing Nubomedia MediaServers:");
        for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
            for (VNFCInstance vnfcInstance : vdu.getVnfc_instance()) {
                try {
                    mediaServerManagement.add(virtualNetworkFunctionRecord.getId(), vnfcInstance);
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                }
            }
        }
        //TODO where to set it to active?
        virtualNetworkFunctionRecord.setStatus(Status.ACTIVE);
        elasticityManagement.activate(virtualNetworkFunctionRecord.getParent_ns_id(), virtualNetworkFunctionRecord.getId());
        return virtualNetworkFunctionRecord;
    }

    @Override
    public VirtualNetworkFunctionRecord configure(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        /**
         * This message should never arrive!
         */
        return virtualNetworkFunctionRecord;
    }

    @Override
    protected void setup() {
        super.setup();
//        try {
//            int amqpPort = 5672;
//            Registry registry = LocateRegistry.createRegistry(registryport);
//            log.debug("Registry created: ");
//            log.debug(registry.toString() + " has: " + registry.list().length + " entries");
//            PluginStartup.startPluginRecursive("./plugins", true, "localhost", "" + amqpPort, 5, "admin", "openbaton", "15672");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        Utils.loadExternalProperties(properties);
        Utils.isNfvoStarted(properties.getProperty("nfvo.ip"), properties.getProperty("nfvo.port"));
        //elasticityManagement.initilizeVim();
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
        //throw new UnsupportedOperationException();
    }
}
