package dev.langchain4j.invocation;

import dev.langchain4j.Internal;
import java.util.Map;

/** Parameters managed by the LangChain4j invocation chain. */
@Internal
public interface LangChain4jManaged {

    ThreadLocal<Map<Class<? extends LangChain4jManaged>, LangChain4jManaged>> CURRENT =
            new ThreadLocal<Map<Class<? extends LangChain4jManaged>, LangChain4jManaged>>();

    static void setCurrent(Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> current) {
        CURRENT.set(current);
    }

    static Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> current() {
        return CURRENT.get();
    }

    static <T extends LangChain4jManaged> T current(Class<T> clazz) {
        Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> current = CURRENT.get();
        return current == null ? null : clazz.cast(current.get(clazz));
    }

    /** Restores a previous managed context, useful for nested calls. */
    static Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> push(
            Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> next) {
        Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> previous = CURRENT.get();
        CURRENT.set(next);
        return previous;
    }

    static void restore(Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> previous) {
        if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
    }

    static void removeCurrent() {
        CURRENT.remove();
    }
}
