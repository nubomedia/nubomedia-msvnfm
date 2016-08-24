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

import org.openbaton.catalogue.nfvo.Action;
import org.openbaton.catalogue.util.IdGenerator;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Version;
import java.io.Serializable;

@Entity
public class ManagedVNFR implements Serializable {
  /**
   * ID of the VnfrNfvoToVnfm
   */
  @Id private String id = IdGenerator.createUUID();
  @Version private int hb_version = 0;

  private String vnfrId;

  private String nsrId;

  private Action task;

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

  public String getVnfrId() {
    return vnfrId;
  }

  public void setVnfrId(String vnfrId) {
    this.vnfrId = vnfrId;
  }

  public String getNsrId() {
    return nsrId;
  }

  public void setNsrId(String nsrId) {
    this.nsrId = nsrId;
  }

  public Action getTask() {
    return task;
  }

  public void setTask(Action task) {
    this.task = task;
  }

  @Override
  public String toString() {
    return "ManagedVNFR{"
        + "id='"
        + id
        + '\''
        + ", hb_version="
        + hb_version
        + ", vnfrId='"
        + vnfrId
        + '\''
        + ", nsrId='"
        + nsrId
        + '\''
        + ", task='"
        + task
        + '\''
        + '}';
  }
}
