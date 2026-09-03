package dev.langchain4j.observability.api;

import java.util.Arrays;
import java.util.ServiceLoader;
import dev.langchain4j.observability.api.event.AiServiceEvent;
import dev.langchain4j.spi.observability.AiServiceListenerRegistrarFactory;
import dev.langchain4j.observability.api.listener.AiServiceListener;

/** Registrar for AI service event listeners. */
public interface AiServiceListenerRegistrar {
    <T extends AiServiceEvent> void register(AiServiceListener<T> listener);

    default void register(AiServiceListener<?>... listeners) {
        if (listeners != null) register(Arrays.asList(listeners));
    }

    default void register(Iterable<? extends AiServiceListener<?>> listeners) {
        if (listeners != null) {
            for (AiServiceListener<?> listener : listeners) registerUnchecked(listener);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    default void registerUnchecked(AiServiceListener<?> listener) {
        register((AiServiceListener) listener);
    }

    <T extends AiServiceEvent> void unregister(AiServiceListener<T> listener);

    default void unregister(AiServiceListener<?>... listeners) {
        if (listeners != null) unregister(Arrays.asList(listeners));
    }

    default void unregister(Iterable<? extends AiServiceListener<?>> listeners) {
        if (listeners != null) {
            for (AiServiceListener<?> listener : listeners) unregisterUnchecked(listener);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    default void unregisterUnchecked(AiServiceListener<?> listener) {
        unregister((AiServiceListener) listener);
    }

    <T extends AiServiceEvent> void fireEvent(T event);

    void shouldThrowExceptionOnEventError(boolean shouldThrowExceptionOnEventError);

    static AiServiceListenerRegistrar newInstance() {
        return newInstance(false);
    }

    static AiServiceListenerRegistrar newInstance(boolean shouldThrowExceptionOnEventError) {
        AiServiceListenerRegistrar registrar = null;
        for (AiServiceListenerRegistrarFactory factory : ServiceLoader.load(AiServiceListenerRegistrarFactory.class)) {
            registrar = factory.get();
            break;
        }
        if (registrar == null) registrar = new DefaultAiServiceListenerRegistrar();
        registrar.shouldThrowExceptionOnEventError(shouldThrowExceptionOnEventError);
        return registrar;
    }
}
