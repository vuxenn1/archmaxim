package com.archmaxim.web;

import com.archmaxim.TestProperties;
import com.archmaxim.config.AppConfig;
import com.archmaxim.config.ArchmaximProperties;
import com.archmaxim.config.SecurityConfig;
import com.archmaxim.controller.AuthController;
import com.archmaxim.controller.FileController;
import com.archmaxim.controller.StudentController;
import com.archmaxim.dto.LoginResponse;
import com.archmaxim.dto.PageResponse;
import com.archmaxim.entity.Role;
import com.archmaxim.search.IndexingService;
import com.archmaxim.search.SearchService;
import com.archmaxim.security.AuthenticatedUser;
import com.archmaxim.security.JwtService;
import com.archmaxim.security.SecurityProblemHandler;
import com.archmaxim.service.AuthService;
import com.archmaxim.service.FileService;
import com.archmaxim.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, FileController.class, StudentController.class})
@Import({SecurityConfig.class, SecurityProblemHandler.class, JwtService.class, AppConfig.class})
@TestPropertySource(properties = "archmaxim.jwt.secret=" + TestProperties.SECRET)
class SecurityRulesTest {

    @TestConfiguration
    @EnableConfigurationProperties(ArchmaximProperties.class)
    static class PropertiesConfig {
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ArchmaximProperties properties;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private FileService fileService;

    @MockitoBean
    private SearchService searchService;

    @MockitoBean
    private IndexingService indexingService;

    @MockitoBean
    private StudentService studentService;

    @Test
    void loginIsOpenToEveryone() throws Exception {
        when(authService.login(any(), anyString()))
                .thenReturn(new LoginResponse("token", 1L, "System Admin", Role.ADMIN, Instant.now()));

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@archmaxim.edu\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"));
    }

    @Test
    void anonymousSearchIsRejectedWithProblemDetail() throws Exception {
        mvc.perform(get("/files/search").param("query", "vize"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("You need to log in to do this."))
                .andExpect(jsonPath("$.instance").value("/files/search"));
    }

    @Test
    void expiredTokenGetsItsOwnMessage() throws Exception {
        JwtService pastIssuer = new JwtService(properties, Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC));
        String expired = pastIssuer.issue(new AuthenticatedUser(1, "student@uni.edu", Role.STUDENT)).value();

        mvc.perform(get("/files/search").param("query", "vize").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Your session has expired. Please log in again."));
    }

    @Test
    void garbageTokenIsTreatedAsAnonymous() throws Exception {
        mvc.perform(get("/files/search").param("query", "vize").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCanSearch() throws Exception {
        when(searchService.search(any(), any())).thenReturn(List.of());

        mvc.perform(get("/files/search").param("query", "vize").header(HttpHeaders.AUTHORIZATION, bearer(Role.STUDENT)))
                .andExpect(status().isOk());
    }

    @Test
    void studentCannotListStudents() throws Exception {
        mvc.perform(get("/students").header(HttpHeaders.AUTHORIZATION, bearer(Role.STUDENT)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("You do not have permission to do this."));
    }

    @Test
    void adminCanListStudents() throws Exception {
        when(studentService.findPage(anyInt(), anyInt())).thenReturn(new PageResponse<>(List.of(), 0, 50, 0, 0));

        mvc.perform(get("/students").header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void onlyLecturersCanUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "vize.pdf", "application/pdf", new byte[]{1});

        mvc.perform(uploadRequest(file).header(HttpHeaders.AUTHORIZATION, bearer(Role.LECTURER)))
                .andExpect(status().isCreated());
        mvc.perform(uploadRequest(file).header(HttpHeaders.AUTHORIZATION, bearer(Role.STUDENT)))
                .andExpect(status().isForbidden());
        mvc.perform(uploadRequest(file).header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isForbidden());
        mvc.perform(uploadRequest(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void indexingIsAdminOnly() throws Exception {
        mvc.perform(post("/files/index").header(HttpHeaders.AUTHORIZATION, bearer(Role.LECTURER)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/files/index").header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isAccepted());
    }

    @Test
    void unknownPathsAreDenied() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mvc.perform(get("/swagger-ui.html").header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidSearchParametersAreReportedPerField() throws Exception {
        mvc.perform(get("/files/search").param("query", " ").header(HttpHeaders.AUTHORIZATION, bearer(Role.STUDENT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.query").exists());
    }

    private static MockMultipartHttpServletRequestBuilder uploadRequest(MockMultipartFile file) {
        return multipart("/files/upload")
                .file(file)
                .param("type", "MIDTERM")
                .param("courseId", "1")
                .param("year", "2026")
                .param("season", "FALL");
    }

    private String bearer(Role role) {
        return "Bearer " + jwtService.issue(new AuthenticatedUser(7, role.name().toLowerCase() + "@uni.edu", role)).value();
    }
}
