package dev.langchain4j.internal;

import dev.langchain4j.Internal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Internal
public final class DefaultExecutorProvider {

    private DefaultExecutorProvider() {
    }

    public static ExecutorService getDefaultExecutorService() {
        return Holder.EXECUTOR_SERVICE;
    }

    private static final class Holder {
        private static final ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(
                0, Integer.MAX_VALUE, 1L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                new DaemonThreadFactory());
    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "langchain4j-default-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
