package org.openbaton.vnfm.core.api;


import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.ManagedVNFR;
import org.openbaton.vnfm.repositories.ManagedVNFRRepository;
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
public class VirtualNetworkFunctionRecordManagement implements org.openbaton.vnfm.core.interfaces.VirtualNetworkFunctionRecordManagement {

    @Autowired
    private ManagedVNFRRepository managedVNFRRepository;

    @Override
    public Set<ManagedVNFR> query() throws NotFoundException {
        Iterable<ManagedVNFR> managedVNFRs = managedVNFRRepository.findAll();
        if (!managedVNFRs.iterator().hasNext()) {
            throw new NotFoundException("Not found any VNFR managed by this VNFM");
        }
        return fromIterbaleToSet(managedVNFRs);
    }

    @Override
    public Set<ManagedVNFR> query(String vnfrId) throws NotFoundException {
        Iterable<ManagedVNFR> managedVNFRsIterbale = managedVNFRRepository.findByVnfrId(vnfrId);
        if (!managedVNFRsIterbale.iterator().hasNext()) {
            throw new NotFoundException("Not found any VNFR with id: " + vnfrId + " managed by this VNFM");
        }
        return fromIterbaleToSet(managedVNFRsIterbale);
    }

    private Set fromIterbaleToSet(Iterable iterable){
        Set set = new HashSet();
        Iterator iterator = iterable.iterator();
        while (iterator.hasNext()) {
            set.add(iterator.next());
        }
        return set;
    }

}
