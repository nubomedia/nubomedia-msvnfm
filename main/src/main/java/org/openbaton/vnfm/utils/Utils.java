package org.openbaton.vnfm.utils;

import org.openbaton.monitoring.interfaces.MonitoringPlugin;
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
