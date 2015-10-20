package org.openbaton.vnfm.catalogue;

import javax.persistence.*;

import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.util.IdGenerator;

import java.io.Serializable;

/**
 * Created by mpa on 19.10.15.
 */
@Entity
public class VNFCInstancePoints implements Serializable {
    /**
     * ID of the Application
     */
    @Id
    private String id = IdGenerator.createUUID();
    @Version
    private int hb_version = 0;

    private String vnfrId;

    @OneToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
    private VNFCInstance vnfcInstance;

    private String usedPoints;

    @Enumerated(EnumType.STRING)
    private Status status;
    private String vnfciId;
    private String usedPoins;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getHb_version() {
        return hb_version;
    }

    public void setHb_version(int hb_version) {
        this.hb_version = hb_version;
    }

    public VNFCInstance getVnfcInstance() {
        return vnfcInstance;
    }

    public void setVnfcInstance(VNFCInstance vnfcInstance) {
        this.vnfcInstance = vnfcInstance;
    }

    public String getUsedPoints() {
        return usedPoints;
    }

    public void setUsedPoints(String usedPoints) {
        this.usedPoints = usedPoints;
    }

    public String getVnfrId() {
        return vnfrId;
    }

    public void setVnfrId(String vnfrId) {
        this.vnfrId = vnfrId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getVnfciId() {
        return vnfciId;
    }

    public void setVnfciId(String vnfciId) {
        this.vnfciId = vnfciId;
    }

    public String getUsedPoins() {
        return usedPoins;
    }

    public void setUsedPoins(String usedPoins) {
        this.usedPoins = usedPoins;
    }

    @Override
    public String toString() {
        return "VNFCInstancePoints{" +
                "id='" + id + '\'' +
                ", hb_version=" + hb_version +
                ", vnfrId='" + vnfrId + '\'' +
                ", vnfcInstance=" + vnfcInstance +
                ", usedPoints='" + usedPoints + '\'' +
                ", status=" + status +
                ", vnfciId='" + vnfciId + '\'' +
                ", usedPoins='" + usedPoins + '\'' +
                '}';
    }
}
