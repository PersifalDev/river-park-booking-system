package ru.haritonenko.userservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.admin")
public class AdminCapabilitiesProperties {

    private Map<String, List<String>> capabilitiesByRole = Map.of();

    public Map<String, List<String>> getCapabilitiesByRole() {
        return capabilitiesByRole;
    }

    public void setCapabilitiesByRole(Map<String, List<String>> capabilitiesByRole) {
        this.capabilitiesByRole = capabilitiesByRole;
    }
}
