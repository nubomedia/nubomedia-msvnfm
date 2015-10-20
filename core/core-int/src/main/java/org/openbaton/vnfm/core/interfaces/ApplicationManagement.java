package org.openbaton.vnfm.core.interfaces;

import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.Application;


/**
 * Created by mpa on 01.10.15.
 */
public interface ApplicationManagement {
    Application add(Application application) throws NotFoundException;

    void delete(String vnfrId, String appId) throws NotFoundException;

    Iterable<Application> query();

    Application query(String id);

    Application update(Application application, String id);
}
