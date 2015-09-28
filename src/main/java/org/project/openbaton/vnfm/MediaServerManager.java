package org.project.openbaton.vnfm;

import org.project.openbaton.catalogue.mano.common.Event;
import org.project.openbaton.catalogue.mano.descriptor.VNFComponent;
import org.project.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.project.openbaton.catalogue.mano.record.Status;
import org.project.openbaton.catalogue.mano.record.VNFCInstance;
import org.project.openbaton.catalogue.mano.record.VNFRecordDependency;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.clients.exceptions.VimDriverException;
import org.project.openbaton.common.vnfm_sdk.exception.VnfmSdkException;
import org.project.openbaton.common.vnfm_sdk.jms.AbstractVnfmSpringJMS;
import org.project.openbaton.exceptions.VimException;
import org.project.openbaton.nfvo.plugin.utils.PluginStartup;
import org.project.openbaton.nfvo.vim_interfaces.resource_management.ResourceManagement;
import org.project.openbaton.vnfm.core.ElasticityManagement;
import org.project.openbaton.vnfm.core.LifecycleManagement;
import org.project.openbaton.vnfm.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;

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
    private void initilizeVim() {
        resourceManagement = (ResourceManagement) context.getBean("openstackVIM", "openstack", 19345);
    }

    @Override
    public VirtualNetworkFunctionRecord instantiate(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, Object object) {
        log.info("Instantiation of VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord.getName());
        log.trace("Instantiation of VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord);

        log.debug("Processing GrantLifeCycleOperation for vnfr: " + virtualNetworkFunctionRecord);
        //Granting LifeCycleOperation
        try {
            virtualNetworkFunctionRecord = vnfmHelper.grantLifecycleOperation(virtualNetworkFunctionRecord).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (VnfmSdkException e) {
            e.printStackTrace();
        }

        //Allocation of Resources
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
        log.trace("I've finished initialization of vnf " + virtualNetworkFunctionRecord.getName() + " in facts there are only " + virtualNetworkFunctionRecord.getLifecycle_event().size() + " events");
        return virtualNetworkFunctionRecord;
    }

    @Override
    public void query() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void scale() {
        throw new UnsupportedOperationException();
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
        return virtualNetworkFunctionRecord;
    }

    @Override
    public void handleError(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        log.error("Error arrised.");
    }

    @Override
    public VirtualNetworkFunctionRecord start(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
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
