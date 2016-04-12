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

package org.openbaton.vnfm.api;

import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.MediaServer;
import org.openbaton.vnfm.core.MediaServerManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/vnfr/{vnfrId}/media-server")
public class RestMediaServer {

	private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MediaServerManagement mediaServerManagement;

    /**
     * Lists all the MediaServers of a specific VNFR
     *
     * @param vnfrId : ID of VNFR
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Set<MediaServer> queryAll(@PathVariable("vnfrId") String vnfrId) throws NotFoundException {
        return mediaServerManagement.queryByVnrfId(vnfrId);
    }

    /**
     * Returns the number of MediaServers of a specific VNFR
     *
     * @param vnfrId : ID of VNFR
     */
    @RequestMapping(value = "number", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public int queryNumber(@PathVariable("vnfrId") String vnfrId) throws NotFoundException {
        return mediaServerManagement.queryByVnrfId(vnfrId).size();
    }

    /**
     * Returns the load over all MediaServers of a specific VNFR
     *
     * @param vnfrId : ID of VNFR
     */
    @RequestMapping(value = "load", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public double queryLoad(@PathVariable("vnfrId") String vnfrId) throws NotFoundException {
        Set<MediaServer> mediaServers = mediaServerManagement.queryByVnrfId(vnfrId);
        double sum = 0;
        for (MediaServer mediaServer : mediaServers) {
            sum = sum + mediaServer.getUsedPoints();
        }
        return sum / mediaServers.size();
    }
}
