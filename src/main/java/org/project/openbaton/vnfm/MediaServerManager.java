package org.project.openbaton.vnfm;

import javassist.NotFoundException;
import org.project.openbaton.catalogue.mano.common.DeploymentFlavour;
import org.project.openbaton.catalogue.mano.common.Event;
import org.project.openbaton.catalogue.mano.common.LifecycleEvent;
import org.project.openbaton.catalogue.mano.descriptor.VNFComponent;
import org.project.openbaton.catalogue.mano.descriptor.VNFDConnectionPoint;
import org.project.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.*;
import org.project.openbaton.clients.exceptions.VimDriverException;
import org.project.openbaton.clients.interfaces.ClientInterfaces;
import org.project.openbaton.common.vnfm_sdk.jms.AbstractVnfmSpringJMS;
import org.project.openbaton.vnfm.core.ElasticityManagement;
import org.project.openbaton.vnfm.core.LifecycleManagement;
import org.project.openbaton.vnfm.core.ResourceManagement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.ClassUtils;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by lto on 27/05/15.
 */
//@ComponentScan(basePackages = "org.project.openbaton.clients")
public class MediaServerManager extends AbstractVnfmSpringJMS {

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    ResourceManagement resourceManagement;

    @Autowired
    ElasticityManagement elasticityManagement;

    @Autowired
    LifecycleManagement lifecycleManagement;

    private ClientInterfaces clientInterfaces;

    @Override
    public void instantiate(VirtualNetworkFunctionRecord vnfr) {
        log.info("Instantiation of VirtualNetworkFunctionRecord " + vnfr.getName());
        log.trace("Instantiation of VirtualNetworkFunctionRecord " + vnfr);
        log.debug("Number of events: " + vnfr.getLifecycle_event().size());

        Set<Event> events = lifecycleManagement.listEvents(vnfr);
        if (events.contains(Event.ALLOCATE)) {
            List<Future<String>> ids = new ArrayList<>();
            try {
                //GrantingLifecycleOperation for initial Allocation
                vnfr = resourceManagement.grantLifecycleOperation(vnfr);
                //Allocate Resources
                for(VirtualDeploymentUnit vdu : vnfr.getVdu()) {
                    ids.add(resourceManagement.allocate(vnfr, vdu));
                }
                //Print ids of deployed VDUs
                for(Future<String> id : ids) {
                    try {
                        log.debug("Created VDU with id: " + id.get());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
                //Put EVENT ALLOCATE to history
                lifecycleManagement.removeEvent(vnfr, Event.ALLOCATE);
            } catch (JMSException e) {
                log.error(e.getMessage(), e);
            } catch (NamingException e) {
                log.error(e.getMessage(), e);
            } catch (NotFoundException e) {
                log.error(e.getMessage(), e);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
        //elasticityManagement.init(vnfr);
        //elasticityManagement.activate();
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

    @Override
    public void run(String... args) throws Exception {
        getPlugin();
        resourceManagement.init(jmsTemplate, clientInterfaces);
        super.run(args);

    }

    private void getPlugin() throws Exception {
        File folder = new File("./plugins");
        if (!folder.exists())
            throw new Exception("plugins does not exist");
        for (File f : folder.listFiles()) {
            if (f.getName().endsWith(".jar")) {
                ClassLoader parent = ClassUtils.getDefaultClassLoader();
                String path = f.getAbsolutePath();
                ClassLoader classLoader = new URLClassLoader(new URL[]{new URL("file://" + path)}, parent);
                URL url = null;
                String type = null;
                if (url == null) {
                    url = classLoader.getResource("org/project/openbaton/clients/interfaces/client/test/TestClient.class");
                    type = "test";
                }
                if (url == null) {
                    url = classLoader.getResource("org/project/openbaton/clients/interfaces/client/openstack/OpenstackClient.class");
                    type = "openstack";
                }
                if (url == null) {
                    url = classLoader.getResource("org/project/openbaton/clients/interfaces/client/amazon/AmazonClient.class");
                    type = "amazon";
                }
                if (url == null)
                    throw new Exception("No ClientInterfaces known were found");

                log.trace("URL: " + url.toString());
                log.trace("type is: " + type);
                Class aClass = null;
                switch (type) {
                    case "test":
                        aClass = classLoader.loadClass("org.project.openbaton.clients.interfaces.client.test.TestClient");

                        break;
                    case "openstack":
                        aClass = classLoader.loadClass("org.project.openbaton.clients.interfaces.client.openstack.OpenstackClient");
                        break;
                    case "amazon":
                        break;
                    default:
                        throw new Exception("No type found");
                }
                ClientInterfaces instance = (ClientInterfaces) aClass.newInstance();
                log.debug("instance: " + instance);
                this.clientInterfaces = instance;
                break;
            }
        }
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(MediaServerManager.class, args);
    }
}
