package com.techie345.moneys.identity;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "app.database.enabled", havingValue = "true", matchIfMissing = true)
public class OidcUserService implements org.springframework.security.oauth2.client.userinfo.OAuth2UserService<OidcUserRequest, OidcUser> {
    private final UserRepository users;
    private final org.springframework.security.oauth2.client.userinfo.OAuth2UserService<OidcUserRequest, OidcUser> delegate;

    @Autowired
    public OidcUserService(UserRepository users) {
        this(users, new org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService());
    }

    OidcUserService(UserRepository users, org.springframework.security.oauth2.client.userinfo.OAuth2UserService<OidcUserRequest, OidcUser> delegate) {
        this.users = users;
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest request) {
        return upsert(delegate.loadUser(request));
    }

    @Transactional
    public OidcUser upsert(OidcUser verified) {
        String subject = verified.getSubject();
        if (subject == null || subject.isBlank()) throw new IllegalArgumentException("verified OIDC subject is required");
        UserEntity user = users.findByGoogleSubject(subject)
                .map(existing -> { existing.updateProfile(displayName(verified), email(verified)); return existing; })
                .orElseGet(() -> UserEntity.create(subject, displayName(verified), email(verified)));
        UserEntity saved = users.save(user);
        OidcIdToken token = verified.getIdToken();
        Map<String, Object> claims = new LinkedHashMap<>(token.getClaims());
        claims.put("localUserId", saved.getId().toString());
        OidcIdToken localToken = OidcIdToken.withTokenValue(token.getTokenValue())
                .issuedAt(token.getIssuedAt()).expiresAt(token.getExpiresAt()).claims(values -> values.putAll(claims)).build();
        return new DefaultOidcUser(verified.getAuthorities(), localToken, verified.getUserInfo(), "localUserId");
    }

    private static String displayName(OidcUser user) {
        return user.getFullName() == null || user.getFullName().isBlank() ? user.getSubject() : user.getFullName();
    }

    private static String email(OidcUser user) {
        return user.getEmail() == null ? "unknown@invalid.local" : user.getEmail();
    }
}
