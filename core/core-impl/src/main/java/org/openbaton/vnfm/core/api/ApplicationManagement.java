package org.openbaton.vnfm.core.api;

import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.Application;
import org.openbaton.vnfm.catalogue.Status;
import org.openbaton.vnfm.catalogue.MediaServer;
import org.openbaton.vnfm.core.interfaces.*;
import org.openbaton.vnfm.core.interfaces.MediaServerManagement;
import org.openbaton.vnfm.core.interfaces.VirtualNetworkFunctionRecordManagement;
import org.openbaton.vnfm.repositories.ApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
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
    private MediaServerManagement mediaServerManagement;

    @Autowired
    private VirtualNetworkFunctionRecordManagement vnfrManagement;

    @Override
    public Application add(Application application) throws NotFoundException {
        if (vnfrManagement.query(application.getVnfr_id()).size() == 0) {
            throw new NotFoundException("Not Found any VNFR with id: " + application.getVnfr_id());
        }
        log.debug("Registering new Application");
        //Check for already existing Application
        if (application.getExtAppId() != null) {
            Application existingApplication = applicationRepository.findAppByExtAppId(application.getExtAppId());
            if (existingApplication != null) {
                log.debug("Application exists already. Returned this one");
                return existingApplication;
            }
        }
        MediaServer mediaServer = mediaServerManagement.queryBestMediaServerByVnfrId(application.getVnfr_id());
        application.setIp(mediaServer.getIp());
        application.setMediaServerId(mediaServer.getId());
        mediaServer.setStatus(Status.ACTIVE);
        mediaServer.setUsedPoints(mediaServer.getUsedPoints() + application.getPoints());
        if (application.getIp() == null) {
            throw new NotFoundException("Not found FloatingIp for any MediaServer on VNFR with id: " + application.getVnfr_id());
        }
        application = applicationRepository.save(application);
        log.info("Registered new Application: " + application);
        return application;
    }

    @Override
    public void delete(String vnfrId, String appId) throws NotFoundException {
        log.debug("Removing Application with id: " + appId + " running on VNFR with id: " + vnfrId);
        Application application = applicationRepository.findOne(appId);
        if (application==null) {
            throw new NotFoundException("Not found application with id: " + application.getId());
        }
        MediaServer mediaServer = mediaServerManagement.query(application.getMediaServerId());
        mediaServer.setUsedPoints(mediaServer.getUsedPoints() - application.getPoints());
        if (mediaServer.getUsedPoints() == 0) {
            mediaServer.setStatus(Status.IDLE);
        }
        applicationRepository.delete(application);
        log.debug("Removed Application with id: " + appId + " running on VNFR with id: " + vnfrId);
    }

    @Override
    public void deleteByVnfrId(String vnfrId) throws NotFoundException {
        log.debug("Removing all Applications running on VNFR with id: " + vnfrId);
        Iterable<Application> appsIterable = applicationRepository.findAppByVnfrId(vnfrId);
        if (!appsIterable.iterator().hasNext()) {
            log.warn("Not found any Applications running on VNFR with id: " + vnfrId);
            return;
        }
        Iterator<Application> iterator = appsIterable.iterator();
        while (iterator.hasNext()) {
            delete(vnfrId, iterator.next().getId());
        }
        log.debug("Removed all Applications running on VNFR with id: " + vnfrId);
    }

    @Override
    public Iterable<Application> query() {
        return applicationRepository.findAll();
    }

    @Override
    public Application query(String id) throws NotFoundException {
        Application application = applicationRepository.findOne(id);
        if (application == null) {
            throw new NotFoundException("Not found Application with id: " + id);
        }
        return application;
    }

    @Override
    public Application query(String vnfrId, String id) throws NotFoundException {
        Application application = applicationRepository.findOne(id);
        if (application == null || !application.getVnfr_id().equals(vnfrId)) {
            throw new NotFoundException("Not found Application with id: " + id + " for VNFR with id: " + vnfrId);
        }
        return application;
    }

    @Override
    public Set<Application> queryByVnfrId(String vnfrId) throws NotFoundException {
        log.debug("Listing all Applications running on VNFR with id: " + vnfrId);
        Iterable<Application> appsIterable = applicationRepository.findAppByVnfrId(vnfrId);
        if (!appsIterable.iterator().hasNext()) {
            throw new NotFoundException("Not found any Applications running on VNFR with id: " + vnfrId);
        }
        return fromIterbaleToSet(appsIterable);
    }

    @Override
    public Application update(Application application, String id) {
        //TODO Update inner fields
        return applicationRepository.save(application);
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
