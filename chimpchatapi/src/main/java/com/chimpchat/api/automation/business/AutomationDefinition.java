package com.chimpchat.api.automation.business;

import java.util.List;
import java.util.Map;

public record AutomationDefinition(
        int schemaVersion,
        TriggerDefinition trigger,
        List<StepDefinition> steps) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final String INSTAGRAM_COMMENT_RECEIVED = "INSTAGRAM_COMMENT_RECEIVED";
    public static final String INSTAGRAM_PUBLIC_COMMENT_REPLY = "INSTAGRAM_PUBLIC_COMMENT_REPLY";
    public static final String INSTAGRAM_PRIVATE_REPLY = "INSTAGRAM_PRIVATE_REPLY";

    public AutomationDefinition {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }

    public record TriggerDefinition(
            String type,
            int schemaVersion,
            Map<String, String> config) {
        public TriggerDefinition {
            config = config == null ? Map.of() : Map.copyOf(config);
        }
    }

    public record StepDefinition(
            String id,
            String type,
            int schemaVersion,
            Map<String, String> config) {
        public StepDefinition {
            config = config == null ? Map.of() : Map.copyOf(config);
        }
    }
}
