/*
 *
 *  * Copyright (c) 2015 Technische Universität Berlin
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *         http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *
 */

package org.openbaton.autoscaling.core.execution;

import org.openbaton.autoscaling.core.features.pool.PoolManagement;
import org.openbaton.autoscaling.core.management.ActionMonitor;
import org.openbaton.autoscaling.utils.Utils;
import org.openbaton.catalogue.mano.common.Ip;
import org.openbaton.catalogue.mano.descriptor.VNFComponent;
import org.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.openbaton.catalogue.mano.record.Status;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.catalogue.nfvo.Action;
import org.openbaton.catalogue.nfvo.Item;
import org.openbaton.catalogue.nfvo.VimInstance;
import org.openbaton.catalogue.nfvo.messages.OrVnfmGenericMessage;
import org.openbaton.common.vnfm_sdk.VnfmHelper;
import org.openbaton.common.vnfm_sdk.utils.VnfmUtils;
import org.openbaton.exceptions.MonitoringException;
import org.openbaton.exceptions.NotFoundException;
import org.openbaton.exceptions.VimException;
import org.openbaton.monitoring.interfaces.MonitoringPluginCaller;
import org.openbaton.plugin.utils.RabbitPluginBroker;
import org.openbaton.sdk.NFVORequestor;
import org.openbaton.sdk.api.exception.SDKException;
import org.openbaton.vnfm.catalogue.MediaServer;
import org.openbaton.vnfm.configuration.*;
import org.openbaton.vnfm.core.MediaServerManagement;
import org.openbaton.vnfm.core.MediaServerResourceManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Created by mpa on 27.10.15.
 */
@Service
@Scope("singleton")
public class ExecutionEngine {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ConfigurableApplicationContext context;

    private NFVORequestor nfvoRequestor;

    //@Autowired
    private MediaServerResourceManagement mediaServerResourceManagement;

    //@Autowired
    private ExecutionManagement executionManagement;

    //@Autowired
    private PoolManagement poolManagement;

    private ActionMonitor actionMonitor;

    private VnfmHelper vnfmHelper;

    //@Autowired
    private MediaServerManagement mediaServerManagement;

    @Autowired
    private NfvoProperties nfvoProperties;

    @Autowired
    private AutoScalingProperties autoScalingProperties;

    @Autowired
    private SpringProperties springProperties;

    @Autowired
    private VnfmProperties vnfmProperties;

    private MonitoringPluginCaller client;

    @Autowired
    private MediaServerProperties mediaServerProperties;


//    public ExecutionEngine(Properties properties) {
//        this.properties = properties;
//        this.nfvoRequestor = new NFVORequestor(properties.getProperty("openbaton-username"), properties.getProperty("openbaton-password"), properties.getProperty("openbaton-url"), properties.getProperty("openbaton-port"), "1");
//        resourceManagement = (ResourceManagement) context.getBean("openstackVIM", "15672");
//    }

    @PostConstruct
    public void init() {
        this.mediaServerResourceManagement = context.getBean(MediaServerResourceManagement.class);
        this.mediaServerManagement = context.getBean(MediaServerManagement.class);
        this.executionManagement = context.getBean(ExecutionManagement.class);
        this.poolManagement = context.getBean(PoolManagement.class);
        this.nfvoRequestor = new NFVORequestor(nfvoProperties.getUsername(), nfvoProperties.getPassword(), nfvoProperties.getIp(), nfvoProperties.getPort(), "1");
        //this.resourceManagement = (ResourceManagement) context.getBean("openstackVIM", "15672");
        this.vnfmHelper = (VnfmHelper) context.getBean("vnfmSpringHelperRabbit");
    }

    private MonitoringPluginCaller getClient() {
        return (MonitoringPluginCaller) ((RabbitPluginBroker) context.getBean("rabbitPluginBroker")).getMonitoringPluginCaller(vnfmProperties.getRabbitmq().getBrokerIp(), springProperties.getRabbitmq().getUsername(), springProperties.getRabbitmq().getPassword(), springProperties.getRabbitmq().getPort(), "icinga-agent", "icinga", vnfmProperties.getRabbitmq().getManagement().getPort());
    }

    public void setActionMonitor(ActionMonitor actionMonitor) {
        this.actionMonitor = actionMonitor;
    }

    public VirtualNetworkFunctionRecord scaleOut(VirtualNetworkFunctionRecord vnfr, int numberOfInstances) throws SDKException, NotFoundException {
        log.info("[EXECUTOR] START_SCALE_OUT " + new Date().getTime());
        log.info("Executing scaling-out of VNFR with id: " + vnfr.getId());
        for (int i = 1; i <= numberOfInstances; i++) {
            log.info("[EXECUTOR] ALLOCATE_INSTANCE " + new Date().getTime());
            if (actionMonitor.isTerminating(vnfr.getId())) {
                actionMonitor.finishedAction(vnfr.getId(), org.openbaton.autoscaling.catalogue.Action.TERMINATED);
                return vnfr;
            }
            log.debug("Adding new VNFCInstance -> number " + i + " " + new Date().getTime());
            VNFCInstance vnfcInstance = null;
            for (VirtualDeploymentUnit vdu : vnfr.getVdu()) {
                VimInstance vimInstance = null;
                if (vdu.getVnfc_instance().size() < vdu.getScale_in_out() && (vdu.getVnfc().iterator().hasNext())) {
                    if (autoScalingProperties.getPool().isActivate()) {
                        log.trace("Getting VNFCInstance from pool");
                        //log.info("[EXECUTOR] REQUEST_RESOURCES_POOL " + new Date().getTime());
                        vnfcInstance = poolManagement.getReservedInstance(vnfr.getParent_ns_id(), vnfr.getId(), vdu.getId());
                        //log.info("[EXECUTOR] FINISH_REQUEST_RESOURCES_POOL " + new Date().getTime());
                        if (vnfcInstance != null) {
                            log.debug("Got VNFCInstance from pool -> " + vnfcInstance);
                        } else {
                            log.debug("No VNFCInstance available in pool");
                        }
                    } else {
                        log.debug("Pool is deactivated");
                    }
                    if (vnfcInstance == null) {
                        if (vimInstance == null) {
                            vimInstance = Utils.getVimInstance(vdu.getVimInstanceName(), nfvoRequestor);
                        }
                        VNFComponent vnfComponent = vdu.getVnfc().iterator().next();
                        try {
                            //log.info("[EXECUTOR] ALLOCATE_RESOURCES " + new Date().getTime());
                            vnfcInstance = mediaServerResourceManagement.allocate(vimInstance, vdu, vnfr, vnfComponent).get();
                            //log.info("[EXECUTOR] FINISH_ALLOCATE_RESOURCES " + new Date().getTime());
                        } catch (InterruptedException e) {
                            log.warn(e.getMessage(), e);
                        } catch (ExecutionException e) {
                            log.warn(e.getMessage(), e);
                        } catch (VimException e) {
                            log.warn(e.getMessage(), e);
                        }
                        //nfvoRequestor.getNetworkServiceRecordAgent().createVNFCInstance(vnfr.getParent_ns_id(), vnfr.getId(), vdu.getId(), vnfComponent_new);
                    }
                } else {
                    log.warn("Maximum size of VDU with id: " + vdu.getId() + " reached...");
                }
                if (vnfcInstance != null) {
                    vdu.getVnfc_instance().add(vnfcInstance);
                    actionMonitor.finishedAction(vnfr.getId(), org.openbaton.autoscaling.catalogue.Action.SCALED);
                    log.debug("Added new VNFCInstance -> number " + i + " " + new Date().getTime());
                    break;
                }
            }

            if (vnfcInstance == null) {
                log.warn("Not found any VDU to scale out a VNFComponent. Limits are reached.");
                return vnfr;
                //throw new NotFoundException("Not found any VDU to scale out a VNFComponent. Limits are reached.");
            }
            //vnfr.setStatus(Status.ACTIVE);
            //nfvoRequestor.getNetworkServiceRecordAgent().updateVNFR(nsr_id, vnfr_id, vnfr);
            vnfr = updateVNFR(vnfr);
            //vnfr = nfvoRequestor.getNetworkServiceRecordAgent().getVirtualNetworkFunctionRecord(nsr_id, vnfr_id);
            for (VirtualDeploymentUnit vdu : vnfr.getVdu()) {
                int cores = 1;
                try {
                    cores = Utils.getCpuCoresOfFlavor(vnfr.getDeployment_flavour_key(), vdu.getVimInstanceName(), nfvoRequestor);
                } catch (NotFoundException e) {
                    log.warn(e.getMessage(), e);
                }
                int maxCapacity = cores * mediaServerProperties.getCapacity().getMax();
                for (VNFCInstance vnfcInstance_new : vdu.getVnfc_instance()) {
                    if (vnfcInstance_new.getHostname().equals(vnfcInstance.getHostname())) {
                        mediaServerManagement.add(vnfr.getId(), vnfcInstance_new, maxCapacity);
                    }
                }
            }
            log.info("[EXECUTOR] ADDED_INSTANCE " + new Date().getTime());
        }
        log.info("Executed scaling-out of VNFR with id: " + vnfr.getId());
        log.info("[EXECUTOR] FINISH_SCALE_OUT " + new Date().getTime());
        return vnfr;
    }

    public void scaleOutTo(VirtualNetworkFunctionRecord vnfr, int value) throws SDKException, NotFoundException, VimException {

        int vnfci_counter = 0;
        for (VirtualDeploymentUnit vdu : vnfr.getVdu()) {
            vnfci_counter += vdu.getVnfc_instance().size();
        }
        for (int i = vnfci_counter + 1; i <= value; i++) {
            scaleOut(vnfr, 1);
        }
    }

    public void scaleOutToFlavour(VirtualNetworkFunctionRecord vnfr, String flavour_id) throws SDKException, NotFoundException {
        throw new NotImplementedException();
    }

    public VirtualNetworkFunctionRecord scaleIn(VirtualNetworkFunctionRecord vnfr, int numberOfInstances) throws SDKException, NotFoundException, VimException {
        //VirtualNetworkFunctionRecord vnfr = nfvoRequestor.getNetworkServiceRecordAgent().getVirtualNetworkFunctionRecord(nsr_id, vnfr_id);
        //vnfr.setStatus(Status.SCALE);
        //nfvoRequestor.getNetworkServiceRecordAgent().updateVNFR(nsr_id, vnfr_id, vnfr);
        //vnfr = nfvoRequestor.getNetworkServiceRecordAgent().getVirtualNetworkFunctionRecord(nsr_id, vnfr_id);
        log.info("Executing scaling-in of VNFR with id: " + vnfr.getId());
        for (int i = 1; i <= numberOfInstances; i++) {
            VNFCInstance vnfcInstance_remove = null;
            String mediaServer_remove = null;
            if (actionMonitor.isTerminating(vnfr.getId())) {
                actionMonitor.finishedAction(vnfr.getId(), org.openbaton.autoscaling.catalogue.Action.TERMINATED);
                return vnfr;
            }
            for (VirtualDeploymentUnit vdu : vnfr.getVdu()) {
                VimInstance vimInstance = null;
                if (vdu.getVnfc_instance().size() > 1 && vdu.getVnfc_instance().iterator().hasNext()) {
                    if (vimInstance == null) {
                        vimInstance = Utils.getVimInstance(vdu.getVimInstanceName(), nfvoRequestor);
                    }
                    Set<MediaServer> mediaServers = mediaServerManagement.queryByVnrfId(vnfr.getId());
                    for (MediaServer mediaServer : mediaServers) {
                        if (mediaServer.getUsedPoints() == 0) {
                            if (autoScalingProperties.getTerminationRule().isActivate()) {
                                if (client == null) client = getClient();
                                log.debug("Search for VNFCInstance that meets the termination rule");
                                List<String> hostnames = new ArrayList<>();
                                hostnames.add(mediaServer.getHostName());
                                List<String> metrics = new ArrayList<>();
                                metrics.add(autoScalingProperties.getTerminationRule().getMetric());
                                List<Item> items = new ArrayList<>();
                                try {
                                    items = client.queryPMJob(hostnames, metrics, "15");
                                } catch (MonitoringException e) {
                                    log.error(e.getMessage(), e);
                                }
                                log.debug("Processing measurement results...");
                                if (items.size() == 0) {
                                    log.warn("Not found the expected measurement results for termination rule. Requested metric is: " + autoScalingProperties.getTerminationRule().getMetric());
                                }
                                for (Item item : items) {
                                    if (item.getLastValue().equals(autoScalingProperties.getTerminationRule().getValue())) {
                                        log.debug("Found VNFCInstance that meets termination-rule.");
                                        mediaServer_remove = item.getHostname();
                                        break;
                                    } else {
                                        log.debug("VNFCInstance " + item.getHostname() + " does not meet termination rule -> " + item.getMetric() + "==" + item.getLastValue() + "!=" + autoScalingProperties.getTerminationRule().getValue());
                                    }
                                }
                            } else {
                                mediaServer_remove = mediaServer.getHostName();
                            }
                            if (mediaServer_remove != null) {
                                log.debug("Found MediaServer to scale-in -> " + mediaServer_remove);
                                break;
                            }
                        } else {
                            log.debug("Cannot scale-in MediaServer with name: " + mediaServer.getHostName() + " since applications are still registered to");
                        }
                        //nfvoRequestor.getNetworkServiceRecordAgent().deleteVNFCInstance(vnfr.getParent_ns_id(), vnfr.getId(), vdu.getId(), vnfcInstance_remove.getId());
                    }
                }
                if (mediaServer_remove != null) {
                    for (VNFCInstance vnfcInstance : vdu.getVnfc_instance()) {
                        if (vnfcInstance.getHostname().equals(mediaServer_remove)) {
                            vnfcInstance_remove = vnfcInstance;
                            break;
                        }
                    }
                }
                if (vnfcInstance_remove != null) {
                    mediaServerResourceManagement.release(vnfcInstance_remove, vimInstance);
                    vdu.getVnfc_instance().remove((vnfcInstance_remove));
                    for (Ip ip : vnfcInstance_remove.getIps()) {
                        vnfr.getVnf_address().remove(ip.getIp());
                    }
                    for (Ip ip : vnfcInstance_remove.getFloatingIps()) {
                        vnfr.getVnf_address().remove(ip.getIp());
                    }
                    actionMonitor.finishedAction(vnfr.getId(), org.openbaton.autoscaling.catalogue.Action.SCALED);
                    log.debug("Removed VNFCInstance " + vnfcInstance_remove.getId() + " from VDU " + vdu.getId());
                    mediaServerManagement.delete(vnfr.getId(), vnfcInstance_remove.getHostname());
                    break;
                } else {
                    log.trace("Not found VNFCInstance in VDU with id: " + vdu.getId() + "to scale in");
                }
            }
            if (vnfcInstance_remove == null) {
                log.warn("Not found any VDU to scale in a VNFInstance.");
                //throw new NotFoundException("Not found any VDU to scale in a VNFComponent. Limits are reached.");
            } else {
                vnfr = updateVNFR(vnfr);
            }
        }
        log.info("Executed scaling-in of VNFR with id: " + vnfr.getId());
        return vnfr;
    }

    public void scaleInTo(VirtualNetworkFunctionRecord vnfr, int value) throws SDKException, NotFoundException, VimException {
        int vnfci_counter = 0;
        for (VirtualDeploymentUnit vdu : vnfr.getVdu()) {
            vnfci_counter += vdu.getVnfc_instance().size();
        }
        for (int i = vnfci_counter; i > value; i--) {
            scaleIn(vnfr, 1);
        }
    }

    public void scaleInToFlavour(VirtualNetworkFunctionRecord vnfr, String flavour_id) throws SDKException, NotFoundException {
        throw new NotImplementedException();
    }

    public void startCooldown(String nsr_id, String vnfr_id, long cooldown) {
        List<String> vnfrIds = new ArrayList<>();
        vnfrIds.add(vnfr_id);

        executionManagement.executeCooldown(nsr_id, vnfr_id, cooldown);
//        List<String> vnfrIds = new ArrayList<>();
//        vnfrIds.add(vnfr_id);
//        try {
//            vnfrMonitor.startCooldown(vnfrIds);
//            log.debug("Starting cooldown period (" + cooldown + "s) for VNFR: " + vnfr_id);
//            Thread.sleep(cooldown * 1000);
//            log.debug("Finished cooldown period (" + cooldown + "s) for VNFR: " + vnfr_id);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    public VirtualNetworkFunctionRecord updateVNFRStatus(String nsr_id, String vnfr_id, Status status) throws SDKException {
        VirtualNetworkFunctionRecord vnfr = nfvoRequestor.getNetworkServiceRecordAgent().getVirtualNetworkFunctionRecord(nsr_id, vnfr_id);
        vnfr.setStatus(status);
        return updateVNFR(vnfr);
        //nfvoRequestor.getNetworkServiceRecordAgent().updateVNFR(nsr_id, vnfr_id, vnfr);
    }

    public VirtualNetworkFunctionRecord updateVNFR(VirtualNetworkFunctionRecord vnfr) {
        OrVnfmGenericMessage response = null;
        log.trace("Updating VNFR on NFVO: " + vnfr);
        try {
            response = (OrVnfmGenericMessage) vnfmHelper.sendAndReceive(VnfmUtils.getNfvMessage(Action.UPDATEVNFR, vnfr));
            log.debug("Updated VNFR on NFVO: " + vnfr.getId());
            //response = (OrVnfmGenericMessage) vnfmHelper.sendToNfvo(VnfmUtils.getNfvMessage(Action.UPDATEVNFR, vnfr));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        if (response.getVnfr() == null) {
            log.error("Problems while updating VNFR on NFVO. Returned VNFR is null.");
            return vnfr;
        } else {
            vnfr = response.getVnfr();
        }
        log.trace("Updated VNFR on NFVO: " + vnfr);
        return vnfr;
    }

}
