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
                ", stunServer=" + stunServer +
                ", turnServer=" + turnServer +
                '}';
    }

    public static class Capacity {

        private String max;

        public String getMax() {
            return max;
        }

        public void setMax(String max) {
            this.max = max;
        }

        @Override
        public String toString() {
            return "Capacity{" +
                    "max='" + max + '\'' +
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

        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        @Override
        public String toString() {
            return "StunServer{" +
                    "url='" + url + '\'' +
                    '}';
        }
    }

    public static class TurnServer {

        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        @Override
        public String toString() {
            return "TurnServer{" +
                    "url='" + url + '\'' +
                    '}';
        }
    }
}
