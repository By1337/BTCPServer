package org.by1337.btcp.common.event;

import org.by1337.btcp.common.annotations.EventHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventManagerTest {

    private final EventManager eventManager = new EventManager();

    @Test
    void callEvent() {
        ExampleListener listener = new ExampleListener();
        eventManager.register(listener);
        eventManager.callEvent(new ExampleEvent());
        assertTrue(listener.called);
    }

    static class ExampleEvent implements Event {

    }

    static class ExampleListener extends AbstractListener {
        public boolean called;
        @EventHandler
        private void on(ExampleEvent event){
            called = true;
        }
    }
}