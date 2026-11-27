package com.iyg16260.farmasterrae;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:securitytestdb;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
class SecurityAccessBaselineTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"/", "/products", "/cart", "/auth", "/auth/register", "/auth/forgotPassword"})
    void publicRoutesAreAvailableAnonymously(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin/dashboard", "/user/dashboard", "/order", "/auth/changePassword"})
    void protectedRoutesRejectAnonymousUsers(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(request().attribute("errorCode", 401))
                .andExpect(forwardedUrl("/common/error"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void regularUserCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(request().attribute("errorCode", 403))
                .andExpect(forwardedUrl("/common/error"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/auth/register"))
                .andExpect(request().attribute("errorCode", 403))
                .andExpect(forwardedUrl("/common/error"));
    }
}
