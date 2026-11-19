package com.chimpchat.api.automation.business;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class AutomationTriggerHandlerRegistry {
    private final Map<String, AutomationTriggerHandler> handlers;

    public AutomationTriggerHandlerRegistry(List<AutomationTriggerHandler> handlers) {
        this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(
                AutomationTriggerHandler::triggerType, Function.identity()));
    }

    public AutomationTriggerHandler require(String triggerType) {
        AutomationTriggerHandler handler = handlers.get(triggerType);
        if (handler == null) {
            throw new IllegalStateException("No automation trigger handler is registered for " + triggerType + ".");
        }
        return handler;
    }

    public void validate(AutomationDefinition.TriggerDefinition trigger) {
        require(trigger.type()).validate(trigger);
    }
}
