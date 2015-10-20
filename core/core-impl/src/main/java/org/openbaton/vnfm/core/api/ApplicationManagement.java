package org.openbaton.vnfm.core.api;

import org.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.Application;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.vnfm.catalogue.Status;
import org.openbaton.vnfm.catalogue.VNFCInstancePoints;
import org.openbaton.vnfm.repositories.ApplicationRepository;
import org.openbaton.vnfm.repositories.VNFCInstancePointsRepository;
import org.openbaton.vnfm.repositories.VirtualNetworkFunctionRecordRepository;
import org.openbaton.vnfm.repositories.VnrfNfvoToVnfmRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Set;

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

    @Autowired
    private VNFCInstancePointsRepository vnfcInstancePointsRepository;

    @Autowired
    private VnrfNfvoToVnfmRepository vnrfNfvoToVnfmRepository;

    @Override
    public Application add(Application application) throws NotFoundException {
        String vnfrVnfmId = vnrfNfvoToVnfmRepository.findVnfrVnfmIdByVnfrNfvoId(application.getVnfr_id()).getVnfrVnfmId();
        if (vnfrVnfmId == null) {
            throw new NotFoundException("Not Found VNFR with id: " + application.getVnfr_id());
        }
        Set<VNFCInstancePoints> vnfcInstancePointses = vnfcInstancePointsRepository.findAllByVNFR(vnfrVnfmId);
        for (VNFCInstancePoints vnfcInstancePoints : vnfcInstancePointses) {
            if (vnfcInstancePoints.getVnfcInstance().getFloatingIps() != null) {
                application.setIp(vnfcInstancePoints.getVnfcInstance().getFloatingIps());
                vnfcInstancePoints.setStatus(Status.ACTIVE);
                vnfcInstancePoints.setUsedPoints(Integer.toString(application.getPoints()));
                break;
            }
        }
        if (application.getIp() == null) {
            throw new NotFoundException("Not found FloatingIp on VNFR");
        }
        return applicationRepository.save(application);
    }

    @Override
    public void delete(String vnfrId, String appId) throws NotFoundException {
        String vnfrVnfmId = vnrfNfvoToVnfmRepository.findOne(vnfrId).getVnfrVnfmId();
        if (vnfrVnfmId == null) {
            throw new NotFoundException("Not Found VNFR with id: " + vnfrId);
        }
        VirtualNetworkFunctionRecord virtualNetworkFunctionRecord = virtualNetworkFunctionRecordRepository.findOne(vnfrId);
        applicationRepository.delete(appId);
    }

    @Override
    public Iterable<Application> query() {
        return applicationRepository.findAll();
    }

    @Override
    public Application query(String id) {
        return applicationRepository.findOne(id);
    }

    @Override
    public Application update(Application application, String id) {
        //TODO Update inner fields
        return applicationRepository.save(application);
    }
}
