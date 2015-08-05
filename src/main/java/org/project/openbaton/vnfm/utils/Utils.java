package org.project.openbaton.vnfm.utils;

import javassist.NotFoundException;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.Action;
import org.project.openbaton.catalogue.nfvo.CoreMessage;
import org.project.openbaton.clients.interfaces.ClientInterfaces;
import org.project.openbaton.common.vnfm_sdk.utils.UtilsJMS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

import javax.jms.JMSException;
import javax.naming.NamingException;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by mpa on 29.07.15.
 */
public class Utils {

    private final static Logger log = LoggerFactory.getLogger(Utils.class);

    public static void sendToCore(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, Action action) {
        CoreMessage coreMessage = new CoreMessage();
        coreMessage.setAction(action);
        coreMessage.setPayload(virtualNetworkFunctionRecord);
        try {
            UtilsJMS.sendToQueue(coreMessage, "vnfm-core-actions");
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public static ClientInterfaces getPlugin() throws Exception {
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
                return instance;
            }
        }
        throw new NotFoundException("Plugin for ClientInterfaces was not found in plugins/");
    }



}
