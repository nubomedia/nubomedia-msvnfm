/*
 *
 *  * Copyright (c) 2015 Technische Universität Berlin
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *         http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *
 */

package org.openbaton.vnfm.catalogue;

import org.openbaton.catalogue.util.IdGenerator;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by mpa on 19.10.15.
 */
@Entity
public class MediaServer implements Serializable {
  /**
   * ID of the Application
   */
  @Id private String id;
  @Version private int hb_version = 0;

  private String vnfrId;

  private String vnfcInstanceId;

  private String hostName;

  private String ip;

  private int usedPoints;

  private int maxCapacity;

  @Enumerated(EnumType.STRING)
  private Status status;

  @PrePersist
  public void ensureId() {
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

  public int getMaxCapacity() {
    return maxCapacity;
  }

  public void setMaxCapacity(int maxCapacity) {
    this.maxCapacity = maxCapacity;
  }

  @Override
  public String toString() {
    return "MediaServer{"
        + "id='"
        + id
        + '\''
        + ", hb_version="
        + hb_version
        + ", vnfrId='"
        + vnfrId
        + '\''
        + ", vnfcInstanceId='"
        + vnfcInstanceId
        + '\''
        + ", hostName='"
        + hostName
        + '\''
        + ", ip='"
        + ip
        + '\''
        + ", usedPoints="
        + usedPoints
        + ", maxCapacity="
        + maxCapacity
        + ", status="
        + status
        + '}';
  }
}
