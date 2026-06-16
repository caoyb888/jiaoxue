package cn.smu.edu.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class SentinelConfig {

    private final List<ViewResolver> viewResolvers;
    private final ServerCodecConfigurer serverCodecConfigurer;

    public SentinelConfig(ObjectProvider<List<ViewResolver>> viewResolversProvider,
                          ServerCodecConfigurer serverCodecConfigurer) {
        this.viewResolvers = viewResolversProvider.getIfAvailable(Collections::emptyList);
        this.serverCodecConfigurer = serverCodecConfigurer;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
        return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
    }

    @Bean
    @Order(-1)
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }

    @Bean
    public BlockRequestHandler blockRequestHandler() {
        BlockRequestHandler handler = (exchange, t) ->
                ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(
                                "{\"code\":429,\"msg\":\"请求过于频繁，请稍后再试\",\"data\":null}"));
        GatewayCallbackManager.setBlockHandler(handler);
        return handler;
    }

    @PostConstruct
    public void initGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        // 签到接口：1500 QPS（高并发课堂签到场景）
        rules.add(new GatewayFlowRule("edu-interaction")
                .setCount(1500)
                .setIntervalSec(1));

        // AI 接口：100 并发（防 GPU/API 过载）
        rules.add(new GatewayFlowRule("edu-ai")
                .setCount(100)
                .setIntervalSec(1));

        // 认证接口：50 QPS（防暴力登录）
        rules.add(new GatewayFlowRule("edu-auth")
                .setCount(50)
                .setIntervalSec(1));

        // 考试交卷：500 QPS（打散后峰值）
        rules.add(new GatewayFlowRule("edu-exam")
                .setCount(500)
                .setIntervalSec(1));

        // 全局兜底：3000 QPS
        rules.add(new GatewayFlowRule("DEFAULT")
                .setCount(3000)
                .setIntervalSec(1));

        GatewayRuleManager.loadRules(rules);
    }
}
