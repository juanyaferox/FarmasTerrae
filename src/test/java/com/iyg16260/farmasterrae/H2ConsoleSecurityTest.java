package com.iyg16260.farmasterrae;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The H2 console is only enabled by the dev profile, and even then only ADMIN users may open it.
 */
@SpringBootTest (properties = {
        "spring.h2.console.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:h2consolesecuritydb;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
class H2ConsoleSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousUsersCannotOpenTheConsole() throws Exception {
        mockMvc.perform(get("/h2-console/"))
                .andExpect(request().attribute("errorCode", 401))
                .andExpect(forwardedUrl("/common/error"));
    }

    @Test
    @WithMockUser (roles = "USER")
    void regularUsersCannotOpenTheConsole() throws Exception {
        mockMvc.perform(get("/h2-console/"))
                .andExpect(request().attribute("errorCode", 403))
                .andExpect(forwardedUrl("/common/error"));
    }

    @Test
    @WithMockUser (roles = "ADMIN")
    void adminsCanPostConsoleFormsWithoutCsrfTokens() throws Exception {
        // MockMvc has no H2 servlet, so getting past Spring Security ends in a plain 404
        mockMvc.perform(post("/h2-console/login.do"))
                .andExpect(status().isNotFound());
    }

    @Test
    void framesAreLimitedToTheSameOriginWhileTheConsoleIsEnabled() throws Exception {
        mockMvc.perform(get("/auth"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }

    @Test
    void otherFormsStillRequireCsrfTokens() throws Exception {
        mockMvc.perform(post("/auth/register"))
                .andExpect(request().attribute("errorCode", 403))
                .andExpect(forwardedUrl("/common/error"));
    }
}
