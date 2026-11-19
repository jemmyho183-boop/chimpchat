package com.chimpchat.api.automation.business;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class AutomationStepHandlerRegistry {
    private final Map<String, AutomationStepHandler> handlers;

    public AutomationStepHandlerRegistry(List<AutomationStepHandler> handlers) {
        this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(
                AutomationStepHandler::stepType, Function.identity()));
    }

    public AutomationStepHandler require(String stepType) {
        AutomationStepHandler handler = handlers.get(stepType);
        if (handler == null) {
            throw new IllegalStateException("No automation step handler is registered for " + stepType + ".");
        }
        return handler;
    }

    public void validate(AutomationDefinition.StepDefinition step) {
        require(step.type()).validate(step);
    }
}
