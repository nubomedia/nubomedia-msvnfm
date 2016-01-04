package org.openbaton.vnfm.catalogue;

import javax.persistence.*;

import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.util.IdGenerator;

import java.io.Serializable;

/**
 * Created by mpa on 19.10.15.
 */
@Entity
public class MediaServer implements Serializable {
    /**
     * ID of the Application
     */
    @Id
    private String id;
    @Version
    private int hb_version = 0;

    private String vnfrId;

    private String vnfcInstanceId;

    private String hostName;

    private String ip;

    private int usedPoints;

    @Enumerated(EnumType.STRING)
    private Status status;

    @PrePersist
    public void ensureId(){
        id = IdGenerator.createUUID();
    }

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

    public String getVnfrId() {
        return vnfrId;
    }

    public void setVnfrId(String vnfrId) {
        this.vnfrId = vnfrId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getUsedPoints() {
        return usedPoints;
    }

    public void setUsedPoints(int usedPoints) {
        this.usedPoints = usedPoints;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getVnfcInstanceId() {
        return vnfcInstanceId;
    }

    public void setVnfcInstanceId(String vnfcInstanceId) {
        this.vnfcInstanceId = vnfcInstanceId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    @Override
    public String toString() {
        return "MediaServer{" +
                "id='" + id + '\'' +
                ", hb_version=" + hb_version +
                ", vnfrId='" + vnfrId + '\'' +
                ", vnfcInstanceId='" + vnfcInstanceId + '\'' +
                ", hostName='" + hostName + '\'' +
                ", ip='" + ip + '\'' +
                ", usedPoints=" + usedPoints +
                ", status=" + status +
                '}';
    }
}
