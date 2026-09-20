package com.icaroerasmo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ConfigService {

    private static final Pattern URL_CREDENTIALS_PATTERN =
            Pattern.compile("^(\\w+://[^:/]+:)([^@]+)(@.*)$");

    @Value("${app.config-file-path:/app/config/config.yaml}")
    private String configFilePath;

    public Map<String, Object> readConfig() {
        Yaml yaml = new Yaml();
        try (FileReader reader = new FileReader(configFilePath)) {
            Map<String, Object> result = yaml.load(reader);
            return result != null ? result : new LinkedHashMap<>();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read config file: " + configFilePath, e);
        }
    }

    public void writeConfig(Map<String, Object> config) {
        Yaml yaml = new Yaml();
        try (FileWriter writer = new FileWriter(configFilePath)) {
            yaml.dump(config, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write config file: " + configFilePath, e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> maskSecrets(Map<String, Object> config) {
        Map<String, Object> masked = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            masked.put(key, maskValue(key, value));
        }
        return masked;
    }

    @SuppressWarnings("unchecked")
    private Object maskValue(String key, Object value) {
        if (value instanceof Map) {
            return maskSecrets((Map<String, Object>) value);
        }
        if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            List<Object> maskedList = new java.util.ArrayList<>(list.size());
            for (Object element : list) {
                if (element instanceof Map) {
                    maskedList.add(maskSecrets((Map<String, Object>) element));
                } else {
                    maskedList.add(element);
                }
            }
            return maskedList;
        }
        if (isSecretKey(key)) {
            return "********";
        }
        if (value instanceof String) {
            String strValue = (String) value;
            Matcher matcher = URL_CREDENTIALS_PATTERN.matcher(strValue);
            if (matcher.matches()) {
                return matcher.group(1) + "********" + matcher.group(3);
            }
        }
        return value;
    }

    private boolean isSecretKey(String key) {
        String lower = key.toLowerCase();
        return lower.contains("password") || lower.contains("token")
                || lower.contains("secret") || lower.contains("credential")
                || lower.endsWith("key");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> restoreSecrets(Map<String, Object> incoming, Map<String, Object> current) {
        Map<String, Object> restored = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : incoming.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            Object currentVal = current.get(key);
            restored.put(key, restoreValue(key, value, currentVal));
        }
        return restored;
    }

    @SuppressWarnings("unchecked")
    private Object restoreValue(String key, Object value, Object currentVal) {
        if (value instanceof Map && currentVal instanceof Map) {
            return restoreSecrets((Map<String, Object>) value, (Map<String, Object>) currentVal);
        }
        if (value instanceof List && currentVal instanceof List) {
            List<Object> incomingList = (List<Object>) value;
            List<Object> currentList = (List<Object>) currentVal;
            List<Object> restoredList = new java.util.ArrayList<>(incomingList.size());
            for (int i = 0; i < incomingList.size(); i++) {
                Object incomingElement = incomingList.get(i);
                if (i < currentList.size()) {
                    Object currentElement = currentList.get(i);
                    if (incomingElement instanceof Map && currentElement instanceof Map) {
                        restoredList.add(restoreSecrets(
                                (Map<String, Object>) incomingElement,
                                (Map<String, Object>) currentElement));
                    } else {
                        restoredList.add(incomingElement);
                    }
                } else {
                    restoredList.add(incomingElement);
                }
            }
            return restoredList;
        }
        if ("********".equals(value)) {
            return currentVal;
        }
        if (value instanceof String && currentVal instanceof String) {
            if (((String) value).contains(":********@")) {
                return currentVal;
            }
        }
        return value;
    }
}
