/*
 *
 *  * Copyright (c) 2015 Technische Universität Berlin
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *         http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *
 */

package org.openbaton.vnfm;

import org.openbaton.autoscaling.core.management.ElasticityManagement;
import org.openbaton.catalogue.security.Project;
import org.openbaton.sdk.NFVORequestor;
import org.openbaton.sdk.api.exception.SDKException;
import org.openbaton.vnfm.configuration.NfvoProperties;
import org.openbaton.vnfm.core.HistoryManagement;
import org.openbaton.vnfm.core.MediaServerResourceManagement;
import org.openbaton.vnfm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Created by mpa on 02.02.16.
 */
@Configuration
@EnableAsync
@EnableScheduling
@ComponentScan("org.openbaton.vnfm")
public class MSBeanConfiguration {

    private static Logger logger = LoggerFactory.getLogger(MSBeanConfiguration.class);

    @Autowired
    private NfvoProperties nfvoProperties;

    @Bean
    public MediaServerResourceManagement mediaServerResourceManagement() {
        return new MediaServerResourceManagement();
    }

    @Bean
    public ElasticityManagement elasticityManagement() {
        return new ElasticityManagement();
    }

    @Bean
    public HistoryManagement historyManagement() {
        return new HistoryManagement();
    }

    @Bean
    public NFVORequestor getNFVORequestor() throws SDKException {
        if (!Utils.isNfvoStarted(nfvoProperties.getIp(), nfvoProperties.getPort())) {
            logger.error("NFVO is not available");
            System.exit(1);
        }
        NFVORequestor nfvoRequestor =
                new NFVORequestor(
                        nfvoProperties.getUsername(),
                        nfvoProperties.getPassword(),
                        "*",
                        false,
                        nfvoProperties.getIp(),
                        nfvoProperties.getPort(),
                        "1");
        this.logger.info("Starting the Open Baton Manager Bean");

        try {
            logger.info("Finding NUBOMEDIA project");
            boolean found = false;
            for (Project project : nfvoRequestor.getProjectAgent().findAll()) {
                if (project.getName().equals(nfvoProperties.getProject().getName())) {
                    found = true;
                    nfvoRequestor.setProjectId(project.getId());
                    logger.info("Found NUBOMEDIA project");
                }
            }
            if (!found) {
                logger.info("Not found NUBOMEDIA project");
                logger.info("Creating NUBOMEDIA project");
                Project project = new Project();
                project.setDescription("NUBOMEDIA project");
                project.setName(nfvoProperties.getProject().getName());
                project = nfvoRequestor.getProjectAgent().create(project);
                nfvoRequestor.setProjectId(project.getId());
                logger.info("Created NUBOMEDIA project " + project);
            }
        } catch (SDKException e) {
            logger.warn("Not able to connect to NFVO. Please check the credentials you provided ...");
            return getNFVORequestor();
        } catch (ClassNotFoundException e) {
            throw new SDKException(e.getMessage());
        }
        return nfvoRequestor;
    }
}

