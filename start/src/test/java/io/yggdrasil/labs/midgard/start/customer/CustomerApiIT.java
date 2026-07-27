package io.yggdrasil.labs.midgard.start.customer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.yggdrasil.labs.midgard.start.Application;

/**
 * 迭代 4 全栈集成测试（failsafe 在 verify 阶段运行）。
 *
 * <p>通过 Testcontainers MySQL（application-test.yml 的 jdbc:tc URL）自动拉起数据库， 并在 Spring 上下文启动时由 Flyway
 * 执行 V1 迁移建表；因此本测试同时验证了：
 *
 * <ul>
 *   <li>Flyway 自动迁移（建表成功，否则上下文无法启动）
 *   <li>Controller 的 HTTP 契约（状态码 / 响应信封 / 错误码）
 *   <li>Repository 持久化往返（写入后可读回、可更新、可删除）
 * </ul>
 *
 * <p>类级 {@code @Transactional} 使每个测试方法在结束后回滚，保证用例间数据隔离。
 */
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CustomerApiIT {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired private MockMvc mockMvc;

    private long createCustomer(String name, String email, String phone) throws Exception {
        String body =
                String.format(
                        "{\"name\":\"%s\",\"email\":\"%s\",\"phone\":\"%s\"}", name, email, phone);
        String response =
                mockMvc.perform(
                                post("/api/customers")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.name").value(name))
                        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return MAPPER.readTree(response).get("data").get("id").asLong();
    }

    @Test
    void create_returns201_andSupportsFullLifecycle() throws Exception {
        long id =
                createCustomer(
                        "迭代四客户", "iter4-" + System.nanoTime() + "@example.com", "13800000000");

        mockMvc.perform(get("/api/customers/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.email").exists());

        mockMvc.perform(
                        put("/api/customers/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"name\":\"改名后的客户\",\"email\":\"iter4-update-"
                                                + System.nanoTime()
                                                + "@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("改名后的客户"));

        mockMvc.perform(delete("/api/customers/" + id)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/customers/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void create_validationFailure_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"x\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_duplicateEmail_returns409() throws Exception {
        String email = "dup-" + System.nanoTime() + "@example.com";
        createCustomer("客户A", email, "13800000001");

        mockMvc.perform(
                        post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"客户B\",\"email\":\"" + email + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }

    @Test
    void list_returnsPagedData() throws Exception {
        createCustomer("列表客户1", "list1-" + System.nanoTime() + "@example.com", "13800000002");
        createCustomer("列表客户2", "list2-" + System.nanoTime() + "@example.com", "13800000003");

        mockMvc.perform(get("/api/customers").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalCount").isNumber());
    }

    @Test
    void list_keywordMatchesRecordsBeyondTheUnfilteredPage() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        for (int i = 0; i < 10; i++) {
            createCustomer(
                    "普通客户" + i, "unmatched-" + suffix + "-" + i + "@example.com", "1380000000" + i);
        }
        createCustomer("命中客户", "needle-" + suffix + "@example.com", "13800000009");

        mockMvc.perform(
                        get("/api/customers")
                                .param("page", "1")
                                .param("size", "10")
                                .param("keyword", "needle-" + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].email").value("needle-" + suffix + "@example.com"))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void list_invalidPageSize_returns400() throws Exception {
        mockMvc.perform(get("/api/customers").param("page", "1").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void update_nonExisting_returns404() throws Exception {
        mockMvc.perform(
                        put("/api/customers/999999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"幽灵\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void delete_nonExisting_returns404() throws Exception {
        mockMvc.perform(delete("/api/customers/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
