package com.skicoach.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.skicoach.backend.dto.auth.LoginRequest;
import com.skicoach.backend.dto.auth.RegisterRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户接口集成测试(P2.2)
 *
 * 注意: 此测试需要MySQL和Redis启动。
 * 顺序执行: 注册 -> 登录 -> 获取资料 -> 修改资料 -> 登出
 *
 * 每次运行使用随机手机号,避免测试间数据干扰。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // 用随机手机号避免数据冲突
    private static final String TEST_PHONE = "139" + String.format("%08d",
            Math.abs(UUID.randomUUID().hashCode() % 100000000));
    private static final String TEST_PASSWORD = "test_pass_123";

    private static String savedToken;

    @Test
    @Order(1)
    void testRegister() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setPhone(TEST_PHONE);
        req.setPassword(TEST_PASSWORD);
        req.setNickname("测试用户");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        savedToken = body.get("data").get("token").asText();
        assertNotNull(savedToken);
    }

    @Test
    @Order(2)
    void testLogin() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setPhone(TEST_PHONE);
        req.setPassword(TEST_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        savedToken = body.get("data").get("token").asText();
    }

    @Test
    @Order(3)
    void testGetProfileWithoutToken() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4010));   // UNAUTHORIZED
    }

    @Test
    @Order(4)
    void testGetProfileWithToken() throws Exception {
        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer " + savedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.nickname").value("测试用户"));
    }

    @Test
    @Order(5)
    void testLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + savedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @Order(6)
    void testTokenInBlacklistAfterLogout() throws Exception {
        // 登出后,Token应该已加入黑名单
        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer " + savedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4011));   // TOKEN_INVALID
    }

    @Test
    @Order(7)
    void testRegisterDuplicate() throws Exception {
        // 重复注册应该失败
        RegisterRequest req = new RegisterRequest();
        req.setPhone(TEST_PHONE);
        req.setPassword(TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4103));   // USER_PHONE_EXISTS
    }

    @Test
    @Order(8)
    void testLoginWithWrongPassword() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setPhone(TEST_PHONE);
        req.setPassword("wrong_password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4104));   // USER_PASSWORD_WRONG
    }
}
