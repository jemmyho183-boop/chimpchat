package com.chimpchat.api.automation.business;

import com.chimpchat.api.automation.entity.InstagramCommentAutomationEntity;
import com.chimpchat.api.channel.entity.InstagramCommentEntity;
import org.springframework.stereotype.Component;

@Component
public class InstagramPublicCommentReplyStepHandler implements AutomationStepHandler {
    private final InstagramCommentAutomationMetaBusiness metaBusiness;

    public InstagramPublicCommentReplyStepHandler(InstagramCommentAutomationMetaBusiness metaBusiness) {
        this.metaBusiness = metaBusiness;
    }

    @Override
    public String stepType() {
        return AutomationDefinition.INSTAGRAM_PUBLIC_COMMENT_REPLY;
    }

    @Override
    public void validate(AutomationDefinition.StepDefinition step) {
        if (!stepType().equals(step.type()) || isBlank(step.config().get("message"))) {
            throw new IllegalArgumentException("A public comment reply step requires a message.");
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
        return metaBusiness.replyToComment(
                comment.getExternalCommentId(), accessToken, message).id();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
