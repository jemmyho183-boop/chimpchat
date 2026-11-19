package com.chimpchat.api.automation.business;

import com.chimpchat.api.automation.entity.InstagramCommentAutomationEntity;
import com.chimpchat.api.channel.entity.InstagramCommentEntity;

public interface AutomationStepHandler {
    String stepType();

    void validate(AutomationDefinition.StepDefinition step);

    String execute(
            AutomationDefinition.StepDefinition step,
            InstagramCommentAutomationEntity automation,
            InstagramCommentEntity comment,
            String accessToken);
}
