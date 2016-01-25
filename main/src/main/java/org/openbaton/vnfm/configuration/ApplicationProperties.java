package org.openbaton.vnfm.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

/**
 * Created by mpa on 25.01.16.
 */
@Service
@ConfigurationProperties(prefix="application")
@PropertySource("classpath:app.properties")
public class ApplicationProperties {

    private HeartBeat heartBeat;

    public HeartBeat getHeartBeat() {
        return heartBeat;
    }

    public void setHeartBeat(HeartBeat heartBeat) {
        this.heartBeat = heartBeat;
    }

    public static class HeartBeat {

        private String activate;

        private String period;

        private Retries retries;

        public String getActivate() {
            return activate;
        }

        public void setActivate(String activate) {
            this.activate = activate;
        }

        public String getPeriod() {
            return period;
        }

        public void setPeriod(String period) {
            this.period = period;
        }

        public Retries getRetries() {
            return retries;
        }

        public void setRetries(Retries retries) {
            this.retries = retries;
        }

        public static class Retries {

            private String max;

            private String timeout;

            public String getMax() {
                return max;
            }

            public void setMax(String max) {
                this.max = max;
            }

            public String getTimeout() {
                return timeout;
            }

            public void setTimeout(String timeout) {
                this.timeout = timeout;
            }
        }
    }
}
