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

package org.openbaton.vnfm.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

/**
 * Created by mpa on 25.01.16.
 */
@Service
@ConfigurationProperties(prefix="mediaserver")
@PropertySource("classpath:mediaserver.properties")
public class MediaServerProperties {

    private Capacity capacity;

    private Monitor monitor;

    public History getHistory() {
        return history;
    }

    public void setHistory(History history) {
        this.history = history;
    }

    private History history;

    private StunServer stunServer;

    private TurnServer turnServer;

    public Capacity getCapacity() {
        return capacity;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public StunServer getStunServer() {
        return stunServer;
    }

    public TurnServer getTurnServer() {
        return turnServer;
    }

    public void setCapacity(Capacity capacity) {
        this.capacity = capacity;
    }

    public void setMonitor(Monitor monitor) {
        this.monitor = monitor;
    }

    public void setStunServer(StunServer stunServer) {
        this.stunServer = stunServer;
    }

    public void setTurnServer(TurnServer turnServer) {
        this.turnServer = turnServer;
    }

    @Override
    public String toString() {
        return "MediaServerProperties{" +
                "capacity=" + capacity +
                ", monitor=" + monitor +
                ", history=" + history +
                ", stunServer=" + stunServer +
                ", turnServer=" + turnServer +
                '}';
    }

    public static class Capacity {

        private int max;

        public int getMax() {
            return max;
        }

        public void setMax(int max) {
            this.max = max;
        }

        @Override
        public String toString() {
            return "Capacity{" +
                    "max='" + max + '\'' +
                    '}';
        }
    }

    public static class History {

        private int length;

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        @Override
        public String toString() {
            return "History{" +
                    "length=" + length +
                    '}';
        }
    }

    public static class Monitor {

        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        @Override
        public String toString() {
            return "Monitor{" +
                    "url='" + url + '\'' +
                    '}';
        }
    }

    public static class StunServer {

        private boolean activate;

        private String address;

        private String port;

        public boolean isActivate() {
            return activate;
        }

        public void setActivate(boolean activate) {
            this.activate = activate;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        @Override
        public String toString() {
            return "StunServer{" +
                    "activate=" + activate +
                    ", address='" + address + '\'' +
                    ", port='" + port + '\'' +
                    '}';
        }
    }

    public static class TurnServer {

        private boolean activate;

        private String url;

        private String username;

        private String password;

        public boolean isActivate() {
            return activate;
        }

        public void setActivate(boolean activate) {
            this.activate = activate;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        @Override
        public String toString() {
            return "TurnServer{" +
                    "activate=" + activate +
                    ", url='" + url + '\'' +
                    ", username='" + username + '\'' +
                    ", password='" + password + '\'' +
                    '}';
        }
    }
}
