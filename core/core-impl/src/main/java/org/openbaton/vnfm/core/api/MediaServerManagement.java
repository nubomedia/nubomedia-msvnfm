package org.openbaton.vnfm.core.api;

import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.Application;
import org.openbaton.vnfm.catalogue.MediaServer;
import org.openbaton.vnfm.catalogue.Status;
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
public class MediaServerManagement implements org.openbaton.vnfm.core.interfaces.MediaServerManagement {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private MediaServerRepository mediaServerRepository;


    @Override
    public MediaServer add(MediaServer mediaServer) {
        if (mediaServer.getStatus() == null)
            mediaServer.setStatus(Status.IDLE);
        if (mediaServer.getVnfcInstanceId() == null)
            log.error("Not defined VNFCInstnaceId of newly created MediaServer");
        if (mediaServer.getVnfrId() == null)
            log.error("Not defined VnfrId of newly created MediaServer");
        if (mediaServer.getIp() == null)
            log.warn("Not defined IP of newly created MediaServer");
        mediaServerRepository.save(mediaServer);
        return mediaServer;
    }

    @Override
    public void delete(String mediaServerId) throws NotFoundException {
        log.debug("Removing MediaServer with id: " + mediaServerId);
        MediaServer mediaServer = mediaServerRepository.findOne(mediaServerId);
        if (mediaServer == null) {
            throw new NotFoundException("Not found MediaServer with id: " + mediaServerId);
        }
        Iterable<Application> appsIterable = applicationRepository.findAppByMediaServerId(mediaServerId);
        if (appsIterable.iterator().hasNext()) {
            log.error("Cannot delete MediaServer with id: " + mediaServerId + " since there are still running Applications: " + fromIterbaleToSet(appsIterable));
        } else {
            mediaServerRepository.delete(mediaServer);
            log.debug("Removed MediaServer with id: " + mediaServerId);
        }
    }

    @Override
    public void deleteByVnfrId(String vnfrId) throws NotFoundException {
        log.debug("Removing all MediaServers running on VNFR with id: " + vnfrId);
        Iterable<MediaServer> mediaServersIterable = mediaServerRepository.findAllByVnrfId(vnfrId);
        if (!mediaServersIterable.iterator().hasNext()) {
            log.error("Not found any MediaServer for VNFR with id: " + vnfrId);
            return;
        }
        Iterator<MediaServer> iterator = mediaServersIterable.iterator();
        while(iterator.hasNext()) {
            delete(iterator.next().getId());
        }
        log.debug("Removed all MediaServers running on VNFR with id: " + vnfrId);
    }

    @Override
    public Iterable<MediaServer> query() {
        return mediaServerRepository.findAll();
    }

    @Override
    public MediaServer query(String id) {
        return mediaServerRepository.findOne(id);
    }

    @Override
    public Set<MediaServer> queryByVnrfId(String vnfr_id) {
        return fromIterbaleToSet(mediaServerRepository.findAllByVnrfId(vnfr_id));
    }

    @Override
    public MediaServer queryByHostName(String hostName) {
        return mediaServerRepository.findByHostName(hostName);
    }

    @Override
    public MediaServer queryBestMediaServerByVnfrId(String vnfr_id) throws NotFoundException {
        MediaServer bestMediaServer = null;
        Set<MediaServer> mediaServers = queryByVnrfId(vnfr_id);
        if (mediaServers.size() == 0) {
            throw new NotFoundException("Not found any MediaServer of VNFR with id: " + vnfr_id);
        }
        for (MediaServer mediaServer : mediaServers) {
            if (mediaServer.getIp() != null) {
                if (bestMediaServer == null) bestMediaServer = mediaServer;
                if (mediaServer.getUsedPoints() < bestMediaServer.getUsedPoints()) {
                    bestMediaServer = mediaServer;
                }
            } else {
                log.warn("Not found FloatingIp for MediaServer (VNFCInstance) with id: " + mediaServer.getVnfcInstanceId());
            }
        }
        if (bestMediaServer == null) {
            throw new NotFoundException("Not found any MediaServer for VNFR with id: " + vnfr_id + ". At least there is no one with a FloatingIp");
        }
        return bestMediaServer;
    }

    @Override
    public MediaServer update(MediaServer mediaServer, String id) {
        throw new UnsupportedOperationException();
    }

    private Set fromIterbaleToSet(Iterable iterable){
        Set set = new HashSet();
        Iterator iterator = iterable.iterator();
        while (iterator.hasNext()) {
            set.add(iterator.next());
        }
        return set;
    }
}
