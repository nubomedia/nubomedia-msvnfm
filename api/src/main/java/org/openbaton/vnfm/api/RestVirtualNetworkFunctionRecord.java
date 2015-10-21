/*
 * Copyright (c) 2015 Fraunhofer FOKUS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openbaton.vnfm.api;

import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.ManagedVNFR;
import org.openbaton.vnfm.core.interfaces.VirtualNetworkFunctionRecordManagement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Set;

@RestController
@RequestMapping("/vnfr")
public class RestVirtualNetworkFunctionRecord {

    //	TODO add log prints
//	private Logger log = LoggerFactory.getLogger(this.getClass());


    @Autowired
    private VirtualNetworkFunctionRecordManagement vnfrManagement;

    /**
     * Returns the list of the VNF software virtualNetworkFunctionDescriptors available
     *
     * @return List<virtualNetworkFunctionDescriptor>: The list of VNF software virtualNetworkFunctionDescriptors available
     */
    @RequestMapping(method = RequestMethod.GET)
    public Iterable<ManagedVNFR> queryAll() throws NotFoundException {
        return vnfrManagement.query();
    }

    /**
     * Returns the VNF software virtualNetworkFunctionRecord selected by id
     *
     * @param vnfrId : The id of the VNF software virtualNetworkFunctionRecord
     * @return virtualNetworkFunctionRecord: The VNF software virtualNetworkFunctionRecord selected
     */
    @RequestMapping(value = "{vnfrId}", method = RequestMethod.GET)
    public Set<ManagedVNFR> queryById(@PathVariable("vnfrId") String vnfrId) throws NotFoundException {
        Set<ManagedVNFR> managedVNFRs = vnfrManagement.query(vnfrId);
        return managedVNFRs;
    }
}
