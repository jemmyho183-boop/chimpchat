package com.chimpchat.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.frontend-url=http://localhost:3000",
        "app.auth.google.enabled=false",
        "app.auth.email.enabled=true",
        "spring.mail.host=mail.test",
        "management.health.mail.enabled=false"
})
class EmailAuthenticationTests {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.chimpchat.api.auth.repository.UserRepository userRepository;

    @Autowired
    private com.chimpchat.api.workspace.repository.WorkspaceRepository workspaceRepository;

    @Autowired
    private com.chimpchat.api.auth.repository.EmailLoginTokenRepository tokenRepository;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void oneTimeEmailLinkCreatesSessionAndPersonalWorkspace() throws Exception {
        String email = "email-flow-" + java.util.UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/auth/email-links")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk());

        var message = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        Matcher matcher = TOKEN_PATTERN.matcher(message.getValue().getText());
        assertTrue(matcher.find());
        String token = matcher.group(1);

        MvcResult consumed = mockMvc.perform(post("/api/v1/auth/email-links/consume")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) consumed.getRequest().getSession(false);
        assertNotNull(session);
        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk());

        assertNotNull(userRepository.findByEmail(email).orElse(null));
        assertTrue(workspaceRepository.findByOwnerId(
                userRepository.findByEmail(email).orElseThrow().getId()).isPresent());

        mockMvc.perform(post("/api/v1/auth/email-links/consume")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emailDeliveryFailureRemovesTheUnusableLink() throws Exception {
        String email = "email-failure-" + java.util.UUID.randomUUID() + "@example.com";
        doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        mockMvc.perform(post("/api/v1/auth/email-links")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isServiceUnavailable());

        org.junit.jupiter.api.Assertions.assertEquals(
                0, tokenRepository.countByEmailAndConsumedAtIsNull(email));
    }
}
