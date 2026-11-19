package com.chimpchat.api.automation.business;

import java.util.Map;

public record AutomationEvent(String type, Map<String, String> attributes) {
    public AutomationEvent {
        attributes = Map.copyOf(attributes);
    }
}
