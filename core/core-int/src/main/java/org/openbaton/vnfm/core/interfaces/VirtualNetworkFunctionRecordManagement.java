package org.openbaton.vnfm.core.interfaces;


import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;

/**
 * Created by mpa on 01.10.15.
 */
public interface VirtualNetworkFunctionRecordManagement {
    VirtualNetworkFunctionRecord add(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord);

    void delete(String id);

    Iterable<VirtualNetworkFunctionRecord> query();

    VirtualNetworkFunctionRecord query(String id);

    VirtualNetworkFunctionRecord update(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, String id);
}
