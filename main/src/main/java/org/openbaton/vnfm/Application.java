package org.openbaton.vnfm;

import org.openbaton.vnfm.configuration.NfvoProperties;
import org.openbaton.vnfm.configuration.PropertiesConfiguration;
import org.openbaton.vnfm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Created by mpa on 25.01.16.
 */

@Component
public class Application {

    protected static Logger log = LoggerFactory.getLogger(Application.class);

    @Autowired
    private NfvoProperties nfvoProperties;

    public static void main(String[] args) {
//        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(PropertiesConfiguration.class);
//        Application application = applicationContext.getBean(Application.class);
//        application.start(args);
        SpringApplication.run(MediaServerManager.class, args);

    }

    private void start(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(PropertiesConfiguration.class);


        NfvoProperties nfvoProperties = applicationContext.getBean(NfvoProperties.class);
        log.debug(nfvoProperties.getIp());
        log.debug(nfvoProperties.getPort());
        log.debug(nfvoProperties.getUsername());
        log.debug(nfvoProperties.getPassword());
        log.debug(nfvoProperties.getIp());
        if (Utils.isNfvoStarted("localhost" , "8080")) {
            SpringApplication.run(MediaServerManager.class, args);
        } else {
            log.error("NFVO not started");
        }
    }


}
