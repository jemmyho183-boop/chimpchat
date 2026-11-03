package com.chimpchat.api.auth.business;

import java.util.Locale;

import com.chimpchat.api.auth.entity.ExternalIdentityEntity;
import com.chimpchat.api.auth.entity.UserEntity;
import com.chimpchat.api.auth.repository.ExternalIdentityRepository;
import com.chimpchat.api.auth.repository.UserRepository;
import com.chimpchat.api.workspace.business.WorkspaceBusiness;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleIdentityBusiness {
    private final ExternalIdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final WorkspaceBusiness workspaceBusiness;

    public GoogleIdentityBusiness(
            ExternalIdentityRepository identityRepository,
            UserRepository userRepository,
            WorkspaceBusiness workspaceBusiness) {
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.workspaceBusiness = workspaceBusiness;
    }

    @Transactional
    public UserEntity resolve(OidcUser googleUser) {
        String subject = googleUser.getSubject();
        String email = googleUser.getEmail();
        if (subject == null || subject.isBlank() || email == null || email.isBlank()
                || !Boolean.TRUE.equals(googleUser.getEmailVerified())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "google_identity_unverified",
                    "Google did not provide a verified email address.");
        }

        return identityRepository.findByProviderAndSubject("GOOGLE", subject)
                .map(ExternalIdentityEntity::getUser)
                .orElseGet(() -> createGoogleIdentity(email.trim().toLowerCase(Locale.ROOT), subject));
    }

    private UserEntity createGoogleIdentity(String email, String subject) {
        UserEntity user = userRepository.findByEmail(email).orElseGet(() -> {
            UserEntity created = userRepository.save(new UserEntity(email));
            workspaceBusiness.getOrCreatePersonalWorkspace(created);
            return created;
        });
        if (identityRepository.existsByUserIdAndProvider(user.getId(), "GOOGLE")) {
            throw new AuthException(HttpStatus.CONFLICT, "identity_link_required",
                    "This account is already linked to another Google identity.");
        }
        identityRepository.save(new ExternalIdentityEntity(user, "GOOGLE", subject));
        return user;
    }
}
