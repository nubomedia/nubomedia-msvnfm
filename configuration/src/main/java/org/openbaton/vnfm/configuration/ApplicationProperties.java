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

    private Heartbeat heartbeat;

    public Heartbeat getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(Heartbeat heartBeat) {
        this.heartbeat = heartBeat;
    }

    @Override
    public String toString() {
        return "ApplicationProperties{" +
                "heartbeat=" + heartbeat +
                '}';
    }

    public static class Heartbeat {

        private String activate;

        private String period;

        private Retry retry;

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

        public Retry getRetry() {
            return retry;
        }

        public void setRetry(Retry retry) {
            this.retry = retry;
        }

        @Override
        public String toString() {
            return "Heartbeat{" +
                    "activate='" + activate + '\'' +
                    ", period='" + period + '\'' +
                    ", retry=" + retry +
                    '}';
        }

        public static class Retry {

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

            @Override
            public String toString() {
                return "Retries{" +
                        "max='" + max + '\'' +
                        ", timeout='" + timeout + '\'' +
                        '}';
            }
        }
    }
}
