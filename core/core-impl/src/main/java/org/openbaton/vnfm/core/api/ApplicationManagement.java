package org.openbaton.vnfm.core.api;

import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.Application;
import org.openbaton.vnfm.catalogue.Status;
import org.openbaton.vnfm.catalogue.MediaServer;
import org.openbaton.vnfm.repositories.ApplicationRepository;
import org.openbaton.vnfm.repositories.MediaServerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Created by mpa on 01.10.15.
 */
@Service
@Scope
public class ApplicationManagement implements org.openbaton.vnfm.core.interfaces.ApplicationManagement {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private MediaServerRepository mediaServerRepository;

    @Override
    public Application add(Application application) throws NotFoundException {
        Set<MediaServer> mediaServers = mediaServerRepository.findAllByVNFR(application.getVnfr_id());
        if (mediaServers.size()==0) {
            throw new NotFoundException("Not Found any MediaServer for VNFR with id: " + application.getVnfr_id());
        }
        for (MediaServer mediaServer : mediaServers) {
            if (mediaServer.getIp() != null) {
                application.setIp(mediaServer.getIp());
                application.setMediaServerId(mediaServer.getId());
                mediaServer.setStatus(Status.ACTIVE);
                mediaServer.setUsedPoints(application.getPoints());
                break;
            } else {
                log.warn("Not found FloatingIp for MediaServer (VNFCInstance) with id: " + mediaServer.getVnfcInstanceId());
            }
        }
        if (application.getIp() == null) {
            throw new NotFoundException("Not found FloatingIp for any MediaServer on VNFR with id: " + application.getVnfr_id());
        }
        return applicationRepository.save(application);
    }

    @Override
    public void delete(String vnfrId, String appId) throws NotFoundException {
        Application application = applicationRepository.findOne(appId);
        if (application==null) {
            throw new NotFoundException("Not Found application with id: " + application.getId());
        }
        MediaServer mediaServer = mediaServerRepository.findOne(application.getId());
        mediaServer.setUsedPoints(mediaServer.getUsedPoints() - application.getPoints());
        if (mediaServer.getUsedPoints() == 0) {
            mediaServer.setStatus(Status.IDLE);
        }
        applicationRepository.delete(application);
    }

    @Override
    public Iterable<Application> query() {
        return applicationRepository.findAll();
    }

    @Override
    public Application query(String id) {
        return applicationRepository.findOne(id);
    }

    @Override
    public Application update(Application application, String id) {
        //TODO Update inner fields
        return applicationRepository.save(application);
    }
}
