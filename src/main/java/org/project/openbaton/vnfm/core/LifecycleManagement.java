package org.project.openbaton.vnfm.core;

import javassist.NotFoundException;
import org.project.openbaton.catalogue.mano.common.Event;
import org.project.openbaton.catalogue.mano.common.LifecycleEvent;
import org.project.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by mpa on 22.07.15.
 */
@SpringBootApplication
public class LifecycleManagement {

    public Set<Event> listEvents(VirtualNetworkFunctionRecord vnfr) {
        Set<Event> events = new HashSet<Event>();
        for (LifecycleEvent event : vnfr.getLifecycle_event()) {
            events.add(event.getEvent());
        }
        return events;
    }

    public void removeEvent(VirtualNetworkFunctionRecord vnfr, Event event) throws NotFoundException {
        LifecycleEvent lifecycleEvent = null;
        if(vnfr.getLifecycle_event_history() == null)
            vnfr.setLifecycle_event_history(new HashSet<LifecycleEvent>());
        for (LifecycleEvent tmpLifecycleEvent : vnfr.getLifecycle_event()) {
            if (event.equals(tmpLifecycleEvent.getEvent())) {
                lifecycleEvent = tmpLifecycleEvent;
                vnfr.getLifecycle_event_history().add(lifecycleEvent);
                vnfr.getLifecycle_event().remove(lifecycleEvent);
                break;
            }
        }
        if (lifecycleEvent == null) {
            throw new NotFoundException("Not found LifecycleEvent with event " + event);
        }
    }

    public Set<Event> listHistoryEvents(VirtualNetworkFunctionRecord vnfr) {
        Set<Event> events = new HashSet<Event>();
        if (vnfr.getLifecycle_event_history() != null) {
            for (LifecycleEvent event : vnfr.getLifecycle_event_history()) {
                events.add(event.getEvent());
            }
        }
        return events;
    }
}
