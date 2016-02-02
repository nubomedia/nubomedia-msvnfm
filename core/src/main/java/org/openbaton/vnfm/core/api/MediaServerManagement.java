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

package org.openbaton.vnfm.core.api;

import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.Application;
import org.openbaton.vnfm.catalogue.MediaServer;
import org.openbaton.vnfm.catalogue.Status;
import org.openbaton.vnfm.configuration.MediaServerProperties;
import org.openbaton.vnfm.repositories.ApplicationRepository;
import org.openbaton.vnfm.repositories.MediaServerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by mpa on 01.10.15.
 */
@Service
@Scope
public class MediaServerManagement {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private MediaServerRepository mediaServerRepository;

    @Autowired
    private MediaServerProperties mediaServerProperties;

    public MediaServer add(MediaServer mediaServer) {
        if (mediaServer.getStatus() == null)
            mediaServer.setStatus(Status.IDLE);
        if (mediaServer.getVnfcInstanceId() == null)
            log.error("Not defined VNFCInstanceId of newly created MediaServer");
        if (mediaServer.getVnfrId() == null)
            log.error("Not defined VnfrId of newly created MediaServer");
        if (mediaServer.getIp() == null)
            log.warn("Not defined IP of newly created MediaServer");
        mediaServerRepository.save(mediaServer);
        return mediaServer;
    }

    public MediaServer add(String vnfrId, VNFCInstance vnfcInstance) {
        MediaServer mediaServer = new MediaServer();
        mediaServer.setVnfrId(vnfrId);
        mediaServer.setVnfcInstanceId(vnfcInstance.getId());
        mediaServer.setHostName(vnfcInstance.getHostname());
        //TODO choose the right network
        if (vnfcInstance.getFloatingIps().size() > 0) {
            mediaServer.setIp(vnfcInstance.getFloatingIps().iterator().next().getIp());
        } else {
            log.warn("No FLoating Ip available! Using private ip...");
            if (vnfcInstance.getIps().size() > 0) {
                mediaServer.setIp(vnfcInstance.getIps().iterator().next().getIp());
            } else {
                log.warn("Even not private IP is available!");
            }
        }
        try {
            mediaServer = add(mediaServer);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        log.debug("Created Nubomedia MediaServer: " + mediaServer);
        return mediaServer;
    }

    public void delete(String vnfrId, String hostname) throws NotFoundException {
        log.debug("Removing MediaServer with hostname: " + hostname);
        MediaServer mediaServer = mediaServerRepository.findByHostName(vnfrId, hostname);
        if (mediaServer == null) {
            throw new NotFoundException("Not found MediaServer with hostname: " + hostname + " on VNFR with id: " + vnfrId);
        }
        Iterable<Application> appsIterable = applicationRepository.findAppByMediaServerId(mediaServer.getId());
        if (appsIterable.iterator().hasNext()) {
            log.warn("Removing MediaServer with hostname: " + hostname + " but there are still running Applications: " + fromIterbaleToSet(appsIterable));
            applicationRepository.delete(appsIterable);
        }
        mediaServerRepository.delete(mediaServer);
        log.debug("Removed MediaServer with hostname: " + hostname);
    }

    public void delete(String mediaServerId) throws NotFoundException {
        log.debug("Removing MediaServer with id: " + mediaServerId);
        MediaServer mediaServer = mediaServerRepository.findOne(mediaServerId);
        if (mediaServer == null) {
            throw new NotFoundException("Not found MediaServer with id: " + mediaServerId);
        }
        Iterable<Application> appsIterable = applicationRepository.findAppByMediaServerId(mediaServerId);
        if (appsIterable.iterator().hasNext()) {
            log.warn("Removing MediaServer with id: " + mediaServerId + " but there are still running Applications: " + fromIterbaleToSet(appsIterable));
            applicationRepository.delete(appsIterable);
        } else {
            mediaServerRepository.delete(mediaServer);
            log.debug("Removed MediaServer with id: " + mediaServerId);
        }
    }

    public void deleteByVnfrId(String vnfrId) throws NotFoundException {
        log.debug("Removing all MediaServers running on VNFR with id: " + vnfrId);
        Iterable<MediaServer> mediaServersIterable = mediaServerRepository.findAllByVnrfId(vnfrId);
        if (!mediaServersIterable.iterator().hasNext()) {
            log.warn("Not found any MediaServer for VNFR with id: " + vnfrId);
            return;
        }
        Iterator<MediaServer> iterator = mediaServersIterable.iterator();
        while (iterator.hasNext()) {
            delete(iterator.next().getId());
        }
        log.info("Removed all MediaServers running on VNFR with id: " + vnfrId);
    }

    public Iterable<MediaServer> query() {
        return mediaServerRepository.findAll();
    }

    public MediaServer query(String id) {
        return mediaServerRepository.findOne(id);
    }

    public Set<MediaServer> queryByVnrfId(String vnfr_id) {
        return fromIterbaleToSet(mediaServerRepository.findAllByVnrfId(vnfr_id));
    }

    public MediaServer queryByHostName(String hostName) {
        return mediaServerRepository.findByHostName(hostName);
    }

    public MediaServer queryBestMediaServerByVnfrId(String vnfr_id, int points) throws NotFoundException {
        MediaServer bestMediaServer = null;
        Set<MediaServer> mediaServers = queryByVnrfId(vnfr_id);
        if (mediaServers.size() == 0) {
            throw new NotFoundException("Not found any MediaServer of VNFR with id: " + vnfr_id);
        }
        log.trace("Searching the best MediaServer for VNFR with id: " + vnfr_id + " that requires " + points + " points");
        for (MediaServer mediaServer : mediaServers) {
            log.trace("Checking MediaServer -> " + mediaServer);
            if (mediaServer.getIp() != null) {
                if (bestMediaServer == null) {
                    if (mediaServer.getUsedPoints() + points <= mediaServerProperties.getCapacity().getMax()) {
                        log.trace("This is the first MediaServer found so far that has enough capacity left");
                        bestMediaServer = mediaServer;
                    } else {
                        log.trace("This MediaServer has not enough capacity left");
                    }
                } else if (mediaServer.getUsedPoints() < bestMediaServer.getUsedPoints()) {
                    log.trace("This is the best MediaServer so far");
                    bestMediaServer = mediaServer;
                }
            } else {
                log.warn("Not found any IP for MediaServer (VNFCInstance) with id: " + mediaServer.getVnfcInstanceId());
            }
        }
        if (bestMediaServer == null) {
            throw new NotFoundException("Not found any MediaServer for VNFR with id: " + vnfr_id + ". At least there is no one with an IP assigned and enough capacity. Please try again later.");
        }
        return bestMediaServer;
    }

    public MediaServer update(MediaServer mediaServer, String id) {
        return mediaServerRepository.save(mediaServer);
    }

    private Set fromIterbaleToSet(Iterable iterable) {
        Set set = new HashSet();
        Iterator iterator = iterable.iterator();
        while (iterator.hasNext()) {
            set.add(iterator.next());
        }
        return set;
    }
}
