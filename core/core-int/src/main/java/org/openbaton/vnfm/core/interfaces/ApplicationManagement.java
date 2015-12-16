package org.openbaton.vnfm.core.interfaces;

import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.Application;

import java.util.Set;


/**
 * Created by mpa on 01.10.15.
 */
public interface ApplicationManagement {
    Application add(Application application) throws NotFoundException;

    void delete(String vnfrId, String appId) throws NotFoundException;

    Iterable<Application> query();

    Application query(String id) throws NotFoundException;

    Application query(String vnfrId, String id) throws NotFoundException;

    Set<Application> queryByVnfrId(String vnfrId) throws NotFoundException;

    Application update(Application application, String id);

    void deleteByVnfrId(String vnfrId) throws NotFoundException;

    void heartbeat(String vnfrId, String appId) throws NotFoundException;
}
