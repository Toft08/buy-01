// package com.buyapp.userservice.controller;

// import com.buyapp.common.dto.UserDto;
// import com.buyapp.userservice.model.User;
// import com.buyapp.userservice.repository.UserRepository;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import org.junit.jupiter.api.*;
// import org.springframework.beans.factory.annotation.Autowired;
// import
// org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.http.MediaType;
// import org.springframework.test.context.DynamicPropertyRegistry;
// import org.springframework.test.context.DynamicPropertySource;
// import org.springframework.test.web.servlet.MockMvc;
// import org.testcontainers.containers.MongoDBContainer;
// import org.testcontainers.junit.jupiter.Container;
// import org.testcontainers.junit.jupiter.Testcontainers;

// import static
// org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
// import static
// org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
// import static org.hamcrest.Matchers.*;

// @SpringBootTest
// @AutoConfigureMockMvc
// @Testcontainers
// @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
// class AuthControllerIntegrationTest {

// @Container
// static MongoDBContainer mongoDBContainer = new
// MongoDBContainer("mongo:latest")
// .withExposedPorts(27017);

// @DynamicPropertySource
// static void setProperties(DynamicPropertyRegistry registry) {
// registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
// }

// @Autowired
// private MockMvc mockMvc;

// @Autowired
// private ObjectMapper objectMapper;

// @Autowired
// private UserRepository userRepository;

// private static String clientToken;
// private static String sellerToken;

// @BeforeEach
// void setUp() {
// userRepository.deleteAll();
// }

// @Test
// @Order(1)
// @DisplayName("Should register CLIENT user successfully")
// void testRegisterClient() throws Exception {
// UserDto userDto = new UserDto();
// userDto.setName("Test Client");
// userDto.setEmail("client@test.com");
// userDto.setPassword("pass123");
// userDto.setRole("CLIENT");

// mockMvc.perform(post("/auth/register")
// .contentType(MediaType.APPLICATION_JSON)
// .content(objectMapper.writeValueAsString(userDto)))
// .andExpect(status().isOk())
// .andExpect(jsonPath("$.token").exists())
// .andExpect(jsonPath("$.user.email").value("client@test.com"))
// .andExpect(jsonPath("$.user.role").value("client"))
// .andExpect(jsonPath("$.message").value("User registered successfully"));
// }

// @Test
// @Order(2)
// @DisplayName("Should register SELLER user successfully")
// void testRegisterSeller() throws Exception {
// UserDto userDto = new UserDto();
// userDto.setName("Test Seller");
// userDto.setEmail("seller@test.com");
// userDto.setPassword("pass123");
// userDto.setRole("SELLER");

// mockMvc.perform(post("/auth/register")
// .contentType(MediaType.APPLICATION_JSON)
// .content(objectMapper.writeValueAsString(userDto)))
// .andExpect(status().isOk())
// .andExpect(jsonPath("$.token").exists())
// .andExpect(jsonPath("$.user.email").value("seller@test.com"))
// .andExpect(jsonPath("$.user.role").value("seller"));
// }

// @Test
// @Order(3)
// @DisplayName("Should reject duplicate email registration")
// void testRegisterDuplicateEmail() throws Exception {
// // Register first user
// UserDto userDto = new UserDto();
// userDto.setName("First User");
// userDto.setEmail("duplicate@test.com");
// userDto.setPassword("pass123");
// userDto.setRole("CLIENT");

// mockMvc.perform(post("/auth/register")
// .contentType(MediaType.APPLICATION_JSON)
// .content(objectMapper.writeValueAsString(userDto)))
// .andExpect(status().isOk());

// // Try to register with same email
// mockMvc.perform(post("/auth/register")
// .contentType(MediaType.APPLICATION_JSON)
// .content(objectMapper.writeValueAsString(userDto)))
// .andExpect(status().isBadRequest())
// .andExpect(jsonPath("$.message").value(containsString("already exists")));
// }

// @Test
// @Order(4)
// @DisplayName("Should login CLIENT user and return JWT token")
// void testLoginClient() throws Exception {
// // First register
// UserDto userDto = new UserDto();
// userDto.setName("Login Test Client");
// userDto.setEmail("loginclient@test.com");
// userDto.setPassword("pass123");
// userDto.setRole("CLIENT");

// mockMvc.perform(post("/auth/register")
// .contentType(MediaType.APPLICATION_JSON)
// .content(objectMapper.writeValueAsString(userDto)));

// // Then login
// String loginData =
// "{\"email\":\"loginclient@test.com\",\"password\":\"pass123\"}";

// mockMvc.perform(post("/auth/login")
// .contentType(MediaType.APPLICATION_JSON)
// .content(loginData))
// .andExpect(status().isOk())
// .andExpect(jsonPath("$.token").exists())
// .andExpect(jsonPath("$.message").value("Login successful"));
// }

// @Test
// @Order(5)
// @DisplayName("Should reject login with wrong password")
// void testLoginWrongPassword() throws Exception {
// // Register user
// UserDto userDto = new UserDto();
// userDto.setName("Password Test");
// userDto.setEmail("passtest@test.com");
// userDto.setPassword("correctpass");
// userDto.setRole("CLIENT");

// mockMvc.perform(post("/auth/register")
// .contentType(MediaType.APPLICATION_JSON)
// .content(objectMapper.writeValueAsString(userDto)));

// // Try login with wrong password
// String loginData =
// "{\"email\":\"passtest@test.com\",\"password\":\"wrongpass\"}";

// mockMvc.perform(post("/auth/login")
// .contentType(MediaType.APPLICATION_JSON)
// .content(loginData))
// .andExpect(status().isUnauthorized())
// .andExpect(jsonPath("$.message").value(containsString("Invalid email or
// password")));
// }

// @Test
// @Order(6)
// @DisplayName("Should reject login with non-existent email")
// void testLoginNonExistentUser() throws Exception {
// String loginData =
// "{\"email\":\"nonexistent@test.com\",\"password\":\"pass123\"}";

// mockMvc.perform(post("/auth/login")
// .contentType(MediaType.APPLICATION_JSON)
// .content(loginData))
// .andExpect(status().isUnauthorized())
// .andExpect(jsonPath("$.message").value(containsString("Invalid email or
// password")));
// }

// @Test
// @Order(7)
// @DisplayName("Should logout successfully with valid token")
// void testLogout() throws Exception {
// // Register and get token
// UserDto userDto = new UserDto();
// userDto.setName("Logout Test");
// userDto.setEmail("logout@test.com");
// userDto.setPassword("pass123");
// userDto.setRole("CLIENT");

// String registerResponse = mockMvc.perform(post("/auth/register")
// .contentType(MediaType.APPLICATION_JSON)
// .content(objectMapper.writeValueAsString(userDto)))
// .andReturn().getResponse().getContentAsString();

// String token = objectMapper.readTree(registerResponse).get("token").asText();

// // Logout
// mockMvc.perform(post("/auth/logout")
// .header("Authorization", "Bearer " + token))
// .andExpect(status().isOk())
// .andExpect(content().string("Logged out successfully."));
// }

// @Test
// @Order(8)
// @DisplayName("Should default to CLIENT role when role not specified")
// void testRegisterDefaultRole() throws Exception {
// UserDto userDto = new UserDto();
// userDto.setName("Default Role User");
// userDto.setEmail("defaultrole@test.com");
// userDto.setPassword("pass123");
// // Not setting role

// mockMvc.perform(post("/auth/register")
// .contentType(MediaType.APPLICATION_JSON)
// .content(objectMapper.writeValueAsString(userDto)))
// .andExpect(status().isOk())
// .andExpect(jsonPath("$.user.role").value("client"));
// }
// }
