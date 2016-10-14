/*
 *
 *  * (C) Copyright 2016 NUBOMEDIA (http://www.nubomedia.eu)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
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
@ConfigurationProperties(prefix = "application")
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
    return "ApplicationProperties{" + "heartbeat=" + heartbeat + '}';
  }

  public static class Heartbeat {

    private boolean activate;

    private int period;

    private Retry retry;

    public boolean isActivate() {
      return activate;
    }

    public void setActivate(boolean activate) {
      this.activate = activate;
    }

    public int getPeriod() {
      return period;
    }

    public void setPeriod(int period) {
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
      return "Heartbeat{"
          + "activate='"
          + activate
          + '\''
          + ", period='"
          + period
          + '\''
          + ", retry="
          + retry
          + '}';
    }

    public static class Retry {

      private int max;

      private int timeout;

      public int getMax() {
        return max;
      }

      public void setMax(int max) {
        this.max = max;
      }

      public int getTimeout() {
        return timeout;
      }

      public void setTimeout(int timeout) {
        this.timeout = timeout;
      }

      @Override
      public String toString() {
        return "Retries{" + "max='" + max + '\'' + ", timeout='" + timeout + '\'' + '}';
      }
    }
  }
}
