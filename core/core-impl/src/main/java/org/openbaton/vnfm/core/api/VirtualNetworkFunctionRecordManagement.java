package org.openbaton.vnfm.core.api;


import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.vnfm.repositories.VirtualNetworkFunctionRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * Created by mpa on 01.10.15.
 */
@Service
@Scope
public class VirtualNetworkFunctionRecordManagement implements org.openbaton.vnfm.core.interfaces.VirtualNetworkFunctionRecordManagement {

    @Autowired
    private VirtualNetworkFunctionRecordRepository virtualNetworkFunctionRecordRepository;

    @Override
    public VirtualNetworkFunctionRecord add(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        // TODO check integrity of VNFD
        return virtualNetworkFunctionRecordRepository.save(virtualNetworkFunctionRecord);
    }

    @Override
    public void delete(String id) {
        virtualNetworkFunctionRecordRepository.delete(id);
    }

    @Override
    public Iterable<VirtualNetworkFunctionRecord> query() {
        return virtualNetworkFunctionRecordRepository.findAll();
    }

    @Override
    public VirtualNetworkFunctionRecord query(String id) {
        return virtualNetworkFunctionRecordRepository.findFirstById(id);
    }

    @Override
    public VirtualNetworkFunctionRecord update(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, String id) {
        //TODO Update inner fields
        return virtualNetworkFunctionRecordRepository.save(virtualNetworkFunctionRecord);
    }
}
