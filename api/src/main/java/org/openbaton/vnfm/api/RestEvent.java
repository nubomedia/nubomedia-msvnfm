/*
 *
 *  * (C) Copyright 2016 NUBOMEDIA (http://www.nubomedia.eu)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package org.openbaton.vnfm.api;

import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.Application;
import org.openbaton.vnfm.core.ApplicationManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/vnfr/{vnfrId}/event")
public class RestEvent {

  //	TODO add log prints
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Autowired private ApplicationManagement applicationManagement;

  /**
   * Adds a new VNF software Image to the image repository
   *
   * @param application : Application to add
   * @param vnfrId : ID of VNFR to add the App
   * @return Application: The Application filled with values from the core
   */
  @RequestMapping(
    value = "",
    method = RequestMethod.POST,
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseStatus(HttpStatus.CREATED)
  public Application create(
      @PathVariable("vnfrId") String vnfrId, @RequestBody Application application)
      throws NotFoundException {
    application.setVnfr_id(vnfrId);
    application = applicationManagement.add(application);
    return application;
  }

  /**
   * Removes the Application from the Application repository
   *
   * @param appId : The application's id to be deleted
   */
  @RequestMapping(value = "{appId}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.OK)
  public void delete(@PathVariable("vnfrId") String vnfrId, @PathVariable("appId") String appId)
      throws NotFoundException {
    applicationManagement.delete(vnfrId, appId);
  }

  /**
   * Lists all the Application for a specific VNFR from the Application repository
   *
   * @param vnfrId : ID of VNFR to add the App
   */
  @RequestMapping(value = "", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public Set<Application> queryAll(@PathVariable("vnfrId") String vnfrId) throws NotFoundException {
    return applicationManagement.queryByVnfrId(vnfrId);
  }

  /**
   * Returns the Application for a specific VNFR from the Application repository
   *
   * @param appId : The application's id to be return
   * @param vnfrId : ID of VNFR of the App
   */
  @RequestMapping(value = "{appId}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public Application query(
      @PathVariable("vnfrId") String vnfrId, @PathVariable("appId") String appId)
      throws NotFoundException {
    return applicationManagement.query(vnfrId, appId);
  }
}
