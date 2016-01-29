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

package org.openbaton.vnfm.core.interfaces;

import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.Application;

import java.util.Set;


/**
 * Created by mpa on 01.10.15.
 */
public interface ApplicationManagement {
    Application add(Application application) throws NotFoundException;

    void delete(String vnfrId, String appId) throws NotFoundException;

    Iterable<Application> query();

    Application query(String id) throws NotFoundException;

    Application query(String vnfrId, String id) throws NotFoundException;

    Set<Application> queryByVnfrId(String vnfrId) throws NotFoundException;

    Application update(Application application, String id);

    void deleteByVnfrId(String vnfrId) throws NotFoundException;

    void heartbeat(String vnfrId, String appId) throws NotFoundException;

    void startHeartbeatCheck();

    void stopHeartbeatCheck();
}
