package com.buyapp.mediaservice.controller;

import com.buyapp.common.test.JwtTestUtil;
import com.buyapp.mediaservice.model.Media;
import com.buyapp.mediaservice.repository.MediaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MediaControllerIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MediaRepository mediaRepository;

    private static String sellerToken;
    private static String clientToken;
    private static String mediaId;

    @BeforeAll
    static void setupTokens() {
        // Generate real JWT tokens using test utility
        sellerToken = JwtTestUtil.generateSellerToken("seller@test.com");
        clientToken = JwtTestUtil.generateClientToken("client@test.com");
    }

    @BeforeEach
    void setUp() {
        mediaRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Should upload media file successfully")
    void testUploadMedia() throws Exception {
        byte[] fileContent = "Test file content for media upload".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                fileContent);

        MvcResult result = mockMvc.perform(multipart("/media/upload")
                .file(file)
                .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("test-image.jpg"))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.size").value(fileContent.length))
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        mediaId = objectMapper.readTree(response).get("id").asText();
    }

    @Test
    @Order(2)
    @DisplayName("Should reject file upload exceeding 2MB limit")
    void testUploadOversizedFile() throws Exception {
        // Create 3MB file (exceeds 2MB limit)
        byte[] largeContent = new byte[3 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large-file.jpg",
                "image/jpeg",
                largeContent);

        mockMvc.perform(multipart("/media/upload")
                .file(file)
                .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("exceeds")));
    }

    @Test
    @Order(3)
    @DisplayName("Should reject file upload without authentication")
    void testUploadWithoutAuth() throws Exception {
        byte[] fileContent = "Test content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "unauthorized.jpg",
                "image/jpeg",
                fileContent);

        mockMvc.perform(multipart("/media/upload")
                .file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(4)
    @DisplayName("Should download media file by ID")
    void testDownloadMedia() throws Exception {
        // Upload first
        byte[] fileContent = "Download test content".getBytes();
        Media media = new Media();
        media.setFilename("download-test.jpg");
        media.setContentType("image/jpeg");
        media.setSize((long) fileContent.length);
        media.setData(fileContent);
        media.setUserId("test-user-id");
        media = mediaRepository.save(media);

        mockMvc.perform(get("/media/" + media.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(fileContent));
    }

    @Test
    @Order(5)
    @DisplayName("Should return 404 for non-existent media")
    void testDownloadNonExistentMedia() throws Exception {
        mockMvc.perform(get("/media/nonexistent123456"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @Order(6)
    @DisplayName("Should delete media when owner")
    void testDeleteMediaAsOwner() throws Exception {
        byte[] fileContent = "Delete test content".getBytes();
        Media media = new Media();
        media.setFilename("delete-test.jpg");
        media.setContentType("image/jpeg");
        media.setSize((long) fileContent.length);
        media.setData(fileContent);
        media.setUserId("seller-user-id");
        media = mediaRepository.save(media);

        mockMvc.perform(delete("/media/" + media.getId())
                .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk());

        // Verify deletion
        mockMvc.perform(get("/media/" + media.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(7)
    @DisplayName("Should reject deletion when not owner")
    void testDeleteMediaAsNonOwner() throws Exception {
        byte[] fileContent = "Protected content".getBytes();
        Media media = new Media();
        media.setFilename("protected.jpg");
        media.setContentType("image/jpeg");
        media.setSize((long) fileContent.length);
        media.setData(fileContent);
        media.setUserId("seller-user-id");
        media = mediaRepository.save(media);

        mockMvc.perform(delete("/media/" + media.getId())
                .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    @DisplayName("Should handle multiple file uploads")
    void testMultipleFileUploads() throws Exception {
        byte[] file1Content = "First file content".getBytes();
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "file1.jpg",
                "image/jpeg",
                file1Content);

        MvcResult result1 = mockMvc.perform(multipart("/media/upload")
                .file(file1)
                .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("file1.jpg"))
                .andReturn();

        byte[] file2Content = "Second file content".getBytes();
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "file2.png",
                "image/png",
                file2Content);

        MvcResult result2 = mockMvc.perform(multipart("/media/upload")
                .file(file2)
                .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("file2.png"))
                .andReturn();

        // Verify both files exist
        String id1 = objectMapper.readTree(result1.getResponse().getContentAsString()).get("id").asText();
        String id2 = objectMapper.readTree(result2.getResponse().getContentAsString()).get("id").asText();

        Assertions.assertNotEquals(id1, id2, "File IDs should be different");
    }

    @Test
    @Order(9)
    @DisplayName("Should upload various image types")
    void testVariousImageTypes() throws Exception {
        String[] contentTypes = { "image/jpeg", "image/png", "image/gif", "image/webp" };
        String[] filenames = { "test.jpg", "test.png", "test.gif", "test.webp" };

        for (int i = 0; i < contentTypes.length; i++) {
            byte[] content = ("Test content for " + filenames[i]).getBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    filenames[i],
                    contentTypes[i],
                    content);

            mockMvc.perform(multipart("/media/upload")
                    .file(file)
                    .header("Authorization", "Bearer " + sellerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.filename").value(filenames[i]))
                    .andExpect(jsonPath("$.contentType").value(contentTypes[i]));
        }
    }

    @Test
    @Order(10)
    @DisplayName("Should reject empty file upload")
    void testUploadEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]);

        mockMvc.perform(multipart("/media/upload")
                .file(file)
                .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("empty")));
    }
}
