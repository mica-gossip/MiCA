package org.princehouse.mica.base.simple;

import org.princehouse.mica.base.model.MicaRuntime;

public class ThreadLocalRuntimeMechanism {

    public static MicaRuntime getRuntime() {
        MicaRuntime rt = runtimeSingleton.get();
        if (rt == null)
            throw new RuntimeException(String.format("Failed attempt to get null runtime for thread %d", Thread
                    .currentThread().getId()));
        return (MicaRuntime) rt;
    }

    // public static void clearRuntime(Runtime rt) {
    // Runtime current = runtimeSingleton.get();
    // if (current != null && !current.equals(rt)) {
    // throw new RuntimeException("attempt to replace active runtime");
    // }
    // setRuntime(null);
    // }

    public static void setRuntime(MicaRuntime rt) {
        // System.err.printf("[set %s for thread %d]\n", rt,
        // Thread.currentThread().getId());
        if (runtimeSingleton.get() != null && rt != null && !runtimeSingleton.equals(rt)) {
            throw new RuntimeException(String.format(
                    "Attempt to set two runtimes in one thread; existing runtime is %s, new runtime is %s",
                    runtimeSingleton.get(), rt));
        }
        runtimeSingleton.set(rt);
    }

    private static ThreadLocal<MicaRuntime> runtimeSingleton = new ThreadLocal<MicaRuntime>();

}
