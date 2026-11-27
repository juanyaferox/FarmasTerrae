package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Replaces the authenticated {@link User} of the current session after the account changes, so the session keeps
 * working with the new username/email instead of a stale copy (which would, e.g., drop the remember-me tokens of
 * the old username's new owner on logout).
 */
@Service
public class PrincipalRefreshService {

    /** Same storage as Spring Security's default (session attribute plus request attribute). */
    private final SecurityContextRepository securityContextRepository = new DelegatingSecurityContextRepository(
            new RequestAttributeSecurityContextRepository(), new HttpSessionSecurityContextRepository());

    private final String rememberMeKey;

    public PrincipalRefreshService(@Value ("${app.security}") String rememberMeKey) {
        this.rememberMeKey = rememberMeKey;
    }

    /**
     * Swaps the current principal for {@code updated}, recomputing its authorities from the stored profile.
     * Does nothing unless the request is authenticated as that same account. A remember-me login stays a
     * remember-me login, so the refresh never upgrades how the user authenticated.
     */
    public void refresh(User updated, HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolderStrategy holder = SecurityContextHolder.getContextHolderStrategy();
        Authentication current = holder.getContext().getAuthentication();
        if (current == null || !(current.getPrincipal() instanceof User principal)
                || !Objects.equals(principal.getId(), updated.getId())) {
            return;
        }
        AbstractAuthenticationToken refreshed = current instanceof RememberMeAuthenticationToken
                ? new RememberMeAuthenticationToken(rememberMeKey, updated, updated.getAuthorities())
                : UsernamePasswordAuthenticationToken.authenticated(updated, null, updated.getAuthorities());
        refreshed.setDetails(current.getDetails());

        SecurityContext context = holder.createEmptyContext();
        context.setAuthentication(refreshed);
        holder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
