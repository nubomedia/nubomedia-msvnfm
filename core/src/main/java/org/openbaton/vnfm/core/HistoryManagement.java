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

package org.openbaton.vnfm.core;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.openbaton.catalogue.util.IdGenerator;
import org.openbaton.exceptions.NotFoundException;
import org.openbaton.vnfm.catalogue.ManagedVNFR;
import org.openbaton.vnfm.catalogue.MediaServer;
import org.openbaton.vnfm.configuration.MediaServerProperties;
import org.openbaton.vnfm.repositories.ManagedVNFRRepository;
import org.openbaton.vnfm.repositories.MediaServerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.persistence.Id;
import javax.persistence.Version;
import java.awt.*;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Queue;

/**
 * Created by mpa on 01.10.15.
 */
@Service
@Scope
public class HistoryManagement {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MediaServerRepository mediaServerRepository;

    @Autowired
    private ManagedVNFRRepository managedVNFRRepository;

    @Autowired
    private MediaServerProperties mediaServerProperties;

    private HashMap<String, Queue> numberHistory;

    private HashMap<String, Queue> loadHistory;

    @PostConstruct
    private void init() {
        loadHistory = new HashMap<>();
        numberHistory = new HashMap<>();

    }

    public HashMap<String, Queue> getNumberHistory() {
        return numberHistory;
    }

    public Queue getNumberHistory(String vnfrId) throws NotFoundException {
        if (numberHistory.containsKey(vnfrId)) {
            return numberHistory.get(vnfrId);
        } else {
            throw new NotFoundException("Not Found History Entry (number of instances) for VNFR " + vnfrId);
        }
    }

    public void deleteNumberHistory(String vnfrId) throws NotFoundException {
        if (numberHistory.containsKey(vnfrId)) {
            numberHistory.remove(vnfrId);
        } else {
            throw new NotFoundException("Not Found History Entry (number of instances) for VNFR " + vnfrId);
        }
    }

    public HashMap<String, Queue> getLoadHistory() {
        return loadHistory;
    }

    public void deleteLoadHistory(String vnfrId) throws NotFoundException {
        if (loadHistory.containsKey(vnfrId)) {
            loadHistory.remove(vnfrId);
        } else {
            throw new NotFoundException("Not Found History Entry (averaged load) for VNFR " + vnfrId);
        }
    }

    public Queue getLoadHistory(String vnfrId) throws NotFoundException {
        if (loadHistory.containsKey(vnfrId)) {
            return loadHistory.get(vnfrId);
        } else {
            throw new NotFoundException("Not Found History Entry (averaged load) for VNFR " + vnfrId);
        }
    }

    @Scheduled(initialDelay=1000, fixedRate=5000)
    private void collectLoadHistory() {
        Long timestamp = new Date().getTime();
        log.debug("Collecting history of averaged load at timestamp " + timestamp);
        for (ManagedVNFR managedVNFR : managedVNFRRepository.findAll()) {
            log.debug("Collecting history for VNFR " + managedVNFR.getVnfrId());
            double sum = 0;
            int size = 0;
            for (MediaServer mediaServer : mediaServerRepository.findAllByVnrfId(managedVNFR.getVnfrId())) {
                sum = sum + mediaServer.getUsedPoints();
                size++;
            }
            double averageValue;
            if (size > 0) {
                averageValue = sum / size;
            } else {
                averageValue = -1;
            }
            HistoryEntry entry = new HistoryEntry(timestamp, averageValue);

            if (! numberHistory.containsKey(managedVNFR.getVnfrId())) {
                numberHistory.put(managedVNFR.getVnfrId(), new CircularFifoQueue<Point>(mediaServerProperties.getHistory().getLength()));
            }
            numberHistory.get(managedVNFR.getVnfrId()).add(entry);
        }
    }

    @Scheduled(initialDelay=1000, fixedRate=5000)
    private void collectNumberOfInstancesHistory() {
        Long timestamp = new Date().getTime();
        log.debug("Collecting history of number of instances at timestamp " + timestamp);
        for (ManagedVNFR managedVNFR : managedVNFRRepository.findAll()) {
            log.debug("Collecting history for VNFR " + managedVNFR.getVnfrId());
            int size = 0;
            for (MediaServer mediaServer : mediaServerRepository.findAllByVnrfId(managedVNFR.getVnfrId())) {
                size++;
            }
            HistoryEntry entry = new HistoryEntry(timestamp, size);

            if (! numberHistory.containsKey(managedVNFR.getVnfrId())) {
                numberHistory.put(managedVNFR.getVnfrId(), new CircularFifoQueue<Point>(mediaServerProperties.getHistory().getLength()));
            }
            numberHistory.get(managedVNFR.getVnfrId()).add(entry);
        }
    }
}

class HistoryEntry implements Serializable {

    private long timestamp;

    private double value;

    public HistoryEntry(long timestamp, double value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "HistoryEntry{" +
                "timestamp=" + timestamp +
                ", value=" + value +
                '}';
    }
}
