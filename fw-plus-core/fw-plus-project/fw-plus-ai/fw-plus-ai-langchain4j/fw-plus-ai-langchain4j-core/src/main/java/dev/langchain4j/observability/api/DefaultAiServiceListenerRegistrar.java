package dev.langchain4j.observability.api;

import dev.langchain4j.observability.api.event.AiServiceEvent;
import dev.langchain4j.observability.api.listener.AiServiceListener;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Default in-process listener registrar. */
public final class DefaultAiServiceListenerRegistrar implements AiServiceListenerRegistrar, dev.langchain4j.spi.observability.AiServiceListenerRegistrarFactory {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultAiServiceListenerRegistrar.class);
    private final List<AiServiceListener<?>> listeners = new ArrayList<AiServiceListener<?>>();
    private volatile boolean throwOnError;

    @Override
    public AiServiceListenerRegistrar get() { return new DefaultAiServiceListenerRegistrar(); }

    @Override
    public synchronized <T extends AiServiceEvent> void register(AiServiceListener<T> listener) {
        if (listener == null) throw new NullPointerException("listener");
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    @Override
    public synchronized <T extends AiServiceEvent> void unregister(AiServiceListener<T> listener) {
        if (listener == null) throw new NullPointerException("listener");
        listeners.remove(listener);
    }

    @Override
    public void shouldThrowExceptionOnEventError(boolean value) {
        throwOnError = value;
    }

    @Override
    public <T extends AiServiceEvent> void fireEvent(T event) {
        if (event == null) throw new NullPointerException("event");
        List<AiServiceListener<?>> snapshot;
        synchronized (this) { snapshot = new ArrayList<AiServiceListener<?>>(listeners); }
        for (AiServiceListener<?> listener : snapshot) {
            if (!listener.getEventClass().equals(event.eventClass())) continue;
            try { fire(listener, event); }
            catch (RuntimeException exception) {
                LOG.warn("An exception occurred during AI service listener invocation and was ignored.", exception);
                if (throwOnError) throw exception;
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void fire(AiServiceListener listener, AiServiceEvent event) {
        listener.onEvent(event);
    }
}
