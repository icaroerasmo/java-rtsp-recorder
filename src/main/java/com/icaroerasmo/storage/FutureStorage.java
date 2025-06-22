package com.icaroerasmo.storage;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Component
public class FutureStorage {

    private final Map<String, Map<String, Future<?>>> threads = new ConcurrentHashMap<>();

    public void put(String futureName, String threadName, Future<?> future) {
        threads.computeIfAbsent(futureName, k -> new ConcurrentHashMap<>()).put(threadName, future);
    }

    public Future<?> get(String name, String threadName) {
        return get(name).get(threadName);
    }

    public Map<String, Future<?>> get(String name) {
        return threads.get(name);
    }

    public void delete(String name) {
        delete(name, null);
    }

    public void delete(String name, String threadName) {

        if(get(name) == null) {
            return;
        }

        if(threadName != null && get(name, threadName) != null) {
            get(name).remove(threadName);
            return;
        }

        get(name).clear();
        threads.remove(name);
    }

    public boolean isRunning(String name) {
        return isRunning(name, "main");
    }

    public boolean isRunning(String name, String threadName) {
        return get(name, threadName).state().equals(Future.State.RUNNING);
    }
}
