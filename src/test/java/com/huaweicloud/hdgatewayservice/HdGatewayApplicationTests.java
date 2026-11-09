package com.huaweicloud.hdgatewayservice;

import com.huaweicloud.hdgatewayservice.config.RouteRegistry;
import com.huaweicloud.hdgatewayservice.model.RouteInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class HdGatewayApplicationTests {

    @MockBean
    private com.huaweicloud.hdgatewayservice.service.InvokeService invokeService;

    @Autowired
    private RouteRegistry routeRegistry;

    @Test
    void contextLoads() {
    }

    @Test
    void growthRoutesWiredWithApigPostAndNoUrlLeak() {
        List<RouteInfo> routes = routeRegistry.list();
        Map<String, RouteInfo> byName = routes.stream()
                .collect(Collectors.toMap(RouteInfo::name, r -> r));

        for (String name : List.of("growth-order-list", "growth-user-list")) {
            RouteInfo info = byName.get(name);
            assertTrue(info != null, "缺少路由: " + name);
            assertEquals("APIG", info.auth(), name + " auth 应为 APIG");
            assertEquals(List.of("POST"), info.methods(), name + " methods 应为 POST");
            assertTrue(info.configured(), name + " 在测试夹具中应已配置 url");
            assertFalse(info.toString().contains("growth.example.com"), name + " 不应泄露上游 URL");
        }
    }
}