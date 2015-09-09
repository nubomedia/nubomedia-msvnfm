package org.project.openbaton.vnfm.core;

import javassist.NotFoundException;
import org.project.openbaton.catalogue.mano.common.DeploymentFlavour;
import org.project.openbaton.catalogue.mano.descriptor.VNFComponent;
import org.project.openbaton.catalogue.mano.descriptor.VNFDConnectionPoint;
import org.project.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.project.openbaton.catalogue.mano.record.VNFCInstance;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.*;
import org.project.openbaton.clients.exceptions.VimDriverException;
import org.project.openbaton.clients.interfaces.ClientInterfaces;
import org.project.openbaton.monitoring.interfaces.ResourcePerformanceManagement;
import org.project.openbaton.vnfm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * Created by mpa on 07.07.15.
 */

@Service
@Scope
public class ResourceManagement {

    protected Logger log = LoggerFactory.getLogger(this.getClass());
    private ClientInterfaces clientInterfaces;

    private ResourcePerformanceManagement resourcePerformanceManagement;

    @Autowired
    private JmsTemplate jmsTemplate;

    public void setClientInterfaces(ClientInterfaces clientInterfaces){
        this.clientInterfaces = clientInterfaces;
    }

    public void setResourcePerformanceManagement(ResourcePerformanceManagement resourcePerformanceManagement) {
        this.resourcePerformanceManagement = resourcePerformanceManagement;
    }

    @Async
    public Future<String> allocate(VirtualNetworkFunctionRecord vnfr, VirtualDeploymentUnit vdu, VNFComponent vnfComponent, boolean wait) throws NotFoundException, VimDriverException {
        //Create VNFCInstance
        VNFCInstance vnfcInstance = new VNFCInstance();
        vnfcInstance.setHostname(vnfr.getName() + "-" + vnfcInstance.getId().substring((vnfcInstance.getId().length() - 5), vnfcInstance.getId().length() - 1));
        vnfcInstance.setVim_id(vdu.getVimInstance().getId());
        //Create ConnectionsPoints of VNFCInstance
        if (vnfcInstance.getConnection_point() == null)
            vnfcInstance.setConnection_point(new HashSet<VNFDConnectionPoint>());
        for (VNFDConnectionPoint connectionPoint : vnfComponent.getConnection_point()) {
            VNFDConnectionPoint connectionPoint_new = new VNFDConnectionPoint();
            connectionPoint_new.setVirtual_link_reference(connectionPoint.getVirtual_link_reference());
            connectionPoint_new.setExtId(connectionPoint.getExtId());
            connectionPoint_new.setName(connectionPoint.getName());
            connectionPoint_new.setType(connectionPoint.getType());
            vnfcInstance.getConnection_point().add(connectionPoint_new);
        }
        //Fetch image id
        String image_id = getImageId(vdu);
        //Fetch flavor id
        String flavor_id = getFlavorID(vnfr, vdu);
        //Collect network ids
        Set<String> networkIds = getNetworkIds(vnfcInstance);
        //Get userdata
        String userdata = Utils.getUserdata();
        //Set the right hostname
        userdata = "echo " + vnfcInstance.getHostname() + " > /etc/hostname\necho " + vdu.getHostname() + " >> /etc/hostname\n" + userdata;
        log.trace(""+vnfr);
        log.trace("");
        log.trace("Params: " + vnfcInstance.getHostname() + " - " + image_id + " - " + flavor_id + " - " + vdu.getVimInstance().getKeyPair() + " - " + networkIds + " - " + vdu.getVimInstance().getSecurityGroups());
        //Launch Server
        Server server = null;
        if (wait)
            server = clientInterfaces.launchInstance(vdu.getVimInstance(), vnfcInstance.getHostname(), image_id, flavor_id, vdu.getVimInstance().getKeyPair(), networkIds, vdu.getVimInstance().getSecurityGroups(), userdata);
        else
            server = clientInterfaces.launchInstanceAndWait(vdu.getVimInstance(), vnfcInstance.getHostname(), image_id, flavor_id, vdu.getVimInstance().getKeyPair(), networkIds, vdu.getVimInstance().getSecurityGroups(), userdata);

        log.debug("launched instance with id " + server.getExtId());
        //Set external ID of VNFCInstance
        vnfcInstance.setVc_id(server.getExtId());

        if (vdu.getVnfc_instance() == null)
            vdu.setVnfc_instance(new HashSet<VNFCInstance>());
        vdu.getVnfc_instance().add(vnfcInstance);

        //Set ips
        for (String network : server.getIps().keySet()) {
            for (String ip : server.getIps().get(network)) {
                vnfr.getVnf_address().add(ip);
            }
        }
        return new AsyncResult<String>(vnfcInstance.getVc_id());
    }

    public void release(VirtualNetworkFunctionRecord vnfr, VirtualDeploymentUnit vdu, VNFCInstance vnfcInstance) throws NotFoundException {
        if (vdu.getVnfc() == null || vdu.getVnfc().size() == 0 )
            return;

        //TODO use the VNFComponent
        //Get server from cloud environment
        Server server = null;
        List<Server> serverList = clientInterfaces.listServer(vdu.getVimInstance());
        for (Server tmpServer : serverList) {
            for (VNFCInstance vnfci : vdu.getVnfc_instance()) {
                if (vnfci.getVc_id().equals(tmpServer.getExtId())) {
                    server = tmpServer;
                    break;
                }
            }
        }
        if (server == null) {
            throw new NotFoundException("Not found Server with id " + vnfcInstance.getVc_id());
        }
        //Remove associated ips
        for (String network : server.getIps().keySet()) {
            for (String ip : server.getIps().get(network)) {
                vnfr.getVnf_address().remove(ip);
            }
        }
        //Terminate server and wait unitl finished
        clientInterfaces.deleteServerByIdAndWait(vdu.getVimInstance(), vnfcInstance.getVc_id());
        //Remove corresponding vdu from vnfr
        vdu.getVnfc_instance().remove(vnfcInstance);
    }

    public synchronized Item getMeasurementResults(VNFCInstance vnfcInstance, String metric, String period) {
        return resourcePerformanceManagement.getMeasurementResults(vnfcInstance, metric, period);
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

    public Set<String> getNetworkIds(VNFCInstance vnfcInstance) {
        Set<String> networks = new HashSet<String>();
        for (VNFDConnectionPoint vnfdConnectionPoint : vnfcInstance.getConnection_point()) {
            networks.add(vnfdConnectionPoint.getExtId());

        }
        return networks;
    }

    public void grantLifecycleOperation(VirtualNetworkFunctionRecord vnfr) {
        CoreMessage coreMessage = new CoreMessage();
        coreMessage.setAction(Action.GRANT_OPERATION);
        coreMessage.setVirtualNetworkFunctionRecord(vnfr);

        final CoreMessage finalCoreMessage = coreMessage;
        MessageCreator messageCreator = new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                ObjectMessage textMessage = session.createObjectMessage(finalCoreMessage);
                return textMessage;
            }
        };
        jmsTemplate.setPubSubDomain(false);
        jmsTemplate.setPubSubNoLocal(false);

        jmsTemplate.send("vnfm-core-actions", messageCreator);
    }
}
