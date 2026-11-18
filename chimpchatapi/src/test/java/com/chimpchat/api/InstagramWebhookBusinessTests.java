package com.chimpchat.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.Instant;

import com.chimpchat.api.automation.business.InstagramDirectMessageReceivedEvent;
import com.chimpchat.api.channel.business.InstagramCommentBusiness;
import com.chimpchat.api.channel.business.InstagramWebhookBusiness;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;

class InstagramWebhookBusinessTests {
    @Test
    void publishesIncomingInstagramMessageEventsButIgnoresBusinessEchoes() throws IOException {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        InstagramWebhookBusiness webhookBusiness = new InstagramWebhookBusiness(
                new ObjectMapper(),
                mock(InstagramCommentBusiness.class),
                eventPublisher,
                "app-secret",
                "verify-token");
        byte[] payload = """
                {
                  "object":"instagram",
                  "entry":[{
                    "id":"instagram-account-id",
                    "messaging":[
                      {
                        "sender":{"id":"commenter-id"},
                        "timestamp":%d,
                        "message":{
                          "mid":"incoming-message-id",
                          "text":"Here is my reply",
                          "reply_to":{"mid":"private-reply-id"}
                        }
                      },
                      {
                        "sender":{"id":"instagram-account-id"},
                        "message":{"mid":"echo-message-id","is_echo":true}
                      }
                    ]
                  }]
                }
                """.formatted(Instant.now().toEpochMilli())
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);

        webhookBusiness.processCommentNotifications(payload);

        ArgumentCaptor<InstagramDirectMessageReceivedEvent> eventCaptor =
                ArgumentCaptor.forClass(InstagramDirectMessageReceivedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        var event = eventCaptor.getValue();
        assertEquals("instagram-account-id", event.instagramAccountId());
        assertEquals("commenter-id", event.commenterId());
        assertEquals("private-reply-id", event.replyToMessageId());
        assertEquals(Instant.now().toEpochMilli(), event.receivedAt().toEpochMilli(), 5000);
    }
}
