package org.project.openbaton.vnfm.utils;

import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.project.openbaton.catalogue.nfvo.Action;
import org.project.openbaton.catalogue.nfvo.CoreMessage;
import org.project.openbaton.clients.interfaces.ClientInterfaces;
import org.project.openbaton.common.vnfm_sdk.utils.UtilsJMS;
import org.project.openbaton.monitoring.interfaces.ResourcePerformanceManagement;
import org.project.openbaton.vnfm.exceptions.PluginInstallException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

import javax.jms.JMSException;
import javax.naming.NamingException;
import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Created by mpa on 29.07.15.
 */
public class Utils {

    private final static Logger log = LoggerFactory.getLogger(Utils.class);

    public static void sendToCore(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, Action action) {
        CoreMessage coreMessage = new CoreMessage();
        coreMessage.setAction(action);
        coreMessage.setVirtualNetworkFunctionRecord(virtualNetworkFunctionRecord);
        try {
            UtilsJMS.sendToQueue(coreMessage, "vnfm-core-actions");
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public static ClientInterfaces getVimDriverPlugin(Properties properties) throws Exception {
        List<String> classes = new ArrayList<>();

        Collections.addAll(classes, properties.get("vim-classes").toString().split(";"));

        for (String clazz : classes)
            clazz = clazz.trim();

        File folder = new File(String.valueOf(properties.get("vim-plugin-dir")));
        if (!folder.exists())
            throw new Exception("vim-drivers plugin folder does not exist");
        for (File file : folder.listFiles()) {
            if (file.getName().endsWith(".jar")) {
                try {
                    ClassLoader classLoader = getClassLoader(file.getAbsolutePath());

                    for (String clazz : classes) {
                        log.debug("Loading class: " + clazz);
                        Class c;
                        try {
                            c = classLoader.loadClass(clazz);
                        } catch (ClassNotFoundException e) {
                            continue;
                        }

                        Field f;
                        try {
                            f = c.getField("interfaceVersion");
                        } catch (NoSuchFieldException e) {
                            throw new PluginInstallException("No valid Plugin found");
                        }
                        if (f.get(c).equals(ClientInterfaces.interfaceVersion)) {
                            log.debug("Correct interface Version");
                            ClientInterfaces instance = (ClientInterfaces) c.newInstance();
                            log.debug("instance: " + instance);
                            log.debug("of type: " + instance);
                            return instance;
                        } else
                            throw new PluginInstallException("The interface Version are different: required: " + ClientInterfaces.interfaceVersion + ", provided: " + f.get(c));
                    }
                } catch (MalformedURLException e) {
                    throw new PluginInstallException(e);
                } catch (InstantiationException e) {
                    throw new PluginInstallException(e);
                } catch (IllegalAccessException e) {
                    throw new PluginInstallException(e);
                } catch (SecurityException e) {
                    throw new PluginInstallException(e);
                }
            }
        }
        throw new PluginInstallException("Plugin for ClientInterfaces not found");
    }

    public static ResourcePerformanceManagement getMonitoringPlugin(Properties properties) throws Exception {
        List<String> classes = new ArrayList<>();

        Collections.addAll(classes, properties.get("monitor-classes").toString().split(";"));

        for (String clazz : classes)
            clazz = clazz.trim();


        File folder = new File(String.valueOf(properties.get("monitor-plugin-dir")));
        if (!folder.exists())
            throw new Exception("monitoring plugin folder does not exist");
        for (File file : folder.listFiles()) {
            if (file.getName().endsWith(".jar")) {
                try {
                    ClassLoader classLoader = getClassLoader(file.getAbsolutePath());
                    boolean found = false;
                    for (String clazz : classes) {
                        log.debug("Loading class: " + clazz + " on path " + file.getAbsolutePath());
                        Class c;

                        try {
                            c = classLoader.loadClass(clazz);
                        } catch (ClassNotFoundException e) {
                            continue;
                        }
                        found = true;

                        Field f;
                        try {
                            f = c.getField("interfaceVersion");
                        } catch (NoSuchFieldException e) {
                            throw new PluginInstallException("Not a valid plugin");
                        }

                        if (f.get(c).equals(ResourcePerformanceManagement.interfaceVersion)) {
                            ResourcePerformanceManagement agent = (ResourcePerformanceManagement) c.newInstance();
                            log.debug("instance: " + agent);
                            log.debug("of type: " + agent.getType());
                            return agent;
                        } else
                            throw new PluginInstallException("The interface Version are different: required: " + ClientInterfaces.interfaceVersion + ", provided: " + f.get(c));
                    }
                    if (!found)
                        throw new PluginInstallException("No valid Plugin found");
                } catch (MalformedURLException e) {
                    throw new PluginInstallException(e);
                } catch (InstantiationException e) {
                    throw new PluginInstallException(e);
                } catch (IllegalAccessException e) {
                    throw new PluginInstallException(e);
                }
            }
        }
        throw new PluginInstallException("Plugin for ResourcePerformanceManagement not found");
    }

    public static ClassLoader getClassLoader(String path) throws PluginInstallException, MalformedURLException {
        File jar = new File(path);
        if (!jar.exists())
            throw new PluginInstallException(path + " does not exist");
        ClassLoader parent = ClassUtils.getDefaultClassLoader();
        path = jar.getAbsolutePath();
        log.trace("path is: " + path);
        return new URLClassLoader(new URL[]{new URL("file://" + path)}, parent);
    }

}
