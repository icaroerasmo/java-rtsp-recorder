package com.icaroerasmo.storage;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Component
public class FutureStorage {

    private final Map<String, Map<String, Future<Void>>> logsThreads = new ConcurrentHashMap<>();

    public void put(String futureName, String threadName, Future<Void> future) {
        logsThreads.computeIfAbsent(futureName, k -> new ConcurrentHashMap<>()).put(threadName, future);
    }

    public Future<Void> get(String camName, String threadName) {
        return get(camName).get(threadName);
    }

    public Map<String, Future<Void>> get(String camName) {
        return logsThreads.get(camName);
    }
}
