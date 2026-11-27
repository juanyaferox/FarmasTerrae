package com.iyg16260.farmasterrae.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

/** Persistent remember-me cookies with an explicit Strict SameSite policy. */
public class StrictPersistentRememberMeServices extends PersistentTokenBasedRememberMeServices {
    public StrictPersistentRememberMeServices(String key, UserDetailsService users, PersistentTokenRepository tokens) {
        super(key, users, tokens);
    }

    @Override
    protected void setCookie(String[] tokens, int maxAge, HttpServletRequest request, HttpServletResponse response) {
        String contextPath = request.getContextPath();
        String path = (contextPath != null && !contextPath.isEmpty()) ? contextPath : "/";
        ResponseCookie.ResponseCookieBuilder cookie = ResponseCookie.from(getCookieName(), encodeCookie(tokens))
                .path(path)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Strict");
        if (maxAge >= 0) {
            cookie.maxAge(maxAge);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.build().toString());
    }
}
