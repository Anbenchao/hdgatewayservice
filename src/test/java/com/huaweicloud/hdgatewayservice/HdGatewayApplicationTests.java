package com.huaweicloud.hdgatewayservice;

import com.huaweicloud.hdgatewayservice.config.RouteRegistry;
import com.huaweicloud.hdgatewayservice.model.RouteInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @SuppressWarnings("unchecked")
    @Test
    void productionApplicationYmlDeclaresGrowthRoutesWithEnvPlaceholders() throws Exception {
        Map<String, Object> root;
        try (InputStream in = new ClassPathResource("application.yml").getInputStream()) {
            root = new Yaml().load(in);
        }

        Map<String, Object> gateway = (Map<String, Object>) root.get("gateway");
        assertNotNull(gateway, "生产 application.yml 缺少 gateway 配置");
        List<Map<String, Object>> routes = (List<Map<String, Object>>) gateway.get("routes");
        assertNotNull(routes, "生产 application.yml 缺少 gateway.routes");
        Map<String, Map<String, Object>> byName = routes.stream()
                .collect(Collectors.toMap(r -> String.valueOf(r.get("name")), r -> r));

        Map<String, String> placeholders = Map.of(
                "growth-order-list", "${GW_ROUTE_GROWTH_ORDER_LIST_URL:}",
                "growth-user-list", "${GW_ROUTE_GROWTH_USER_LIST_URL:}");
        placeholders.forEach((name, placeholder) -> {
            Map<String, Object> def = byName.get(name);
            assertNotNull(def, "生产 application.yml 缺少路由: " + name);
            assertEquals("APIG", def.get("auth"), name + " auth 应为 APIG");
            assertEquals("POST", String.valueOf(def.get("methods")), name + " methods 应为 POST");
            assertEquals(placeholder, def.get("url"), name + " url 占位符键/缩进不正确");
        });
    }
}