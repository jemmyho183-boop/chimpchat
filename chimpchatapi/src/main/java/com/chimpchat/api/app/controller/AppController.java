package com.chimpchat.api.app.controller;

import com.chimpchat.api.app.business.AppBootstrapBusiness;
import com.chimpchat.api.app.dto.AppBootstrapResponseDto;
import com.chimpchat.api.auth.business.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/app")
@RequiredArgsConstructor
public class AppController {
    private final AppBootstrapBusiness bootstrapBusiness;

    @GetMapping("/bootstrap")
    public AppBootstrapResponseDto bootstrap(@AuthenticationPrincipal AppUserPrincipal principal) {
        return bootstrapBusiness.get(principal.getId());
    }
}
