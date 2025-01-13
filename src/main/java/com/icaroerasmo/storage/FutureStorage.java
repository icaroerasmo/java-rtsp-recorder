package com.icaroerasmo.storage;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Component
public class FutureStorage {

    private final Map<String, Map<String, Future<Void>>> logsThreads = new ConcurrentHashMap<>();

    public void put(String camName, String threadName, Future<Void> future) {
        logsThreads.computeIfAbsent(camName, k -> new ConcurrentHashMap<>()).put(threadName, future);
    }

    public Future<Void> get(String camName, String threadName) {
        return logsThreads.get(camName).get(threadName);
    }
}
