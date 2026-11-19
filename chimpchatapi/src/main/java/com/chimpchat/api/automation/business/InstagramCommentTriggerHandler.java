package com.chimpchat.api.automation.business;

import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class InstagramCommentTriggerHandler implements AutomationTriggerHandler {
    @Override
    public String triggerType() {
        return AutomationDefinition.INSTAGRAM_COMMENT_RECEIVED;
    }

    @Override
    public void validate(AutomationDefinition.TriggerDefinition trigger) {
        String matchType = trigger.config().get("matchType");
        if (!triggerType().equals(trigger.type())
                || isBlank(trigger.config().get("mediaId"))
                || !("ANY_COMMENT".equals(matchType) || "KEYWORD".equals(matchType))
                || ("KEYWORD".equals(matchType) && isBlank(trigger.config().get("keyword")))) {
            throw new IllegalArgumentException("The Instagram comment trigger configuration is invalid.");
        }
    }

    @Override
    public boolean matches(AutomationDefinition.TriggerDefinition trigger, AutomationEvent event) {
        validate(trigger);
        if (!triggerType().equals(event.type())) {
            return false;
        }
        if (!equals(trigger.config().get("mediaId"), event.attributes().get("mediaId"))) {
            return false;
        }

        String matchType = trigger.config().get("matchType");
        if ("ANY_COMMENT".equals(matchType)) {
            return true;
        }
        if (!"KEYWORD".equals(matchType)) {
            throw new IllegalArgumentException("The Instagram comment trigger has an unsupported match type.");
        }

        String keyword = trigger.config().get("keyword");
        String commentText = event.attributes().get("commentText");
        return keyword != null
                && commentText != null
                && commentText.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private boolean equals(String left, String right) {
        return left != null && left.equals(right);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
