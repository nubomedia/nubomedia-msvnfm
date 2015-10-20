/*
 * Copyright (c) 2015 Fraunhofer FOKUS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openbaton.vnfm.api;

import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.Application;
import org.openbaton.vnfm.core.interfaces.ApplicationManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/vnfr")
public class RestApplication {

    //	TODO add log prints
	private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ApplicationManagement applicationManagement;

    /**
     * Adds a new VNF software Image to the image repository
     *
     * @param application : Application to add
     * @param vnfrId : ID of VNFR to add the App
     * @return Application: The Application filled with values from the core
     */
    @RequestMapping(value = "{vnfrId}/app", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Application create(@PathVariable("vnfrId") String vnfrId, @RequestBody Application application) throws NotFoundException {
        log.warn("Registering new Application");
        application.setVnfr_id(vnfrId);
        return applicationManagement.add(application);
    }

    /**
     * Removes the Application from the Application repository
     *
     * @param appId : The application's id to be deleted
     */
    @RequestMapping(value = "{vnfrId}/app/{appId}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("vnfrId") String vnfrId, @PathVariable("appId") String appId) {
        applicationManagement.delete(vnfrId, appId);
    }
}
