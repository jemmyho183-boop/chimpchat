package com.chimpchat.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import com.chimpchat.api.automation.business.AutomationDefinition;
import com.chimpchat.api.automation.business.AutomationDefinitionMapper;
import com.chimpchat.api.automation.business.AutomationEvent;
import com.chimpchat.api.automation.business.InstagramCommentTriggerHandler;
import com.chimpchat.api.automation.dto.InstagramCommentAutomationRequestDto;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AutomationDefinitionMapperTests {
    private final AutomationDefinitionMapper mapper = new AutomationDefinitionMapper(new ObjectMapper());

    @Test
    void roundTripsACommentTriggerAndOrderedReplySteps() {
        AutomationDefinition original = mapper.fromRequest(new InstagramCommentAutomationRequestDto(
                "account-id",
                "media-id",
                "Summer collection",
                "KEYWORD",
                "send me the link",
                "Thanks for your comment!",
                "Here is the link.",
                "Shop now",
                "https://example.test/shop",
                true));

        AutomationDefinition restored = mapper.fromJson(mapper.toJson(original));

        assertEquals(AutomationDefinition.CURRENT_SCHEMA_VERSION, restored.schemaVersion());
        assertEquals(AutomationDefinition.INSTAGRAM_COMMENT_RECEIVED, restored.trigger().type());
        assertEquals("media-id", restored.trigger().config().get("mediaId"));
        assertEquals(2, restored.steps().size());
        assertEquals("public-reply", restored.steps().get(0).id());
        assertEquals(AutomationDefinition.INSTAGRAM_PUBLIC_COMMENT_REPLY, restored.steps().get(0).type());
        assertEquals("opening-dm", restored.steps().get(1).id());
        assertEquals("https://example.test/shop", restored.steps().get(1).config().get("buttonUrl"));
    }

    @Test
    void rejectsUnsupportedDefinitionVersionsAndMatchesCommentKeywordsWithoutCaseSensitivity() {
        AutomationDefinition definition = mapper.fromRequest(new InstagramCommentAutomationRequestDto(
                "account-id",
                "media-id",
                "Post",
                "KEYWORD",
                "send me",
                "Thanks!",
                "Here you go.",
                null,
                null,
                true));
        String unsupported = mapper.toJson(definition).replace(
                "\"schemaVersion\":1",
                "\"schemaVersion\":2");

        assertThrows(IllegalArgumentException.class, () -> mapper.fromJson(unsupported));
        assertTrue(new InstagramCommentTriggerHandler().matches(
                definition.trigger(),
                new AutomationEvent(
                        AutomationDefinition.INSTAGRAM_COMMENT_RECEIVED,
                        Map.of("mediaId", "media-id", "commentText", "Please SEND ME the link"))));
    }
}
