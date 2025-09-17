package com.hmall.gateway.filters;

import com.hmall.common.exception.UnauthorizedException;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final AuthProperties authProperties;

    private final JwtTool jwtTool;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // 1.獲取 request
        ServerHttpRequest request = exchange.getRequest();
        //2.判斷是否需要做登陸攔截
        if (isExclude(request.getPath().toString())){ //轉成字串
            // 放行進到下一個filter
            return chain.filter(exchange);
        }
        //3. 獲得Token
        String token = null;
        List<String> headers = request.getHeaders().get("authorization");
        if (headers != null && !headers.isEmpty()){
            token = headers.get(0);
        }
        //4. 校驗並解析Token
        Long userId = null;
        try {
            userId = jwtTool.parseToken(token); //裡面會解析token是null的情況
        } catch (UnauthorizedException e) {
            // 攔截，設定響應狀態碼401
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete(); // 這個請求到此為止，不會往下傳遞，不會執行其他Filter跟後端的其他請求
        }
        //5. 將用戶訊息寫入請求頭
        System.out.println("userId = " + userId);

        //6. 放行
        return chain.filter(exchange);
    }

    private boolean isExclude(String path) {
        for (String pathPattern : authProperties.getExcludePaths()) {
            if (antPathMatcher.match(pathPattern, path)) { // spring提供的匹配類似正則工具類
               return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
