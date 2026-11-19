package com.chimpchat.api.automation.business;

public interface AutomationTriggerHandler {
    String triggerType();

    void validate(AutomationDefinition.TriggerDefinition trigger);

    boolean matches(AutomationDefinition.TriggerDefinition trigger, AutomationEvent event);
}
