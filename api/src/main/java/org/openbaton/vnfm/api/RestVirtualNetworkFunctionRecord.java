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
import org.openbaton.vnfm.core.interfaces.VirtualNetworkFunctionRecordManagement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/vnf-records")
public class RestVirtualNetworkFunctionRecord {

    //	TODO add log prints
//	private Logger log = LoggerFactory.getLogger(this.getClass());


    @Autowired
    private VirtualNetworkFunctionRecordManagement vnfrManagement;

    /**
     * Adds a new VNF software Image to the image repository
     *
     * @param virtualNetworkFunctionDescriptor : VirtualNetworkFunctionDescriptor to add
     * @return VirtualNetworkFunctionDescriptor: The VirtualNetworkFunctionDescriptor filled with values from the core
     */
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public VirtualNetworkFunctionRecord create(@RequestBody @Valid VirtualNetworkFunctionRecord virtualNetworkFunctionDescriptor) {
        return vnfrManagement.add(virtualNetworkFunctionDescriptor);
    }

    /**
     * Removes the VNF software virtualNetworkFunctionDescriptor from the virtualNetworkFunctionDescriptor repository
     *
     * @param id : The virtualNetworkFunctionDescriptor's id to be deleted
     */
    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") String id) {
        vnfrManagement.delete(id);
    }

    /**
     * Returns the list of the VNF software virtualNetworkFunctionDescriptors available
     *
     * @return List<virtualNetworkFunctionDescriptor>: The list of VNF software virtualNetworkFunctionDescriptors available
     */
    @RequestMapping(method = RequestMethod.GET)
    public Iterable<VirtualNetworkFunctionRecord> findAll() {
        return vnfrManagement.query();
    }

    /**
     * Returns the VNF software virtualNetworkFunctionRecord selected by id
     *
     * @param id : The id of the VNF software virtualNetworkFunctionRecord
     * @return virtualNetworkFunctionRecord: The VNF software virtualNetworkFunctionRecord selected
     */
    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public VirtualNetworkFunctionRecord findById(@PathVariable("id") String id) {
        VirtualNetworkFunctionRecord virtualNetworkFunctionRecord = vnfrManagement.query(id);

        return virtualNetworkFunctionRecord;
    }

    /**
     * Updates the VNF software virtualNetworkFunctionRecord
     *
     * @param virtualNetworkFunctionRecord : the VNF software virtualNetworkFunctionRecord to be updated
     * @param id                           : the id of VNF software virtualNetworkFunctionRecord
     * @return networkServiceRecord: the VNF software virtualNetworkFunctionRecord updated
     */

    @RequestMapping(value = "{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public VirtualNetworkFunctionRecord update(@RequestBody @Valid VirtualNetworkFunctionRecord virtualNetworkFunctionRecord,
                                               @PathVariable("id") String id) {
        return vnfrManagement.update(virtualNetworkFunctionRecord, id);
    }
}
