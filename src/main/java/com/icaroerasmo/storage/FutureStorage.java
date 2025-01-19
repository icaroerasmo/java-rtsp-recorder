package com.icaroerasmo.storage;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Component
public class FutureStorage {

    private final Map<String, Map<String, Future<?>>> logsThreads = new ConcurrentHashMap<>();

    public void put(String futureName, String threadName, Future<?> future) {
        logsThreads.computeIfAbsent(futureName, k -> new ConcurrentHashMap<>()).put(threadName, future);
    }

    public Future<?> get(String camName, String threadName) {
        return get(camName).get(threadName);
    }

    public Map<String, Future<?>> get(String camName) {
        return logsThreads.get(camName);
    }
}
