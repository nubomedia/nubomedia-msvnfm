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

import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.Application;
import org.openbaton.vnfm.catalogue.MediaServer;
import org.openbaton.vnfm.core.interfaces.ApplicationManagement;
import org.openbaton.vnfm.core.interfaces.MediaServerManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.print.attribute.standard.Media;
import java.util.*;

@RestController
@RequestMapping("/monitor/vnfr/{vnfrId}")
public class RestMonitor {

    //	TODO add log prints
	private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ApplicationManagement applicationManagement;

    @Autowired
    private MediaServerManagement mediaServerManagement;

    /**
     * Returns the consumed capacity of a VNFR
     *
     * @param vnfrId : ID of VNFR
     * @return consumed_capacity: Capacity consumed by Applications
     */
    @RequestMapping(value = "CONSUMED_CAPACITY", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public String create(@PathVariable("vnfrId") String vnfrId) throws NotFoundException {
//        Iterable<Application> applications = applicationManagement.queryByVnfrId(vnfrId);
//        int consumed_capacity = 0;
//        for (Application application : applications) {
//            consumed_capacity += application.getPoints();
//        }
        Iterable<MediaServer> mediaServers = mediaServerManagement.queryByVnrfId(vnfrId);
        int sum_consumed_capacity = 0;
        int number_of_mediaServers = 0;
        double consumed_capacity = 0;
        for (MediaServer mediaServer : mediaServers) {
            number_of_mediaServers += 1;
            sum_consumed_capacity += mediaServer.getUsedPoints();
        }
        if (sum_consumed_capacity != 0 && number_of_mediaServers != 0) {
            consumed_capacity = sum_consumed_capacity / number_of_mediaServers;
        }
        return Double.toString(consumed_capacity);
    }

    /**
     * Returns the elapsed heartbeat time of each Application running on a specific VNFR
     *
     * @param vnfrId : ID of VNFR
     */
    @RequestMapping(value = "HEARTBEAT_ELAPSED", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> delete(@PathVariable("vnfrId") String vnfrId) throws NotFoundException {
        Iterable<Application> applications = applicationManagement.queryByVnfrId(vnfrId);
        Map<String, String> elapsedHeartbeats = new HashMap<String, String>();
        for (Application application : applications) {
            elapsedHeartbeats.put(application.getId(), Long.toString((new Date().getTime() - application.getHeartbeat().getTime())));
        }
        return elapsedHeartbeats;
    }
}
