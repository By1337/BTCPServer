package org.by1337.btcp.common.event;

import org.by1337.btcp.common.util.collection.LockableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventManager.class);
    private final LockableList<Listener> listeners = LockableList.createThreadSaveList();

    public void register(Listener listener) {
        listeners.add(listener);
    }

    public void unregister(Listener listener) {
        listeners.remove(listener);
    }

    public void callEvent(Event event) {
        listeners.lock();
        for (Listener listener : listeners) {
            try {
                listener.on(event);
            } catch (Throwable throwable) {
                LOGGER.error("An error occurred during the execution of an event in the listener", throwable);
            }
        }
        listeners.unlock();
    }

}
