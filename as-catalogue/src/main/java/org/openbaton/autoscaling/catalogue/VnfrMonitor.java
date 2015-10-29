package org.openbaton.autoscaling.catalogue;

import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;

@Service
@Scope
public class VnfrMonitor {

    private HashMap<String, ScalingStatus> states;

    @PostConstruct
    public synchronized void init() {
        states = new HashMap<String, ScalingStatus>();
    }

    public synchronized void addVnfr(String vnfrId) {
        states.put(vnfrId, ScalingStatus.READY);
    }

    public synchronized void removeVnfr(String vnfrId) {
        states.remove(vnfrId);
    }

    public synchronized void setState(String vnfrId, ScalingStatus state) {
        states.put(vnfrId, state);
    }

    public synchronized ScalingStatus getState(String vnfrId) {
        return states.get(vnfrId);
    }

    @Override
    public String toString() {
        return "VnfrMonitor{" +
                "states=" + states +
                '}';
    }

    //    private HashMap<String, VirtualNetworkFunctionRecord> virtualNetworkFunctionRecords;
//
//    @PostConstruct
//    public synchronized void init() {
//        this.virtualNetworkFunctionRecords = new HashMap<>();
//    }
//
//    public synchronized VirtualNetworkFunctionRecord getVNFR(String id) {
//        return virtualNetworkFunctionRecords.get(id);
//    }
//
//    public synchronized void addVNFR(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
//        virtualNetworkFunctionRecords.put(virtualNetworkFunctionRecord.getId(), virtualNetworkFunctionRecord);
//    }
//
//    public synchronized void removeVNFR(String id) {
//        virtualNetworkFunctionRecords.remove(id);
//    }

}
