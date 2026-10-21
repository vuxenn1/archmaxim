package com.archmaxim;

import com.archmaxim.exception.EmbeddingUnavailableException;
import com.archmaxim.search.EmbeddingClient;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the whole archive against a real Postgres with pgvector: Flyway migrations,
 * schema validation, the admin seed and the full upload, search and download flow.
 * The embedding service is replaced by a deterministic bag-of-words fake.
 */
@SpringBootTest(properties = {
        "archmaxim.jwt.secret=" + TestProperties.SECRET,
        "archmaxim.admin.password=first-admin-password",
        "archmaxim.search.max-distance=0.95"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ArchiveFlowIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@archmaxim.edu";
    private static final String ADMIN_PASSWORD = "first-admin-password";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) throws Exception {
        Path storage = Files.createTempDirectory("archmaxim-it");
        registry.add("archmaxim.storage.location", storage::toString);
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EmbeddingClient embeddingClient;

    @BeforeEach
    void fakeEmbeddings() {
        reset(embeddingClient);
        when(embeddingClient.embed(anyString())).thenAnswer(call -> FakeEmbeddings.of(call.getArgument(0)));
        when(embeddingClient.embedAll(anyList())).thenAnswer(call -> {
            List<String> texts = call.getArgument(0);
            return texts.stream().map(FakeEmbeddings::of).toList();
        });
    }

    @Test
    void uploadSearchDownloadAndRemove() throws Exception {
        String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        int n = SEQUENCE.incrementAndGet();

        long facultyId = createdId(post("/faculties").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Mühendislik Fakültesi " + n + "\"}"));
        long departmentId = createdId(post("/departments").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"CENG" + n + "\",\"name\":\"Bilgisayar Mühendisliği\",\"facultyId\":" + facultyId + "}"));
        long courseId = createdId(post("/courses").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"BIL20" + n + "\",\"name\":\"Bilgisayar Programlama II\",\"departmentId\":"
                        + departmentId + "}"));

        mvc.perform(post("/courses/" + courseId + "/aliases").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"alias\":\"BP2X" + n + "\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/courses/" + courseId + "/aliases").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"alias\":\"bp2x" + n + "\"}"))
                .andExpect(status().isConflict());

        String lecturerEmail = "ayse" + n + "@uni.edu";
        long lecturerId = createdId(post("/lecturers").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"ASSISTANT_PROFESSOR\",\"firstName\":\"Ayşe\",\"lastName\":\"Yılmaz\","
                        + "\"email\":\"" + lecturerEmail + "\",\"password\":\"lecturer-pass\",\"departmentId\":"
                        + departmentId + "}"));

        mvc.perform(post("/students").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("S" + n, lecturerEmail.toUpperCase(), departmentId)))
                .andExpect(status().isConflict());
        String studentEmail = "mehmet" + n + "@uni.edu";
        createdId(post("/students").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(studentJson("S" + n, studentEmail, departmentId)));

        mvc.perform(get("/students").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("password"))));

        String lecturer = login(lecturerEmail, "lecturer-pass");
        MockMultipartFile pdf = new MockMultipartFile("file", "Vize Soruları.pdf", "application/pdf",
                "exam content".getBytes(StandardCharsets.UTF_8));

        upload(pdf, courseId, lecturer).andExpect(status().isForbidden());

        mvc.perform(post("/course-lecturers").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":" + courseId + ",\"lecturerId\":" + lecturerId + "}"))
                .andExpect(status().isCreated());

        MockMultipartFile exe = new MockMultipartFile("file", "setup.exe", "application/octet-stream", new byte[]{1});
        upload(exe, courseId, lecturer)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Files of type .exe are not allowed."));

        String uploaded = upload(pdf, courseId, lecturer)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.indexed").value(true))
                .andExpect(jsonPath("$.file.originalName").value("Vize Soruları.pdf"))
                .andExpect(jsonPath("$.file.uploadedBy.name").value("Ayşe Yılmaz"))
                .andExpect(jsonPath("$.file.uploadedBy.email").doesNotExist())
                .andExpect(jsonPath("$.file.uuid").doesNotExist())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long fileId = ((Number) JsonPath.read(uploaded, "$.file.id")).longValue();

        String student = login(studentEmail, "student-pass");
        mvc.perform(get("/files/search").param("query", "bp2x" + n + " vize").header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(fileId))
                .andExpect(jsonPath("$[0].relevance").isNumber())
                .andExpect(jsonPath("$[0].course.id").value(courseId));

        mvc.perform(get("/files/search").param("query", "vize").param("mine", "true")
                        .header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get("/files/" + fileId + "/download").header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("filename*=UTF-8''Vize%20Sorular%C4%B1.pdf")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes("exam content".getBytes(StandardCharsets.UTF_8)));

        mvc.perform(get("/files").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == " + fileId + ")].downloadCount").value(1));

        mvc.perform(delete("/files/" + fileId).header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/files/" + fileId).header(HttpHeaders.AUTHORIZATION, lecturer))
                .andExpect(status().isNoContent());
        mvc.perform(get("/files/" + fileId + "/download").header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isNotFound());
        mvc.perform(get("/files/search").param("query", "bp2x" + n + " vize").header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(jsonPath("$[?(@.id == " + fileId + ")]").isEmpty());
    }

    @Test
    void uploadSucceedsWhileEmbeddingServiceIsDown() throws Exception {
        String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        int n = SEQUENCE.incrementAndGet();
        long facultyId = createdId(post("/faculties").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Fen Fakültesi " + n + "\"}"));
        long departmentId = createdId(post("/departments").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"FIZ" + n + "\",\"name\":\"Fizik\",\"facultyId\":" + facultyId + "}"));
        long courseId = createdId(post("/courses").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"FIZ10" + n + "\",\"name\":\"Fizik I\",\"departmentId\":" + departmentId + "}"));
        String email = "can" + n + "@uni.edu";
        long lecturerId = createdId(post("/lecturers").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"LECTURER\",\"firstName\":\"Can\",\"lastName\":\"Demir\",\"email\":\"" + email
                        + "\",\"password\":\"lecturer-pass\",\"departmentId\":" + departmentId + "}"));
        createdId(post("/course-lecturers").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"courseId\":" + courseId + ",\"lecturerId\":" + lecturerId + "}"));
        String lecturer = login(email, "lecturer-pass");

        EmbeddingUnavailableException outage = new EmbeddingUnavailableException("Search is temporarily unavailable.");
        when(embeddingClient.embedAll(anyList())).thenThrow(outage);
        when(embeddingClient.embed(anyString())).thenThrow(outage);

        MockMultipartFile notes = new MockMultipartFile("file", "hafta1.pdf", "application/pdf", new byte[]{1, 2, 3});
        upload(notes, courseId, lecturer)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.indexed").value(false));

        mvc.perform(get("/files/search").param("query", "fizik").header(HttpHeaders.AUTHORIZATION, lecturer))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Search is temporarily unavailable."));
    }

    @Test
    void reportsValidationErrorsAndBadCredentials() throws Exception {
        String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mvc.perform(post("/faculties").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        mvc.perform(post("/departments").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"X\",\"name\":\"Y\",\"facultyId\":999999}"))
                .andExpect(status().isNotFound());

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password."));

        mvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL));
    }

    @Test
    void changesPassword() throws Exception {
        String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        int n = SEQUENCE.incrementAndGet();
        long facultyId = createdId(post("/faculties").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Edebiyat Fakültesi " + n + "\"}"));
        long departmentId = createdId(post("/departments").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"TDE" + n + "\",\"name\":\"Türk Dili\",\"facultyId\":" + facultyId + "}"));
        String email = "zeynep" + n + "@uni.edu";
        createdId(post("/students").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(studentJson("T" + n, email, departmentId)));
        String student = login(email, "student-pass");

        mvc.perform(post("/auth/change-password").header(HttpHeaders.AUTHORIZATION, student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong-one\",\"newPassword\":\"brand-new-pass\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/auth/change-password").header(HttpHeaders.AUTHORIZATION, student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"student-pass\",\"newPassword\":\"brand-new-pass\"}"))
                .andExpect(status().isNoContent());

        login(email, "brand-new-pass");
    }

    private ResultActions upload(MockMultipartFile file, long courseId, String token) throws Exception {
        return mvc.perform(multipart("/files/upload")
                .file(file)
                .param("type", "MIDTERM")
                .param("courseId", String.valueOf(courseId))
                .param("year", "2025")
                .param("season", "FALL")
                .header(HttpHeaders.AUTHORIZATION, token));
    }

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private long createdId(RequestBuilder request) throws Exception {
        String body = mvc.perform(request)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static String studentJson(String number, String email, long departmentId) {
        return "{\"studentNumber\":\"" + number + "\",\"firstName\":\"Mehmet\",\"lastName\":\"Kaya\",\"email\":\""
                + email + "\",\"password\":\"student-pass\",\"departmentId\":" + departmentId + "}";
    }
}
