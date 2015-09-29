package org.openbaton.vnfm.utils;

import org.openbaton.vim.drivers.interfaces.ClientInterfaces;
import org.openbaton.monitoring.interfaces.ResourcePerformanceManagement;
import org.openbaton.vnfm.exceptions.PluginInstallException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ClassUtils;

import java.io.*;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Created by mpa on 29.07.15.
 */
public class Utils {

    private final static Logger log = LoggerFactory.getLogger(Utils.class);

//    public static void sendToCore(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, Action action) {
//        CoreMessage coreMessage = new CoreMessage();
//        coreMessage.setAction(action);
//        coreMessage.setVirtualNetworkFunctionRecord(virtualNetworkFunctionRecord);
//        try {
//            UtilsJMS.sendToQueue(coreMessage, "vnfm-core-actions");
//        } catch (NamingException e) {
//            e.printStackTrace();
//        } catch (JMSException e) {
//            e.printStackTrace();
//        }
//    }

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

    public static String getUserdata() {
        StringBuilder sb = new StringBuilder();
        sb.append(getUserdataFromJar());
        sb.append(getUserdataFromFS());
        return sb.toString();
    }

    public static String getUserdataFromJar() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = {};
        StringBuilder script = new StringBuilder();
        try {
            resources = resolver.getResources("/scripts/*.sh");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        for (Resource resource : resources) {
            InputStream in = null;
            InputStreamReader is = null;
            BufferedReader br = null;
            try {
                in = resource.getInputStream();
                is = new InputStreamReader(in);
                br = new BufferedReader(is);
                String line = br.readLine();
                while (line != null) {
                    script.append(line).append("\n");
                    line = br.readLine();
                }
                script.append("\n");
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            } finally {
                try {
                    br.close();
                    is.close();
                    in.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return script.toString();
    }

    public static String getUserdataFromFS() {
        File folder = new File("/etc/openbaton/scripts/ms-vnfm");
        List<String> lines = new ArrayList<String>();
        for (File file : folder.listFiles()) {
            if (file.getAbsolutePath().endsWith(".sh")) {
                try {
                    lines.addAll(Files.readAllLines(Paths.get(file.getAbsolutePath()), StandardCharsets.UTF_8));
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
            lines.add("\n");
        }
        //Create the script
        StringBuilder script = new StringBuilder();
        for (String line : lines) {
            script.append(line).append("\n");
        }
        return script.toString();
    }

}
