package org.openbaton.vnfm.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

/**
 * Created by mpa on 25.01.16.
 */
@Service
@ConfigurationProperties(prefix="autoscaling")
@PropertySource("classpath:autoscaling.properties")
public class AutoScalingProperties {

    private Pool pool;

    private TerminationRule terminationRule;

    public Pool getPool() {
        return pool;
    }

    public void setPool(Pool pool) {
        this.pool = pool;
    }

    public TerminationRule getTerminationRule() {
        return terminationRule;
    }

    public void setTerminationRule(TerminationRule terminationRule) {
        this.terminationRule = terminationRule;
    }

    public static class Pool {

        private boolean activate;

        private int size;

        private int period;

        private boolean prepare;

        public boolean isActivate() {
            return activate;
        }

        public void setActivate(boolean activate) {
            this.activate = activate;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getPeriod() {
            return period;
        }

        public void setPeriod(int period) {
            this.period = period;
        }

        public boolean isPrepare() {
            return prepare;
        }

        public void setPrepare(boolean prepare) {
            this.prepare = prepare;
        }
    }

    public static class TerminationRule {
        private boolean activate;

        private String metric;

        private String value;

        public boolean isActivate() {
            return activate;
        }

        public void setActivate(boolean activate) {
            this.activate = activate;
        }

        public String getMetric() {
            return metric;
        }

        public void setMetric(String metric) {
            this.metric = metric;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

}
