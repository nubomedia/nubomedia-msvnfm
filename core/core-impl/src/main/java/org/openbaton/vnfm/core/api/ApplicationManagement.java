package org.openbaton.vnfm.core.api;

import org.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.Application;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.vnfm.repositories.ApplicationRepository;
import org.openbaton.vnfm.repositories.VirtualNetworkFunctionRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * Created by mpa on 01.10.15.
 */
@Service
@Scope
public class ApplicationManagement implements org.openbaton.vnfm.core.interfaces.ApplicationManagement {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private VirtualNetworkFunctionRecordRepository virtualNetworkFunctionRecordRepository;

    @Override
    public Application add(Application application) throws NotFoundException {
        VirtualNetworkFunctionRecord virtualNetworkFunctionRecord = virtualNetworkFunctionRecordRepository.findOne(application.getVnfr_id());
        if (virtualNetworkFunctionRecord == null) {
            throw new NotFoundException("Not Found VNFR with id: " + application.getVnfr_id());
        }
        for (VirtualDeploymentUnit vdu : virtualNetworkFunctionRecord.getVdu()) {
            for (VNFCInstance vnfcInstance : vdu.getVnfc_instance()) {
                if (vnfcInstance.getFloatingIps() != null) {
                    application.setIp(vnfcInstance.getFloatingIps());
                }
            }
        }
        return applicationRepository.save(application);
    }

    @Override
    public void delete(String vnfrId, String appId) {
        VirtualNetworkFunctionRecord virtualNetworkFunctionRecord = virtualNetworkFunctionRecordRepository.findFirstById(vnfrId);
        applicationRepository.delete(appId);
    }

    @Override
    public Iterable<Application> query() {
        return applicationRepository.findAll();
    }

    @Override
    public Application query(String id) {
        return applicationRepository.findFirstById(id);
    }

    @Override
    public Application update(Application application, String id) {
        //TODO Update inner fields
        return applicationRepository.save(application);
    }
}
