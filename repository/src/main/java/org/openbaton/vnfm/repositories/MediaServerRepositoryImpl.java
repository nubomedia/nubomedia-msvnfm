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

package org.openbaton.vnfm.repositories;

import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.MediaServer;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by lto on 06/05/15.
 */
public class MediaServerRepositoryImpl implements MediaServerRepositoryCustom {

    @Autowired
    private MediaServerRepository mediaServerRepository;

    @Override
    public Iterable findAllByVnrfId(String vnfrId) {
        Set<MediaServer> entities = new HashSet<MediaServer>();
        Iterable<MediaServer> allEntitites = mediaServerRepository.findAll();
        for (MediaServer mediaServer : allEntitites) {
            if(mediaServer.getVnfrId().equals(vnfrId)) {
                entities.add(mediaServer);
            }
        }
        return (Iterable) entities;
    }

    @Override
    public void deleteByVnfrId(String vnfrId) throws NotFoundException {
        Iterable<MediaServer> entities = findAllByVnrfId(vnfrId);
        if (!entities.iterator().hasNext()) {
            throw new NotFoundException("Not found any MediaServer for VNFR with id: " + vnfrId + "managed by this VNFM");
        } else {
            mediaServerRepository.delete(entities);
        }
    }

    @Override
    public MediaServer findByHostName(String hostName) {
        Iterable<MediaServer> allEntitites = mediaServerRepository.findAll();
        for (MediaServer mediaServer : allEntitites) {
            if(mediaServer.getHostName().equals(hostName)) {
                return mediaServer;
            }
        }
        return null;
    }
}
