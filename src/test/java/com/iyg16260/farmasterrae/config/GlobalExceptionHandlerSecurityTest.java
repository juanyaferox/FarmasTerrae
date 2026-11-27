package com.iyg16260.farmasterrae.config;

import com.iyg16260.farmasterrae.controller.ErrorViewController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class GlobalExceptionHandlerSecurityTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController(), new ErrorViewController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://evil.example/phish",
            "http://localhost//evil.example",
            "http://localhost:80//evil.example",
            "//evil.example",
            "//evil_host/admin",
            "/\\evil.example",
            "http://evil.example?@localhost",
            "https://localhost.evil.example/safe",
            "http://localhost@evil.example/safe",
            "http://attacker@localhost/safe",
            "http://localhost:8081/safe",
            "https://localhost/safe",
            "http://localhost/safe\r\nX-Injected: 1",
            "ht!tp://bad uri"
    })
    void unsafeRefererAlwaysFallsBackToRoot(String referer) throws Exception {
        mockMvc.perform(get("/probe/error").header("Referer", referer))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("errorMessage", "Producto no encontrado"));
    }

    @Test
    void sameOriginRefererIsReducedToPathAndQuery() throws Exception {
        mockMvc.perform(get("/probe/error").header("Referer", "http://localhost/safe?tab=1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/safe?tab=1"));

        mockMvc.perform(get("/probe/error").header("Referer", "http://localhost"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void bindingTypeMismatchUsesPlainSafeMessage() throws Exception {
        var result = mockMvc.perform(post("/probe/bind")
                        .param("quantity", "<img src=x onerror=alert(1)>")
                        .header("Referer", "/safe"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "Errores de validación:"))
                .andReturn();
        Object errors = result.getFlashMap().get("errorMessages");
        assertThat(errors).isInstanceOf(List.class);
        assertThat(errors.toString()).contains("Valor inválido para el campo quantity")
                .doesNotContain("<img", "onerror", "alert");
    }

    @Test
    void bindingExceptionWithoutRefererMapsToBadRequest() throws Exception {
        mockMvc.perform(post("/probe/bind").param("quantity", "bad"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void auditUsesRemoteAddressAndOnlyHashedSessionIdentifier() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.12");
        request.addHeader("X-Forwarded-For", "203.0.113.5");
        request.addHeader("User-Agent", "TestAgent\r\nInjected: yes");
        request.setRequestURI("/auth/resetPassword\r\nInjected: true");
        request.setQueryString("token=super-secret-reset-token");
        request.setMethod("GET");
        request.setSession(new org.springframework.mock.web.MockHttpSession(null, "raw-session-id"));

        Method method = GlobalExceptionHandler.class.getDeclaredMethod("collectAuditInfo", HttpServletRequest.class);
        method.setAccessible(true);
        Object audit = method.invoke(handler, request);
        assertThat(read(audit, "remoteAddress")).isEqualTo("192.0.2.12");
        assertThat(read(audit, "sessionId")).isNotEqualTo("raw-session-id").hasSize(8);
        assertThat(read(audit, "requestUri")).doesNotContain("\r", "\n", "super-secret-reset-token");
        assertThat(read(audit, "userAgent")).doesNotContain("\r", "\n");

        var noSession = new org.springframework.mock.web.MockHttpServletRequest();
        noSession.setRequestURI("/");
        Object noSessionAudit = method.invoke(handler, noSession);
        assertThat(read(noSessionAudit, "sessionId")).isEqualTo("none");
        assertThat(noSession.getSession(false)).isNull();
    }

    @Test
    void requestAuditFilterDoesNotAttachErrorIdOnNormalRequests() throws Exception {
        RequestAuditFilter filter = new RequestAuditFilter();
        var request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/products");
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        FilterChain okChain = (req, res) -> {};

        filter.doFilter(request, response, okChain);
        assertThat(request.getAttribute("errorId")).isNull();
    }

    @Test
    void errorViewControllerAcceptsAllHttpMethodsAndPreservesStatus() throws Exception {
        for (var builder : List.of(get("/common/error"), post("/common/error"), put("/common/error"), delete("/common/error"))) {
            mockMvc.perform(builder.requestAttr("errorCode", 403).requestAttr("errorMessageLabel", "Prohibido"))
                    .andExpect(status().isForbidden())
                    .andExpect(view().name("common/error"));
        }
    }

    @Test
    void badRequestResponseStatusExceptionsAreNotLogged() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        assertThat(handler.shouldLogException(new ResponseStatusException(HttpStatus.BAD_REQUEST))).isFalse();
        assertThat(handler.shouldLogException(new ResponseStatusException(HttpStatus.FORBIDDEN))).isFalse();
        assertThat(handler.shouldLogException(new ResponseStatusException(HttpStatus.NOT_FOUND))).isFalse();
        assertThat(handler.shouldLogException(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR))).isTrue();
    }

    private String read(Object record, String component) throws Exception {
        Field field = record.getClass().getDeclaredField(component);
        field.setAccessible(true);
        return (String) field.get(record);
    }

    @Controller
    static class ProbeController {
        @GetMapping("/probe/error")
        void error() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado");
        }

        @PostMapping("/probe/bind")
        void bind(@Valid @ModelAttribute BindForm form) { }
    }

    static class BindForm {
        private Integer quantity;
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
