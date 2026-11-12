package com.chimpchat.api.channel.controller;

import java.io.IOException;

import com.chimpchat.api.channel.business.InstagramWebhookBusiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/channels/instagram/webhook")
public class InstagramWebhookController {
    private static final Logger logger = LoggerFactory.getLogger(InstagramWebhookController.class);
    private static final String EVENT_RECEIVED = "EVENT_RECEIVED";

    private final InstagramWebhookBusiness webhookBusiness;

    public InstagramWebhookController(InstagramWebhookBusiness webhookBusiness) {
        this.webhookBusiness = webhookBusiness;
    }

    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if (!webhookBusiness.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Instagram webhook is not configured.");
        }
        if (challenge == null || !webhookBusiness.verifiesSubscription(mode, verifyToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Webhook verification failed.");
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(challenge);
    }

    @PostMapping
    public ResponseEntity<String> receive(
            @RequestBody byte[] payload,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature) {
        if (!webhookBusiness.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Instagram webhook is not configured.");
        }
        if (!webhookBusiness.verifiesSignature(payload, signature)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Webhook signature is invalid.");
        }
        try {
            webhookBusiness.processCommentNotifications(payload);
        } catch (IOException | IllegalArgumentException exception) {
            logger.warn("Rejected an invalid Instagram webhook payload: {}", exception.getMessage());
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Invalid Instagram webhook payload.");
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(EVENT_RECEIVED);
    }
}
