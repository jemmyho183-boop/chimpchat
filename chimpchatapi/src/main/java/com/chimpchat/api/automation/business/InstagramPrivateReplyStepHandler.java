package com.chimpchat.api.automation.business;

import com.chimpchat.api.automation.entity.InstagramCommentAutomationEntity;
import com.chimpchat.api.channel.entity.InstagramCommentEntity;
import org.springframework.stereotype.Component;

@Component
public class InstagramPrivateReplyStepHandler implements AutomationStepHandler {
    private final InstagramCommentAutomationMetaBusiness metaBusiness;

    public InstagramPrivateReplyStepHandler(InstagramCommentAutomationMetaBusiness metaBusiness) {
        this.metaBusiness = metaBusiness;
    }

    @Override
    public String stepType() {
        return AutomationDefinition.INSTAGRAM_PRIVATE_REPLY;
    }

    @Override
    public void validate(AutomationDefinition.StepDefinition step) {
        String buttonText = step.config().get("buttonText");
        String buttonUrl = step.config().get("buttonUrl");
        if (!stepType().equals(step.type())
                || isBlank(step.config().get("message"))
                || (isBlank(buttonText) != isBlank(buttonUrl))) {
            throw new IllegalArgumentException(
                    "A private reply step requires a message and both link button fields or neither.");
        }
    }

    @Override
    public String execute(
            AutomationDefinition.StepDefinition step,
            InstagramCommentAutomationEntity automation,
            InstagramCommentEntity comment,
            String accessToken) {
        validate(step);
        String message = step.config().get("message");
        String buttonText = step.config().get("buttonText");
        String buttonUrl = step.config().get("buttonUrl");
        boolean hasButton = buttonText != null && buttonUrl != null;
        return metaBusiness.sendPrivateReply(
                automation.getInstagramAccountId(),
                comment.getExternalCommentId(),
                accessToken,
                message,
                hasButton ? buttonText : null,
                hasButton ? buttonUrl : null).id();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
