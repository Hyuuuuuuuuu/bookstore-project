package com.hutech.bookstore.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoggingInterceptor implements HandlerInterceptor {

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) throws Exception {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();
        
        // Chỉ log các request API (bỏ qua static resources)
        if (uri.startsWith("/api") || uri.startsWith("/uploads")) {
            System.out.println(String.format("[%s] %s - %d", method, uri, status));
        }
    }
}

