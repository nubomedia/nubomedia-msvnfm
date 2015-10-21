package org.openbaton.vnfm.core.interfaces;


import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.ManagedVNFR;

import java.util.Set;

/**
 * Created by mpa on 01.10.15.
 */
public interface VirtualNetworkFunctionRecordManagement {

    Iterable<ManagedVNFR> query() throws NotFoundException;

    Set<ManagedVNFR> query(String vnfrId) throws NotFoundException;

}
