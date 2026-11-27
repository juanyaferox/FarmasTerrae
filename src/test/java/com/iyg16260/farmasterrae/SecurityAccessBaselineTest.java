package com.iyg16260.farmasterrae;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.model.Profile;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.ProfileRepository;
import com.iyg16260.farmasterrae.repository.UserRepository;
import com.iyg16260.farmasterrae.service.UserService;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.web.servlet.filter.OrderedHiddenHttpMethodFilter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.RememberMeServices;
import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:securitytestdb;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
class SecurityAccessBaselineTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired private ApplicationContext applicationContext;
    @Autowired private RememberMeServices rememberMeServices;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;

    @ParameterizedTest
    @ValueSource(strings = {
            "/", "/products", "/products/categories", "/cart",
            "/auth", "/auth/register", "/auth/forgotPassword", "/auth/resetPassword",
            "/css/tokens.css", "/favicon.ico"
    })
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

    @Test
    void unmappedRouteRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/unmapped-private-route"))
                .andExpect(request().attribute("errorCode", 401))
                .andExpect(forwardedUrl("/common/error"));
    }

    @Test
    void staleRememberMeCookieIsCancelledWithoutServerError() throws Exception {
        mockMvc.perform(get("/auth").cookie(new Cookie("remember-me", "invalid-token")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("Max-Age=0")));
    }

    @Test
    void rememberMeCookieUsesHttpOnlyAndStrictSameSite() {
        UserDetails principal = org.springframework.security.core.userdetails.User.withUsername("cookie-user")
                .password("unused").authorities(AuthorityUtils.createAuthorityList("ROLE_USER")).build();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, "unused", principal.getAuthorities());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true); request.setParameter("remember-me", "on");
        MockHttpServletResponse response = new MockHttpServletResponse();
        rememberMeServices.loginSuccess(request, response, authentication);
        assertThat(response.getHeader("Set-Cookie")).contains("HttpOnly", "SameSite=Strict", "Secure");
    }

    @Test
    void orderedHiddenMethodFilterRunsBeforeSpringSecurity() {
        OrderedHiddenHttpMethodFilter filter = applicationContext.getBean(OrderedHiddenHttpMethodFilter.class);
        assertThat(filter.getOrder()).isLessThan(SecurityProperties.DEFAULT_FILTER_ORDER);
    }

    @Test
    void resetPasswordGetSetsNoReferrerAndFlagsInvalidToken() throws Exception {
        mockMvc.perform(get("/auth/resetPassword").param("token", "bogus-token"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Referrer-Policy", "no-referrer"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("no es válido o ha caducado")));
    }

    @Test
    void renamingUserRevokesRememberMeTokensSoNewOwnerOfOldUsernameIsNotHijacked() throws Exception {
        Profile userProfile = profileRepository.findByType("USER");
        User original = new User();
        original.setUsername("rename_alice");
        original.setEmail("rename_alice@example.com");
        original.setPassword(passwordEncoder.encode("StrongPassword9!"));
        original.setProfile(userProfile);
        original = userRepository.save(original);

        var auth = UsernamePasswordAuthenticationToken.authenticated(original, null, original.getAuthorities());
        MockHttpServletRequest loginReq = new MockHttpServletRequest();
        loginReq.setParameter("remember-me", "on");
        MockHttpServletResponse loginRes = new MockHttpServletResponse();
        rememberMeServices.loginSuccess(loginReq, loginRes, auth);
        String setCookie = loginRes.getHeader("Set-Cookie");
        assertThat(setCookie).isNotBlank();
        String cookieVal = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));

        // Rename without changing role
        UserDTO renameDto = new UserDTO();
        renameDto.setUsername("rename_alice_new");
        renameDto.setEmail("rename_alice@example.com");
        renameDto.setType("USER");
        userService.updateUserById(original.getId(), renameDto);

        // Another user later claims the old username
        User newcomer = new User();
        newcomer.setUsername("rename_alice");
        newcomer.setEmail("newcomer_alice@example.com");
        newcomer.setPassword(passwordEncoder.encode("OtherPassword9!"));
        newcomer.setProfile(userProfile);
        userRepository.save(newcomer);

        // Old remember-me cookie must NOT authenticate as newcomer
        mockMvc.perform(get("/user/dashboard").cookie(new Cookie("remember-me", cookieVal)))
                .andExpect(request().attribute("errorCode", 401))
                .andExpect(forwardedUrl("/common/error"));
    }

    @Test
    void invalidRegistrationUsernameKeepsNonPasswordFieldsAndDoesNotPersist() throws Exception {
        long before = userRepository.count();
        mockMvc.perform(post("/auth/register").with(csrf())
                        .param("username", "bad username")
                        .param("email", "preserved@example.com")
                        .param("password", "GoodPassword9!")
                        .param("confirmPassword", "GoodPassword9!"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.view().name("auth/register"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attributeHasFieldErrors("registerForm", "username"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("preserved@example.com")));
        assertThat(userRepository.count()).isEqualTo(before);
    }

    @Test
    void registrationRejectsMismatchedConfirmationAndKeepsUsernameAndEmail() throws Exception {
        mockMvc.perform(post("/auth/register").with(csrf())
                        .param("username", "valid_user")
                        .param("email", "preserved@example.com")
                        .param("password", "GoodPassword9!")
                        .param("confirmPassword", "OtherPassword9!"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attributeHasFieldErrors("registerForm", "confirmationValid"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("valid_user")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("preserved@example.com")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("GoodPassword9!"))));
    }
}
