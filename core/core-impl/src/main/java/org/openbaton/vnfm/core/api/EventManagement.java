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
public class EventManagement {

    protected Logger log = LoggerFactory.getLogger(this.getClass());



}
