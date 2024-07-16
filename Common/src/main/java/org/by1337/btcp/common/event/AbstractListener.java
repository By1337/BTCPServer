package org.by1337.btcp.common.event;

import com.google.common.base.Joiner;
import org.by1337.btcp.common.annotations.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractListener implements Listener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractListener.class);
    private final Map<Class<? extends Event>, List<Method>> handlers;

    public AbstractListener() {
        Map<Class<? extends Event>, List<Method>> handlers0 = new HashMap<>();
        for (Method method : this.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventHandler.class)) {
                Class<?>[] parameters = method.getParameterTypes();
                if (parameters.length != 1) {
                    LOGGER.error(
                            "The {} method is annotated with the {} annotation, but takes more than just event! Parameters ({}), expected (? extends Event)",
                            method.getName(),
                            EventHandler.class,
                            Joiner.on(", ").join(parameters)
                    );
                    continue;
                }
                Class<?> clazz = parameters[0];
                if (clazz.isAssignableFrom(Event.class)) {
                    LOGGER.error(
                            "The {} method is annotated with the {} annotation but accepts non-event! parameters ({})",
                            method.getName(),
                            EventHandler.class,
                            Joiner.on(", ").join(parameters)
                    );
                    continue;
                }
                Class<? extends Event> eventType = (Class<? extends Event>) clazz;
                handlers0.computeIfAbsent(eventType, k -> new ArrayList<>()).add(method);
            }
        }
        handlers = Collections.unmodifiableMap(handlers0);
    }

    @Override
    public void on(Event event) {
        var list = handlers.get(event.getClass());
        if (list != null) {
            for (Method method : list) {
                try {
                    method.invoke(this, event);
                } catch (Throwable t) {
                    LOGGER.error("Failed to complete the event!", t);
                }
            }
        }
    }
}
