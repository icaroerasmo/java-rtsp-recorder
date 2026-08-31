package com.icaroerasmo.storage;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Component
public class FutureStorage {

    private final Map<String, Map<String, Future<?>>> threads = new ConcurrentHashMap<>();
    private final Map<String, Process> processes = new ConcurrentHashMap<>();
    private final Map<String, Long> startTimes = new ConcurrentHashMap<>();

    public void put(String futureName, String threadName, Future<?> future) {
        threads.computeIfAbsent(futureName, k -> new ConcurrentHashMap<>()).put(threadName, future);
    }

    public void putStartTime(String name, long startTime) {
        startTimes.put(name, startTime);
    }

    public long getStartTime(String name) {
        return startTimes.getOrDefault(name, 0L);
    }

    public void putProcess(String name, Process process) {
        processes.put(name, process);
    }

    public Process getProcess(String name) {
        return processes.get(name);
    }

    public Future<?> get(String name, String threadName) {
        Map<String, Future<?>> map = get(name);
        return map != null ? map.get(threadName) : null;
    }

    public Map<String, Future<?>> get(String name) {
        return threads.get(name);
    }

    public void delete(String name) {
        delete(name, null);
    }

    public void delete(String name, String threadName) {

        var threadMap = get(name);

        if(threadMap == null) {
            return;
        }

        if(threadName != null) {
            Future<?> future = threadMap.remove(threadName);
            if (future != null) {
                future.cancel(true);
            }
            return;
        }

        // Cancel all futures before removing so that any running task
        // (e.g. the ffmpeg runner retry loop) is interrupted and stops
        // spawning new processes.
        threadMap.values().forEach(future -> future.cancel(true));
        threadMap.clear();
        threads.remove(name);
        processes.remove(name);
        startTimes.remove(name);
    }

    public boolean isRunning(String name) {
        return isRunning(name, "main");
    }

    public boolean isRunning(String name, String threadName) {
        Future<?> future = get(name, threadName);
        return future != null && future.state().equals(Future.State.RUNNING);
    }
}
