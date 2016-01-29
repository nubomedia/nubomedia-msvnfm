package org.openbaton.vnfm.core.api;

import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.Application;
import org.openbaton.vnfm.catalogue.Status;
import org.openbaton.vnfm.catalogue.MediaServer;
import org.openbaton.vnfm.configuration.ApplicationProperties;
import org.openbaton.vnfm.configuration.MediaServerProperties;
import org.openbaton.vnfm.core.interfaces.*;
import org.openbaton.vnfm.core.interfaces.MediaServerManagement;
import org.openbaton.vnfm.core.interfaces.VirtualNetworkFunctionRecordManagement;
import org.openbaton.vnfm.repositories.ApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

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

    private ThreadPoolTaskScheduler taskScheduler;

    private ScheduledFuture heartbeatTaskScheduled;

    @Autowired
    private ApplicationProperties applicationProperties;

    @PostConstruct
    public void init() {
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.setPoolSize(10);
        this.taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        this.taskScheduler.setRemoveOnCancelPolicy(true);
        this.taskScheduler.initialize();
    }

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
        MediaServer mediaServer = mediaServerManagement.queryBestMediaServerByVnfrId(application.getVnfr_id(), application.getPoints());
        application.setIp(mediaServer.getIp());
        application.setMediaServerId(mediaServer.getId());
        application.setCreated(new Date());
        application.setHeartbeat(new Date());
        mediaServer.setStatus(Status.ACTIVE);
        mediaServer.setUsedPoints(mediaServer.getUsedPoints() + application.getPoints());
        if (application.getIp() == null) {
            throw new NotFoundException("Not found IP for any MediaServer on VNFR with id: " + application.getVnfr_id());
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
        mediaServerManagement.update(mediaServer, mediaServer.getId());
        applicationRepository.delete(application);
        log.info("Removed Application with id: " + appId + " running on VNFR with id: " + vnfrId);
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
        log.info("Removed all Applications running on VNFR with id: " + vnfrId);
    }

    @Override
    public void heartbeat(String vnfrId, String appId) throws NotFoundException {
        log.debug("Received Heartbeat for Application " + appId + " running on VNFR with id: " + vnfrId);
        Application application = applicationRepository.findOne(appId);
        if (application == null) {
            throw new NotFoundException("Not found Application with id: " + appId + " running on VNFR with id: " + vnfrId);
        }
        if (!application.getVnfr_id().equals(vnfrId)) {
            log.warn("Found Application with id: " + appId + " but this Application does not belongs to the VNFR with id: " + vnfrId);
            throw new NotFoundException("Not found Application with id: " + appId + " running on VNFR with id: " + vnfrId);
        }
        application.setHeartbeat(new Date());
        applicationRepository.save(application);
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

    public void startHeartbeatCheck() {
        log.debug("Starting HeartbeatTask...");
        if (heartbeatTaskScheduled == null) {
            HeartbeatTask heartbeatTask = new HeartbeatTask(this, applicationProperties);
            heartbeatTaskScheduled = taskScheduler.scheduleAtFixedRate(heartbeatTask, applicationProperties.getHeartbeat().getPeriod() * 1000);
            log.debug("Started HeartbeatTask...");
        } else {
            log.warn("HeartbeatTask was already started. Not start it again");
        }
    }

    public void stopHeartbeatCheck() {
        log.debug("Stopping HeartbeatTask...");
        if (heartbeatTaskScheduled != null) {
            heartbeatTaskScheduled.cancel(true);
            log.debug("Stoopped HeartbeatTask...");
        } else {
            log.warn("HeartbeatTask was not running. Cannot stop it");
        }
    }
}

class HeartbeatTask implements Runnable {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    private ApplicationManagement applicationManagement;

    private ApplicationProperties applicationProperties;

    public HeartbeatTask(ApplicationManagement applicationManagement, ApplicationProperties applicationProperties) {
        this.applicationManagement = applicationManagement;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void run() {
        log.debug("Checking Heartbeats of Applications");
        Set<Application> applicationToRemove = new HashSet<>();
        for (Application application : applicationManagement.query()) {
            log.debug("Checking Heartbeat of Application: " + application);
            if (System.currentTimeMillis() - application.getHeartbeat().getTime() < applicationProperties.getHeartbeat().getPeriod() * 1000) {
                log.debug("Last Heartbeat received in time at " + application.getHeartbeat().getTime());
                application.setMissedHeartbeats(0);
            } else {
                log.debug("Heartbeat missed in the last interval");
                application.setMissedHeartbeats(application.getMissedHeartbeats() + 1);
                log.debug("Increased missed Heartbeats by one. Counter: " + application.getMissedHeartbeats());
            }
            applicationManagement.update(application, application.getId());

            if (application.getMissedHeartbeats() >= applicationProperties.getHeartbeat().getRetry().getMax()) {
                log.warn("Reached maximum number of missed Heartbeats. Remove Application.");
                try {
                    applicationManagement.delete(application.getVnfr_id(), application.getId());
                } catch (NotFoundException e) {
                    log.warn(e.getMessage());
                }
            } else if (System.currentTimeMillis() - application.getHeartbeat().getTime() > applicationProperties.getHeartbeat().getRetry().getTimeout() * 1000) {
                log.warn("Reached timeout of Heartbeat. Remove Application.");
                try {
                    applicationManagement.delete(application.getVnfr_id(), application.getId());
                } catch (NotFoundException e) {
                    log.warn(e.getMessage());
                }
            }
        }
    }
}