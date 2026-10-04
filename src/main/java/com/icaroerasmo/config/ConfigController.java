package com.icaroerasmo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @GetMapping
    public Map<String, Object> getConfig() {
        return configService.maskSecrets(configService.readConfig());
    }

    @PutMapping
    public ResponseEntity<Void> updateConfig(@RequestBody Map<String, Object> config) {
        Map<String, Object> current = configService.readConfig();
        Map<String, Object> merged = configService.restoreSecrets(config, current);
        configService.writeConfig(merged);

        Thread shutdownThread = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            Runtime.getRuntime().halt(0);
        });
        shutdownThread.setDaemon(true);
        shutdownThread.start();

        return ResponseEntity.ok().build();
    }
}
