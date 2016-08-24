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

package org.openbaton.vnfm.repositories;

import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.Application;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by lto on 06/05/15.
 */
public class ApplicationRepositoryImpl implements ApplicationRepositoryCustom {

  @Autowired private ApplicationRepository applicationRepository;

  @Override
  public Iterable findAppByVnfrId(String vnfrId) {
    Set<Application> entities = new HashSet<>();
    Iterable<Application> allEntitites = applicationRepository.findAll();
    for (Application application : allEntitites) {
      if (application.getVnfr_id().equals(vnfrId)) {
        entities.add(application);
      }
    }
    return (Iterable) entities;
  }

  @Override
  public Iterable<Application> findAppByMediaServerId(String msId) {
    Set<Application> entities = new HashSet<>();
    Iterable<Application> allEntitites = applicationRepository.findAll();
    for (Application application : allEntitites) {
      if (application.getMediaServerId().equals(msId)) {
        entities.add(application);
      }
    }
    return (Iterable) entities;
  }

  @Override
  public Application findAppByExtAppId(String extAppId) {
    Iterable<Application> allEntitites = applicationRepository.findAll();
    for (Application application : allEntitites) {
      if (application.getExtAppId() != null && application.getExtAppId().equals(extAppId)) {
        return application;
      }
    }
    return null;
  }

  @Override
  public void deleteAppsByVnfrId(String vnfrId) throws NotFoundException {
    Iterable<Application> entities = findAppByVnfrId(vnfrId);
    if (!entities.iterator().hasNext()) {
      throw new NotFoundException("Not Found any Applications running on VNFR with id: " + vnfrId);
    } else {
      applicationRepository.delete(entities);
    }
  }
}
