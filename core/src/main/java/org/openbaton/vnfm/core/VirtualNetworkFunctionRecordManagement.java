/*
 *
 *  * (C) Copyright 2016 NUBOMEDIA (http://www.nubomedia.eu)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package org.openbaton.vnfm.core;

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
public class VirtualNetworkFunctionRecordManagement {

  @Autowired private ManagedVNFRRepository managedVNFRRepository;

  public Set<ManagedVNFR> query() throws NotFoundException {
    Iterable<ManagedVNFR> managedVNFRs = managedVNFRRepository.findAll();
    if (!managedVNFRs.iterator().hasNext()) {
      throw new NotFoundException("Not found any VNFR managed by this VNFM");
    }
    return fromIterbaleToSet(managedVNFRs);
  }

  public Set<ManagedVNFR> query(String vnfrId) throws NotFoundException {
    Iterable<ManagedVNFR> managedVNFRsIterbale = managedVNFRRepository.findByVnfrId(vnfrId);
    if (!managedVNFRsIterbale.iterator().hasNext()) {
      throw new NotFoundException(
          "Not found any VNFR with id: " + vnfrId + " managed by this VNFM");
    }
    return fromIterbaleToSet(managedVNFRsIterbale);
  }

  private Set fromIterbaleToSet(Iterable iterable) {
    Set set = new HashSet();
    Iterator iterator = iterable.iterator();
    while (iterator.hasNext()) {
      set.add(iterator.next());
    }
    return set;
  }
}
