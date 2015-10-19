package org.openbaton.vnfm.catalogue;

import org.openbaton.catalogue.util.IdGenerator;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;
import java.io.Serializable;

/**
 * Created by mpa on 19.10.15.
 */
@Entity
public class VNFCInstancePoints implements Serializable{
    /**
     * ID of the Application
     */
    @Id
    private String id = IdGenerator.createUUID();
    @Version
    private int hb_version = 0;

    private String vnfcId;
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

    public String getVnfcId() {
        return vnfcId;
    }

    public void setVnfcId(String vnfcId) {
        this.vnfcId = vnfcId;
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
                ", vnfcId='" + vnfcId + '\'' +
                ", usedPoins='" + usedPoins + '\'' +
                '}';
    }
}
