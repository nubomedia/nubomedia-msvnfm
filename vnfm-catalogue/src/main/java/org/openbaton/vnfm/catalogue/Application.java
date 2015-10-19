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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;
import java.io.Serializable;

@Entity
public class Application implements Serializable{
    /**
     * ID of the Application
     */
    @Id
    private String id;
    @Version
    private int hb_version = 0;

    private String vnfr_id;

    private int points;

    private String ip;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVnfr_id() {
        return vnfr_id;
    }

    public void setVnfr_id(String vnfr_id) {
        this.vnfr_id = vnfr_id;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    public String toString() {
        return "Application{" +
                "id='" + id + '\'' +
                ", hb_version=" + hb_version +
                ", vnfr_id='" + vnfr_id + '\'' +
                ", points=" + points +
                ", ip='" + ip + '\'' +
                '}';
    }
}