/*
 *
 *  * (C) Copyright 2016 NUBOMEDIA (http://www.nubomedia.eu)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package org.openbaton.vnfm;

import org.openbaton.autoscaling.core.management.ElasticityManagement;
import org.openbaton.catalogue.mano.descriptor.VNFComponent;
import org.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.openbaton.catalogue.mano.record.Status;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VNFRecordDependency;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.catalogue.nfvo.Action;
import org.openbaton.catalogue.nfvo.Script;
import org.openbaton.catalogue.nfvo.VimInstance;
import org.openbaton.catalogue.nfvo.messages.Interfaces.NFVMessage;
import org.openbaton.common.vnfm_sdk.amqp.AbstractVnfmSpringAmqp;
import org.openbaton.common.vnfm_sdk.utils.VnfmUtils;
import org.openbaton.exceptions.NotFoundException;
import org.openbaton.exceptions.VimException;
import org.openbaton.plugin.utils.PluginStartup;
import org.openbaton.sdk.NFVORequestor;
import org.openbaton.sdk.api.exception.SDKException;
import org.openbaton.vnfm.catalogue.Application;
import org.openbaton.vnfm.catalogue.ManagedVNFR;
import org.openbaton.vnfm.catalogue.MediaServer;
import org.openbaton.vnfm.configuration.*;
import org.openbaton.vnfm.core.ApplicationManagement;
import org.openbaton.vnfm.core.MediaServerManagement;
import org.openbaton.vnfm.core.MediaServerResourceManagement;
import org.openbaton.vnfm.repositories.ManagedVNFRRepository;
import org.openbaton.vnfm.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by lto on 27/05/15.
 */
@SpringBootApplication
@EntityScan("org.openbaton.vnfm.catalogue")
@ComponentScan({
  "org.openbaton.vnfm.api",
  "org.openbaton.autoscaling.api",
  "org.openbaton.autoscaling"
})
@EnableJpaRepositories("org.openbaton.vnfm")
@ContextConfiguration(
  loader = AnnotationConfigContextLoader.class,
  classes = {MSBeanConfiguration.class}
)
public class MediaServerManager extends AbstractVnfmSpringAmqp
    implements ApplicationListener<ContextClosedEvent> {

  @Autowired private ElasticityManagement elasticityManagement;

  @Autowired private ConfigurableApplicationContext context;

  @Autowired private ApplicationManagement applicationManagement;

  @Autowired private MediaServerManagement mediaServerManagement;

  @Autowired private ManagedVNFRRepository managedVnfrRepository;

  @Autowired private NFVORequestor nfvoRequestor;

  @Autowired private NfvoProperties nfvoProperties;

  @Autowired private ApplicationProperties applicationProperties;

  @Autowired private SpringProperties springProperties;
  @Autowired private VnfmProperties vnfmProperties;

  @Autowired private MediaServerProperties mediaServerProperties;

  @Autowired private MediaServerResourceManagement mediaServerResourceManagement;

  /**
   * Vim must be initialized only after the registry is up and plugin registered
   */
  private void initilize() throws SDKException {
    this.mediaServerResourceManagement.initializeClient();
    //resourceManagement = (ResourceManagement) context.getBean("openstackVIM", "15672");
  }

  @Override
  public VirtualNetworkFunctionRecord instantiate(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord,
      Object scripts,
      Map<String, Collection<VimInstance>> vimInstances) {
    Iterable<ManagedVNFR> managedVnfrs =
        managedVnfrRepository.findByVnfrId(virtualNetworkFunctionRecord.getId());
    ManagedVNFR managedVNFR = null;
    if (managedVnfrs.iterator().hasNext()) {
      managedVNFR = managedVnfrs.iterator().next();
    } else {
      managedVNFR = new ManagedVNFR();
      managedVNFR.setNsrId(virtualNetworkFunctionRecord.getParent_ns_id());
      managedVNFR.setVnfrId(virtualNetworkFunctionRecord.getId());
    }
    managedVNFR.setTask(Action.INSTANTIATE);
    managedVnfrRepository.save(managedVNFR);

    log.info(
        "Instantiation of VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord.getName());
    log.trace("Instantiation of VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord);
    /**
     * Allocation of Resources the grant operation is already done before this method
     */
    log.debug("Processing allocation of Recources for vnfr: " + virtualNetworkFunctionRecord);
    for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
      VimInstance vimInstance = vimInstances.get(vdu.getParent_vdu()).iterator().next();
      List<Future<VNFCInstance>> vnfcInstancesFuturePerVDU = new ArrayList<>();
      log.debug("Creating " + vdu.getVnfc().size() + " VMs");
      for (VNFComponent vnfComponent : vdu.getVnfc()) {
        Future<VNFCInstance> allocate = null;
        try {
          allocate =
              mediaServerResourceManagement.allocate(
                  vimInstance, vdu, virtualNetworkFunctionRecord, vnfComponent);
          vnfcInstancesFuturePerVDU.add(allocate);
        } catch (VimException e) {
          log.error(e.getMessage());
          if (log.isDebugEnabled()) log.error(e.getMessage(), e);
        }
      }
      //Print ids of deployed VNFCInstances
      for (Future<VNFCInstance> vnfcInstanceFuture : vnfcInstancesFuturePerVDU) {
        try {
          VNFCInstance vnfcInstance = vnfcInstanceFuture.get();
          vdu.getVnfc_instance().add(vnfcInstance);
          log.debug("Created VNFCInstance with id: " + vnfcInstance);
        } catch (InterruptedException e) {
          log.error(e.getMessage());
          if (log.isDebugEnabled()) log.error(e.getMessage(), e);
        } catch (ExecutionException e) {
          log.error(e.getMessage());
          if (log.isDebugEnabled()) log.error(e.getMessage(), e);
        }
      }
    }
    log.trace(
        "I've finished initialization of vnfr "
            + virtualNetworkFunctionRecord.getName()
            + " in facts there are only "
            + virtualNetworkFunctionRecord.getLifecycle_event().size()
            + " events");
    ManagedVNFR managedVnfr =
        managedVnfrRepository.findByVnfrId(virtualNetworkFunctionRecord.getId()).iterator().next();
    managedVnfr.setTask(Action.START);
    managedVnfrRepository.save(managedVnfr);
    return virtualNetworkFunctionRecord;
  }

  @Override
  public void query() {
    throw new UnsupportedOperationException();
  }

  @Override
  public VirtualNetworkFunctionRecord scale(
      Action scaleInOrOut,
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord,
      VNFComponent component,
      Object scripts,
      VNFRecordDependency dependency)
      throws Exception {
    if (scaleInOrOut.ordinal() == Action.SCALE_OUT.ordinal()) {
      log.debug(
          "Scaling out " + virtualNetworkFunctionRecord.getName() + " and adding " + component);
      List<String> vimInstanceNames = new ArrayList<>();
      VirtualDeploymentUnit vduToUse = null;
      for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
        vimInstanceNames.addAll(vdu.getVimInstanceName());
        vduToUse = vdu;
      }
      VimInstance vimInstance = Utils.getVimInstance(vimInstanceNames, nfvoRequestor);
      VNFCInstance vnfcInstance =
          mediaServerResourceManagement
              .allocate(vimInstance, vduToUse, virtualNetworkFunctionRecord, component)
              .get();
      vduToUse.getVnfc_instance().add(vnfcInstance);
      int cores = 1;
      try {
        cores =
            org.openbaton.autoscaling.utils.Utils.getCpuCoresOfFlavor(
                virtualNetworkFunctionRecord.getDeployment_flavour_key(),
                vduToUse.getVimInstanceName(),
                nfvoRequestor);
      } catch (NotFoundException e) {
        log.warn(e.getMessage(), e);
      }
      int maxCapacity = cores * mediaServerProperties.getCapacity().getMax();
      MediaServer mediaServer =
          mediaServerManagement.add(
              virtualNetworkFunctionRecord.getId(), vnfcInstance, maxCapacity);
    } else {
      log.debug("Scaling in media server " + ((VNFCInstance) component).getHostname());
      mediaServerManagement.delete(
          virtualNetworkFunctionRecord.getId(), ((VNFCInstance) component).getHostname());
    }
    return virtualNetworkFunctionRecord;
  }

  @Override
  public void checkInstantiationFeasibility() {
    throw new UnsupportedOperationException();
  }

  @Override
  public VirtualNetworkFunctionRecord heal(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord,
      VNFCInstance component,
      String cause)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public VirtualNetworkFunctionRecord updateSoftware(
      Script script, VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public VirtualNetworkFunctionRecord modify(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFRecordDependency dependency) {
    return virtualNetworkFunctionRecord;
  }

  @Override
  public void upgradeSoftware() {
    throw new UnsupportedOperationException();
  }

  @Override
  public VirtualNetworkFunctionRecord terminate(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
    log.info("Terminating vnfr with id " + virtualNetworkFunctionRecord.getId());
    ManagedVNFR managedVnfr = null;
    Iterable<ManagedVNFR> managedVnfrs =
        managedVnfrRepository.findByVnfrId(virtualNetworkFunctionRecord.getId());
    if (managedVnfrs.iterator().hasNext()) {
      managedVnfr = managedVnfrs.iterator().next();
    } else {
      managedVnfr = new ManagedVNFR();
      managedVnfr.setNsrId(virtualNetworkFunctionRecord.getParent_ns_id());
      managedVnfr.setVnfrId(virtualNetworkFunctionRecord.getId());
    }
    managedVnfr.setTask(Action.RELEASE_RESOURCES);
    managedVnfrRepository.save(managedVnfr);
    try {
      elasticityManagement
          .deactivate(
              virtualNetworkFunctionRecord.getParent_ns_id(), virtualNetworkFunctionRecord.getId())
          .get(60, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      log.error(e.getMessage(), e);
    } catch (ExecutionException e) {
      log.error(e.getMessage(), e);
    } catch (TimeoutException e) {
      log.error(e.getMessage(), e);
    }
    try {
      virtualNetworkFunctionRecord =
          nfvoRequestor
              .getNetworkServiceRecordAgent()
              .getVirtualNetworkFunctionRecord(
                  virtualNetworkFunctionRecord.getParent_ns_id(),
                  virtualNetworkFunctionRecord.getId());
    } catch (SDKException e) {
      log.error(e.getMessage(), e);
    }
    for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
      Set<VNFCInstance> vnfciToRem = new HashSet<>();
      VimInstance vimInstance = null;
      try {
        vimInstance = Utils.getVimInstance(vdu.getVimInstanceName(), nfvoRequestor);
      } catch (NotFoundException e) {
        log.error(e.getMessage(), e);
      }
      for (VNFCInstance vnfcInstance : vdu.getVnfc_instance()) {
        log.debug("Releasing resources for vdu with id " + vdu.getId());
        try {
          mediaServerResourceManagement.release(vnfcInstance, vimInstance);
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
    } catch (NotFoundException e) {
      log.warn(e.getMessage());
    }
    try {
      managedVnfrRepository.deleteByVnfrId(virtualNetworkFunctionRecord.getId());
    } catch (NotFoundException e) {
      log.warn("ManagedVNFR were not existing and therefore not deletable");
    }
    return virtualNetworkFunctionRecord;
  }

  @Override
  public void handleError(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
    log.error("Error arrised.");
  }

  @Override
  protected void checkEMS(String hostname) {}

  @Override
  public VirtualNetworkFunctionRecord start(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws SDKException {
    ManagedVNFR managedVnfr =
        managedVnfrRepository.findByVnfrId(virtualNetworkFunctionRecord.getId()).iterator().next();
    managedVnfr.setTask(Action.START);
    managedVnfrRepository.save(managedVnfr);
    log.debug("Initializing Nubomedia MediaServers:");
    for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
      int cores = 1;
      try {
        cores =
            Utils.getCpuCoresOfFlavor(
                virtualNetworkFunctionRecord.getDeployment_flavour_key(),
                vdu.getVimInstanceName(),
                nfvoRequestor);
      } catch (NotFoundException e) {
        log.warn(e.getMessage(), e);
      }
      int maxCapacity = cores * mediaServerProperties.getCapacity().getMax();
      for (VNFCInstance vnfcInstance : vdu.getVnfc_instance()) {
        try {
          mediaServerManagement.add(
              virtualNetworkFunctionRecord.getId(), vnfcInstance, maxCapacity);
        } catch (Exception e) {
          log.warn(e.getMessage(), e);
        }
      }
    }
    //TODO where to set it to active?
    virtualNetworkFunctionRecord.setStatus(Status.ACTIVE);
    if (virtualNetworkFunctionRecord.getAuto_scale_policy().size() > 0) {
      try {
        elasticityManagement.activate(
            virtualNetworkFunctionRecord.getParent_ns_id(), virtualNetworkFunctionRecord.getId());
      } catch (NotFoundException e) {
        log.warn(e.getMessage());
        if (log.isDebugEnabled()) {
          log.error(e.getMessage(), e);
        }
      } catch (VimException e) {
        log.warn(e.getMessage());
        if (log.isDebugEnabled()) {
          log.error(e.getMessage(), e);
        }
      }
    } else {
      log.debug("Do not activate Elasticity because there are no AutoScalePolicies defined.");
    }
    managedVnfr =
        managedVnfrRepository.findByVnfrId(virtualNetworkFunctionRecord.getId()).iterator().next();
    managedVnfr.setTask(Action.SCALING);
    managedVnfrRepository.save(managedVnfr);
    return virtualNetworkFunctionRecord;
  }

  public VirtualNetworkFunctionRecord stop(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public VirtualNetworkFunctionRecord startVNFCInstance(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFCInstance vnfcInstance)
      throws Exception {
    log.debug("Starting VNFCInstance " + vnfcInstance.getHostname());
    MediaServer mediaServer = mediaServerManagement.queryByHostName(vnfcInstance.getHostname());
    if (mediaServer == null) {
      //TODO add message to history
    }
    if (mediaServer.getUsedPoints() == 0) {
      mediaServer.setStatus(org.openbaton.vnfm.catalogue.Status.IDLE);
    } else {
      mediaServer.setStatus(org.openbaton.vnfm.catalogue.Status.ACTIVE);
    }
    for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
      for (VNFCInstance mediaServerVNFCI : vdu.getVnfc_instance()) {
        if (mediaServerVNFCI.getHostname().equals(vnfcInstance.getHostname())) {
          mediaServerVNFCI.setState("ACTIVE");
        }
      }
    }
    mediaServerManagement.update(mediaServer);
    return virtualNetworkFunctionRecord;
  }

  @Override
  public VirtualNetworkFunctionRecord stopVNFCInstance(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFCInstance vnfcInstance)
      throws Exception {
    log.debug("Stopping VNFCInstance " + vnfcInstance.getHostname());
    MediaServer mediaServer = mediaServerManagement.queryByHostName(vnfcInstance.getHostname());
    if (mediaServer == null) {
      //TODO add message to history
    }
    mediaServer.setStatus(org.openbaton.vnfm.catalogue.Status.INACTIVE);
    for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
      for (VNFCInstance mediaServerVNFCI : vdu.getVnfc_instance()) {
        if (mediaServerVNFCI.getHostname().equals(vnfcInstance.getHostname())) {
          mediaServerVNFCI.setState("INACTIVE");
        }
      }
    }
    mediaServerManagement.update(mediaServer);
    return virtualNetworkFunctionRecord;
  }

  @Override
  public VirtualNetworkFunctionRecord configure(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
    /**
     * This message should never arrive!
     */
    return virtualNetworkFunctionRecord;
  }

  @Override
  public VirtualNetworkFunctionRecord resume(
      VirtualNetworkFunctionRecord virtualNetworkFunctionRecord,
      VNFCInstance vnfcInstance,
      VNFRecordDependency dependency)
      throws Exception {
    log.warn("resume() not supported");
    return virtualNetworkFunctionRecord;
  }

  @Override
  protected void setup() {
    super.setup();
    context.registerShutdownHook();
  }

  @Override
  public void NotifyChange() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void checkEmsStarted(String hostname) {
    //throw new UnsupportedOperationException();
  }

  private void startPlugins() throws IOException {
    PluginStartup.startPluginRecursive(
        "./plugins",
        true,
        vnfmProperties.getRabbitmq().getBrokerIp(),
        String.valueOf(springProperties.getRabbitmq().getPort()),
        15,
        springProperties.getRabbitmq().getUsername(),
        springProperties.getRabbitmq().getPassword(),
        vnfmProperties.getRabbitmq().getManagement().getPort(),
        "/var/log/nubomedia/plugins-logs");
  }

  private void destroyPlugins() {
    PluginStartup.destroy();
  }

  @PostConstruct
  private void init() throws SDKException {
    if (!Utils.isNfvoStarted(nfvoProperties.getIp(), nfvoProperties.getPort())) {
      log.error("NFVO is not available");
      System.exit(1);
    }
    log.debug("Initializing MS-VNFM");
    try {
      startPlugins();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    initilize();
    for (ManagedVNFR managedVNFR : managedVnfrRepository.findAll()) {
      VirtualNetworkFunctionRecord vnfr = null;
      try {
        vnfr =
            nfvoRequestor
                .getNetworkServiceRecordAgent()
                .getVirtualNetworkFunctionRecord(managedVNFR.getNsrId(), managedVNFR.getVnfrId());
        NFVMessage nfvMessage = null;
        if (vnfr != null && vnfr.getId() != null) {
          if (managedVNFR.getTask() == Action.INSTANTIATE) {
            try {
              List<VimInstance> vimInstances = nfvoRequestor.getVimInstanceAgent().findAll();
              Map<String, Collection<VimInstance>> vimInstancesMap = new HashMap<>();
              for (VirtualDeploymentUnit vdu : vnfr.getVdu()) {
                vimInstancesMap.put(vdu.getId(), new HashSet<VimInstance>());
                for (String vimInstanceName : vdu.getVimInstanceName()) {
                  for (VimInstance vimInstance : vimInstances) {
                    if (vimInstanceName.equals(vimInstance.getName())) {
                      vimInstancesMap.get(vdu.getId()).add(vimInstance);
                    }
                  }
                }
              }
              nfvMessage =
                  VnfmUtils.getNfvMessage(
                      Action.INSTANTIATE, instantiate(vnfr, null, vimInstancesMap));
            } catch (SDKException e) {
              log.error(e.getMessage(), e);
            } catch (ClassNotFoundException e) {
              log.error(e.getMessage(), e);
            }
          } else if (managedVNFR.getTask() == Action.START) {
            nfvMessage = VnfmUtils.getNfvMessage(Action.START, start(vnfr));
          } else if (managedVNFR.getTask() == Action.RELEASE_RESOURCES) {
            nfvMessage = VnfmUtils.getNfvMessage(Action.RELEASE_RESOURCES, terminate(vnfr));
          } else if (managedVNFR.getTask() == Action.SCALING) {
            if (vnfr.getAuto_scale_policy().size() > 0) {
              try {
                elasticityManagement.activate(managedVNFR.getNsrId(), managedVNFR.getVnfrId());
              } catch (NotFoundException e) {
                log.warn(e.getMessage());
                if (log.isDebugEnabled()) {
                  log.error(e.getMessage(), e);
                }
              } catch (VimException e) {
                log.warn(e.getMessage());
                if (log.isDebugEnabled()) {
                  log.error(e.getMessage(), e);
                }
              }
            } else {
              log.debug(
                  "Do not activate Elasticity because there are no AutoScalePolicies defined.");
            }
          }
          if (nfvMessage != null) {
            vnfmHelper.sendToNfvo(nfvMessage);
          }
        } else {
          log.error(
              "VNFR with id: "
                  + managedVNFR.getVnfrId()
                  + " is not available anymore on NFVO-side");
          managedVnfrRepository.delete(managedVNFR);
        }
      } catch (SDKException e) {
        log.error(e.getMessage(), e);
      }
    }
    log.info("Set number of missed heartbeats to 0 for all applications.");
    for (Application app : applicationManagement.query()) {
      try {
        applicationManagement.heartbeat(app.getVnfr_id(), app.getId());
      } catch (NotFoundException e) {
        log.error(e.getMessage(), e);
      }
      log.debug("Set missed heartbeats to 0 for application " + app);
    }
    if (applicationProperties.getHeartbeat().isActivate()) {
      applicationManagement.startHeartbeatCheck();
    }
  }

  @Override
  public void onApplicationEvent(ContextClosedEvent event) {
    Set<Future<Boolean>> pendingTasks = new HashSet<>();
    for (ManagedVNFR managedVNFR : managedVnfrRepository.findAll()) {
      pendingTasks.add(
          elasticityManagement.deactivate(managedVNFR.getNsrId(), managedVNFR.getVnfrId()));
    }
    for (Future<Boolean> pendingTask : pendingTasks) {
      try {
        pendingTask.get(100, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        log.error(e.getMessage(), e);
      } catch (ExecutionException e) {
        log.error(e.getMessage(), e);
      } catch (TimeoutException e) {
        log.error(e.getMessage(), e);
      }
    }
    try {
      Thread.sleep(2500);
    } catch (InterruptedException e) {
      log.error(e.getMessage(), e);
    }
    destroyPlugins();
  }
}
