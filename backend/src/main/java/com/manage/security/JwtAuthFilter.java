package com.manage.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manage.common.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final List<String> WHITE_LIST = List.of(
            "/api/auth/login"
    );

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isWhiteListed(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, "未登录或token无效");
            return;
        }

        try {
            String token = authHeader.substring(7);
            var claims = jwtUtil.parseToken(token);
            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(Long.parseLong(claims.getSubject()));
            loginUser.setUsername(claims.get("username", String.class));
            UserContext.set(loginUser);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            writeUnauthorized(response, "登录已过期，请重新登录");
        } finally {
            UserContext.clear();
        }
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(401, msg)));
    }
}
