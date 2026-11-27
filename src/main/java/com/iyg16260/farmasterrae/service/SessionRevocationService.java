package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.model.User;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
public class SessionRevocationService {
    private final SessionRegistry sessionRegistry;
    private final PersistentTokenRepository persistentTokenRepository;

    public SessionRevocationService(SessionRegistry sessionRegistry, PersistentTokenRepository persistentTokenRepository) {
        this.sessionRegistry = sessionRegistry;
        this.persistentTokenRepository = persistentTokenRepository;
    }

    public void revoke(User user) { revoke(user, user.getUsername(), null); }

    public void revoke(User user, String previousUsername) {
        revoke(user, previousUsername, null);
    }

    public void revokeOthers(User user, String currentSessionId) {
        revoke(user, user.getUsername(), currentSessionId);
    }

    /**
     * Like {@link #revokeOthers(User, String)} after a rename: also drops the remember-me tokens issued for
     * {@code previousUsername}, so whoever registers that name later cannot inherit them.
     */
    public void revokeOthers(User user, String previousUsername, String currentSessionId) {
        revoke(user, previousUsername, currentSessionId);
    }

    private void revoke(User user, String previousUsername, String currentSessionId) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (sameUser(principal, user, previousUsername)) {
                for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
                    if (currentSessionId == null || !currentSessionId.equals(session.getSessionId())) session.expireNow();
                }
            }
        }
        if (previousUsername != null) persistentTokenRepository.removeUserTokens(previousUsername);
        if (user.getUsername() != null && !Objects.equals(previousUsername, user.getUsername()))
            persistentTokenRepository.removeUserTokens(user.getUsername());
    }

    private boolean sameUser(Object principal, User user, String previousUsername) {
        if (principal instanceof User sessionUser) {
            // Ids never change, while a session can keep the username its account had at login time, and
            // that username may now belong to someone else: never match another account by name.
            if (sessionUser.getId() != null && user.getId() != null) {
                return sessionUser.getId().equals(user.getId());
            }
            return Objects.equals(sessionUser.getUsername(), previousUsername)
                    || Objects.equals(sessionUser.getUsername(), user.getUsername());
        }
        return principal instanceof String name && (Objects.equals(name, previousUsername) || Objects.equals(name, user.getUsername()));
    }
}
