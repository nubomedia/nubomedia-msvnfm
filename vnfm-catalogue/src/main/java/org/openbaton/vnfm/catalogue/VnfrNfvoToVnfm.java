/*
 * Copyright (c) 2015 Fraunhofer FOKUS
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openbaton.vnfm.catalogue;

import org.openbaton.catalogue.util.IdGenerator;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;
import java.io.Serializable;

@Entity
public class VnfrNfvoToVnfm implements Serializable{
    /**
     * ID of the VnfrNfvoToVnfm
     */
    @Id
    private String id = IdGenerator.createUUID();
    @Version
    private int hb_version = 0;

    private String vnfrNfvoId;

    private String vnfrVnfmId;

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

    public String getVnfrVnfmId() {
        return vnfrVnfmId;
    }

    public void setVnfrVnfmId(String vnfrVnfmId) {
        this.vnfrVnfmId = vnfrVnfmId;
    }

    public String getVnfrNfvoId() {
        return vnfrNfvoId;
    }

    public void setVnfrNfvoId(String vnfrNfvoId) {
        this.vnfrNfvoId = vnfrNfvoId;
    }

    @Override
    public String toString() {
        return "VnfrNfvoToVnfm{" +
                "id='" + id + '\'' +
                ", hb_version=" + hb_version +
                ", vnfrNfvoId='" + vnfrNfvoId + '\'' +
                ", vnfrVnfmId='" + vnfrVnfmId + '\'' +
                '}';
    }
}