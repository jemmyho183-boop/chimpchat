package com.chimpchat.api.automation.controller;

import java.util.List;
import java.util.UUID;

import com.chimpchat.api.auth.business.AppUserPrincipal;
import com.chimpchat.api.automation.business.InstagramCommentAutomationBusiness;
import com.chimpchat.api.automation.dto.InstagramCommentAutomationRequestDto;
import com.chimpchat.api.automation.dto.InstagramCommentAutomationResponseDto;
import com.chimpchat.api.automation.dto.InstagramMediaResponseDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/channels/instagram/automations")
public class InstagramCommentAutomationController {
    private final InstagramCommentAutomationBusiness automationBusiness;

    public InstagramCommentAutomationController(InstagramCommentAutomationBusiness automationBusiness) {
        this.automationBusiness = automationBusiness;
    }

    @GetMapping("/posts")
    public List<InstagramMediaResponseDto> listPosts(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam String accountId) {
        return automationBusiness.listPosts(principal.getId(), accountId);
    }

    @GetMapping
    public List<InstagramCommentAutomationResponseDto> list(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) String accountId) {
        return automationBusiness.list(principal.getId(), accountId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstagramCommentAutomationResponseDto create(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody InstagramCommentAutomationRequestDto request) {
        return automationBusiness.create(principal.getId(), request);
    }

    @PutMapping("/{automationId}")
    public InstagramCommentAutomationResponseDto update(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID automationId,
            @Valid @RequestBody InstagramCommentAutomationRequestDto request) {
        return automationBusiness.update(principal.getId(), automationId, request);
    }

    @DeleteMapping("/{automationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID automationId) {
        automationBusiness.delete(principal.getId(), automationId);
    }
}
