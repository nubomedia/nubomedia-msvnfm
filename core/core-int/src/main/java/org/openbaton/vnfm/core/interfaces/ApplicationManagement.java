package org.openbaton.vnfm.core.interfaces;

import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.vnfm.catalogue.Application;


/**
 * Created by mpa on 01.10.15.
 */
public interface ApplicationManagement {
    Application add(Application application);

    void delete(String vnfrId, String appId);

    Iterable<Application> query();

    Application query(String id);

    Application update(Application application, String id);
}
