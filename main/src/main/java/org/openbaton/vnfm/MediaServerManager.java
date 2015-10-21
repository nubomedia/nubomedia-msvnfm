package org.openbaton.vnfm;

import org.openbaton.catalogue.mano.common.Event;
import org.openbaton.catalogue.mano.descriptor.VNFComponent;
import org.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.openbaton.catalogue.mano.record.Status;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VNFRecordDependency;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.common.vnfm_sdk.jms.AbstractVnfmSpringJMS;
import org.openbaton.exceptions.NotFoundException;
import org.openbaton.exceptions.VimException;
import org.openbaton.nfvo.vim_interfaces.resource_management.ResourceManagement;
import org.openbaton.plugin.utils.PluginStartup;
import org.openbaton.vim.drivers.exceptions.VimDriverException;
import org.openbaton.vnfm.catalogue.ManagedVNFR;
import org.openbaton.vnfm.catalogue.MediaServer;
import org.openbaton.vnfm.core.ElasticityManagement;
import org.openbaton.vnfm.core.LifecycleManagement;

import org.openbaton.vnfm.core.interfaces.MediaServerManagement;
import org.openbaton.vnfm.core.interfaces.ApplicationManagement;
import org.openbaton.vnfm.repositories.MediaServerRepository;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by lto on 27/05/15.
 */
@SpringBootApplication
@EntityScan("org.openbaton.vnfm.catalogue")
@ComponentScan("org.openbaton.vnfm.api")
@EnableJpaRepositories("org.openbaton.vnfm")
public class MediaServerManager extends AbstractVnfmSpringJMS {

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
        resourceManagement = (ResourceManagement) context.getBean("openstackVIM", "openstack", 19345);
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
        log.debug("Processing allocation of Recourses for vnfr: " + virtualNetworkFunctionRecord);
        List<Future<VNFCInstance>> vnfcInstances = new ArrayList<>();
        try {
            for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
                log.debug("Creating " + vdu.getVnfc().size() + " VMs");
                String userdata = Utils.getUserdata();
                for (VNFComponent vnfComponent : vdu.getVnfc()) {
                    Future<VNFCInstance> allocate = resourceManagement.allocate(vdu, virtualNetworkFunctionRecord, vnfComponent, userdata, vnfComponent.isExposed());
                    vnfcInstances.add(allocate);
                }
            }
        } catch (VimDriverException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } catch (VimException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        //Print ids of deployed VDUs
        for (Future<VNFCInstance> vnfcInstance : vnfcInstances) {
            try {
                log.debug("Created VNFCInstance with id: " + vnfcInstance.get());
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e.getMessage(), e);
            } catch (ExecutionException e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e.getMessage(), e);
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
    public VirtualNetworkFunctionRecord scale(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFCInstance component, Object scripts, VNFRecordDependency dependency) throws Exception {
        return null;
    }

    @Override
    public void checkInstantiationFeasibility() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void heal() {
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
        if (events.contains(Event.SCALE))
            elasticityManagement.deactivate(virtualNetworkFunctionRecord);

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
    public VirtualNetworkFunctionRecord start(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        log.debug("Initializing Nubomedia MediaServer:");
        for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
            for (VNFCInstance vnfcInstance : vdu.getVnfc_instance()) {
                MediaServer mediaServer = new MediaServer();
                mediaServer.setVnfrId(virtualNetworkFunctionRecord.getId());
                mediaServer.setVnfcInstanceId(vnfcInstance.getId());
                mediaServer.setIp(vnfcInstance.getFloatingIps());
                try {
                    mediaServerManagement.add(mediaServer);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
                log.debug("Created Nubomedia MediaServer: " + mediaServer);
            }
        }
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
    public VirtualNetworkFunctionRecord configure(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        /**
         * This message should never arrive!
         */
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
            PluginStartup.startPluginRecursive("./plugins", true, "localhost", "" + registryport);
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
        throw new UnsupportedOperationException();
    }
}
