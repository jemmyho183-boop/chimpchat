package com.chimpchat.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import com.chimpchat.api.auth.business.GoogleIdentityBusiness;
import com.chimpchat.api.auth.entity.UserEntity;
import com.chimpchat.api.auth.repository.UserRepository;
import com.chimpchat.api.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = "app.auth.google.enabled=false")
class GoogleIdentityTests {
    @Autowired
    private GoogleIdentityBusiness identityBusiness;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void existingGoogleIdentityCanBeResolvedAfterItsTransactionEnds() {
        String subject = UUID.randomUUID().toString();
        String email = "google-" + subject + "@example.com";
        OidcUser googleUser = mock(OidcUser.class);
        when(googleUser.getSubject()).thenReturn(subject);
        when(googleUser.getEmail()).thenReturn(email);
        when(googleUser.getEmailVerified()).thenReturn(true);

        UserEntity createdUser = identityBusiness.resolve(googleUser);
        UserEntity existingUser = identityBusiness.resolve(googleUser);

        assertEquals(createdUser.getId(), existingUser.getId());
        assertEquals(email, existingUser.getEmail());
        assertNotNull(workspaceRepository.findByOwnerId(existingUser.getId()).orElse(null));
    }

    @Test
    void verifiedGoogleEmailReusesExistingEmailAccount() {
        String email = "shared-" + UUID.randomUUID() + "@example.com";
        UserEntity emailUser = userRepository.save(new UserEntity(email));

        OidcUser googleUser = mock(OidcUser.class);
        when(googleUser.getSubject()).thenReturn(UUID.randomUUID().toString());
        when(googleUser.getEmail()).thenReturn(email);
        when(googleUser.getEmailVerified()).thenReturn(true);

        UserEntity resolved = identityBusiness.resolve(googleUser);

        assertEquals(emailUser.getId(), resolved.getId());
    }
}
