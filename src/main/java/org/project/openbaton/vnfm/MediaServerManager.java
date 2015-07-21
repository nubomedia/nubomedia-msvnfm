package org.project.openbaton.vnfm;

import org.project.openbaton.clients.exceptions.VimDriverException;
import org.project.openbaton.clients.interfaces.ClientInterfaces;
import org.project.openbaton.catalogue.mano.common.DeploymentFlavour;
import org.project.openbaton.catalogue.mano.common.Event;
import org.project.openbaton.catalogue.mano.common.LifecycleEvent;
import org.project.openbaton.catalogue.mano.descriptor.VNFComponent;
import org.project.openbaton.catalogue.mano.descriptor.VNFDConnectionPoint;
import org.project.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.*;

import org.project.openbaton.common.vnfm_sdk.jms.AbstractVnfmSpringJMS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import javax.jms.Message;
import javax.jms.ObjectMessage;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by lto on 27/05/15.
 */
@ComponentScan(basePackages = "org.project.openbaton.clients")
public class MediaServerManager extends AbstractVnfmSpringJMS {

    @Autowired
    @Qualifier("openstackClient")
    private ClientInterfaces clientInterfaces;

    @Override
    public void instantiate(VirtualNetworkFunctionRecord vnfr) {
        log.info("Instantiation of VirtualNetworkFunctionRecord " + vnfr.getName());
        log.trace("Instantiation of VirtualNetworkFunctionRecord " + vnfr);
        boolean allocate = false;

        log.debug("Number of events: " + vnfr.getLifecycle_event().size());

        List<Future<String>> ids = new ArrayList<>();
        for (LifecycleEvent event : vnfr.getLifecycle_event()){
            try {
                if (event.getEvent().ordinal() == Event.ALLOCATE.ordinal()){
                    /**
                     * Grant Op
                     */
                    CoreMessage coreMessage = new CoreMessage();
                    coreMessage.setAction(Action.GRANT_OPERATION);
                    coreMessage.setPayload(vnfr);
                    this.sendMessageToQueue("vnfm-core-actions", coreMessage);

                    this.getJmsTemplate().setPubSubDomain(true);
                    this.getJmsTemplate().setPubSubNoLocal(true);
                    CoreMessage message = (CoreMessage) ((ObjectMessage) this.getJmsTemplate().receive("core-vnfm-actions")).getObject();
                    this.getJmsTemplate().setPubSubDomain(false);
                    this.getJmsTemplate().setPubSubNoLocal(false);
                    if (message.getAction() == Action.ERROR) {
                        coreMessage = new CoreMessage();
                        coreMessage.setAction(Action.ERROR);
                        coreMessage.setPayload(vnfr);
                        this.sendMessageToQueue("vnfm-core-actions", coreMessage);
                        throw new Exception();
                    }

                    if(vnfr.getLifecycle_event_history() == null)
                        vnfr.setLifecycle_event_history(new HashSet<LifecycleEvent>());
                    vnfr.getLifecycle_event_history().add(event);
                    vnfr.getLifecycle_event().remove(event);
                    for(VirtualDeploymentUnit vdu : vnfr.getVdu()) {
                        //Initialize VimInstance
                        VimInstance vimInstance = vdu.getVimInstance();
                        log.trace("Initializing " + vimInstance);
                        clientInterfaces.init(vimInstance);
                        log.debug("initialized VimInstance");
                        //Set Hostname
                        vdu.setHostname(vnfr.getName() + "-" + vdu.getId().substring((vdu.getId().length() - 5), vdu.getId().length() - 1));
                        //Fetch image id
                        String image_id = null;
                        if (vdu.getVm_image() != null && vdu.getVm_image().size() > 0) {
                            for (String image : vdu.getVm_image()) {
                                for (NFVImage nfvImage : vimInstance.getImages()) {
                                    if (image.equals(nfvImage.getName()) || image.equals(nfvImage.getExtId()))
                                        image_id = nfvImage.getExtId();
                                }
                            }
                        }
                        if (image_id == null)
                            throw new Exception("Image(s): " + vdu.getVm_image() + " not found.");
                        //Fetch flavor id
                        String flavor_id = null;
                        for (DeploymentFlavour flavor : vimInstance.getFlavours()) {
                            if (vnfr.getDeployment_flavour_key().equals(flavor.getFlavour_key())) {
                                flavor_id = flavor.getExtId();
                            }
                        }
                        if (flavor_id == null)
                            throw new Exception("Flavor: " + vnfr.getDeployment_flavour_key() + " not found.");
                        //Set networks
                        Set<String> networks = new HashSet<String>();
                        for (VNFComponent vnfc: vdu.getVnfc()) {
                            for (VNFDConnectionPoint vnfdConnectionPoint : vnfc.getConnection_point())
                                networks.add(vnfdConnectionPoint.getExtId());
                        }

                        log.trace("" + vnfr);
                        log.trace("");
                        log.trace("Params: " + vdu.getHostname() + " - " + image_id + " - " + flavor_id + " - " + vimInstance.getKeyPair() + " - " + networks + " - " + vimInstance.getSecurityGroups());
                        //Launch Server
                        Server server = null;
                        try {
                            server = clientInterfaces.launchInstanceAndWait(vdu.getHostname(), image_id, flavor_id, vimInstance.getKeyPair(), networks, vimInstance.getSecurityGroups(), "#userdata");
                        } catch (VimDriverException e) {
                            log.error("Cannot launch vdu.", e);
                        }
                        log.debug("launched instance with id " + server.getExtId());
                        //Set external id
                        vdu.setExtId(server.getExtId());
                        //Set ips
                        for (String network : server.getIps().keySet()) {
                            for (String ip : server.getIps().get(network)) {
                                vnfr.getVnf_address().add(ip);
                            }
                        }
                    }
                    for(Future<String> id : ids) {
                        try {
                            log.debug("Created VDU with id: " + id.get());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                    allocate = true;
                    break;
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        } // for
        log.trace("I've finished initialization of vnf " + vnfr.getName() + " in facts there are only " + vnfr.getLifecycle_event().size() + " events");
        CoreMessage coreMessage = new CoreMessage();
        coreMessage.setAction(Action.INSTANTIATE_FINISH);
        coreMessage.setPayload(vnfr);
        this.sendMessageToQueue("vnfm-core-actions", coreMessage);
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
    public void modify(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        log.trace("Adding relation with VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord);
        log.debug("Adding relation with VirtualNetworkFunctionRecord " + virtualNetworkFunctionRecord.getName());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void upgradeSoftware() {

    }

    @Override
    public void terminate() {

    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(MediaServerManager.class, args);
    }
}
