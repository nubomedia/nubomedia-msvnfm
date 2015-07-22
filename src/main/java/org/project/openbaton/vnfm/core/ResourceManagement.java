package org.project.openbaton.vnfm.core;

import javassist.NotFoundException;
import org.project.openbaton.catalogue.mano.common.DeploymentFlavour;
import org.project.openbaton.catalogue.mano.descriptor.VNFComponent;
import org.project.openbaton.catalogue.mano.descriptor.VNFDConnectionPoint;
import org.project.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.*;
import org.project.openbaton.clients.exceptions.VimDriverException;
import org.project.openbaton.clients.interfaces.ClientInterfaces;
import org.project.openbaton.common.vnfm_sdk.utils.UtilsJMS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.AsyncResult;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * Created by mpa on 07.07.15.
 */

@SpringBootApplication
@ComponentScan(basePackages = "org.project.openbaton.clients")
public class ResourceManagement {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    private ClientInterfaces clientInterfaces;

    private JmsTemplate jmsTemplate;

    public void init(JmsTemplate jmsTemplate, ClientInterfaces clientInterfaces) {
        this.jmsTemplate = jmsTemplate;
        this.clientInterfaces = clientInterfaces;
    }

    public Future<String> allocate(VirtualNetworkFunctionRecord vnfr, VirtualDeploymentUnit vdu) throws NotFoundException{
        //Initialize VimInstance
        VimInstance vimInstance = vdu.getVimInstance();
        log.trace("Initializing " + vimInstance);
        clientInterfaces.init(vimInstance);
        log.debug("initialized VimInstance");
        //Set Hostname
        vdu.setHostname(vnfr.getName() + "-" + vdu.getId().substring((vdu.getId().length() - 5), vdu.getId().length() - 1));
        //Fetch image id
        String image_id = getImageId(vdu);
        //Fetch flavor id
        String flavor_id = getFlavorID(vnfr, vdu);
        //Collect network ids
        Set<String> networks = getNetworkIds(vdu);

        log.trace(""+vnfr);
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
        return new AsyncResult<String>(vdu.getId());
    }

    public void release(VirtualNetworkFunctionRecord vnfr, VirtualDeploymentUnit vdu) throws NotFoundException {
        clientInterfaces.init(vdu.getVimInstance());
        //Get server from cloud environment
        Server server = null;
        List<Server> serverList = clientInterfaces.listServer();
        for (Server tmpServer : serverList) {
            if (vdu.getExtId().equals(tmpServer.getExtId())) {
                server = tmpServer;
                break;
            }
        }
        if (server == null) {
            throw new NotFoundException("Not found Server with id " + vdu.getExtId());
        }
        //Remove associated ips
        for (String network : server.getIps().keySet()) {
            for (String ip : server.getIps().get(network)) {
                vnfr.getVnf_address().remove(ip);
            }
        }
        //Terminate server and wait unitl finished
        clientInterfaces.deleteServerByIdAndWait(vdu.getExtId());
        //Remove corresponding vdu from vnfr
        vnfr.getVdu().remove(vdu);
    }

    public int getMeasurementResults(VirtualDeploymentUnit vdu, String metric) {
        long time = System.currentTimeMillis();
        if (time / 1000 / 60 % 2 == 0) {
            return 80;
        } else {
            return 20;
        }
    }

    public String getImageId(VirtualDeploymentUnit vdu) throws NotFoundException {
        //Fetch image id
        String image_id = null;
        if (vdu.getVm_image() != null && vdu.getVm_image().size() > 0) {
            for (String image : vdu.getVm_image()) {
                for (NFVImage nfvImage : vdu.getVimInstance().getImages()) {
                    if (image.equals(nfvImage.getName()) || image.equals(nfvImage.getExtId())) {
                        image_id = nfvImage.getExtId();
                        break;
                    }
                if (image_id != null)
                    break;
                }
            }
        }
        if (image_id == null)
            throw new NotFoundException("Image(s): " + vdu.getVm_image() + " not found.");
        return image_id;
    }

    public String getFlavorID(VirtualNetworkFunctionRecord vnfr, VirtualDeploymentUnit vdu) throws NotFoundException {
        String flavor_id = null;
        for (DeploymentFlavour flavor : vdu.getVimInstance().getFlavours()) {
            if (vnfr.getDeployment_flavour_key().equals(flavor.getFlavour_key())) {
                flavor_id = flavor.getExtId();
                break;
            }
        }
        if (flavor_id == null)
            throw new NotFoundException("Flavor: " + vnfr.getDeployment_flavour_key() + " not found.");
        return flavor_id;
    }

    public Set<String> getNetworkIds(VirtualDeploymentUnit vdu) {
        Set<String> networks = new HashSet<String>();
        for (VNFComponent vnfc: vdu.getVnfc()) {
            for (VNFDConnectionPoint vnfdConnectionPoint : vnfc.getConnection_point()) {
                networks.add(vnfdConnectionPoint.getExtId());
            }
        }
        return networks;
    }

    public boolean grantLifecycleOperation(VirtualNetworkFunctionRecord vnfr) throws JMSException, NamingException {
        CoreMessage coreMessage = new CoreMessage();
        coreMessage.setAction(Action.GRANT_OPERATION);
        coreMessage.setPayload(vnfr);
        UtilsJMS.sendToQueue(coreMessage, "vnfm-core-actions");

        jmsTemplate.setPubSubDomain(true);
        jmsTemplate.setPubSubNoLocal(true);
        CoreMessage message = (CoreMessage) ((ObjectMessage) jmsTemplate.receive("core-vnfm-actions")).getObject();
        jmsTemplate.setPubSubDomain(false);
        jmsTemplate.setPubSubNoLocal(false);
        if (message.getAction() == Action.ERROR) {
            coreMessage = new CoreMessage();
            coreMessage.setAction(Action.ERROR);
            coreMessage.setPayload(vnfr);
            UtilsJMS.sendToQueue(coreMessage, "vnfm-core-actions");
            return false;
        }
        return true;
    }

}
