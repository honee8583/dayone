package com.dayone.sample.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";    // 인증타입 JWT는 토큰앞에 Bearer를 붙인다.

    private final TokenProvider tokenProvider;

    // 요청이 들어올때마다 필터 실행
    // 요청이 들어올때마다 토큰의 존재를 확인하고 유효한 토큰인지 검사
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 1. 요청의 헤더에 토큰이 있는지 확인하고 가져온다.
        String token = this.resolveTokenFormRequest(request);

        // 2. 토큰이 있고 유효하다면 인증정보를 SecurityContext에 삽입
        if (StringUtils.hasText(token) && this.tokenProvider.validateToken(token)) {
            // 토큰 유효성 검증
            Authentication auth = this.tokenProvider.getAuthentication(token);

            // 사용자명 -> 요청경로명
            log.info(String.format("[%s] -> %s", this.tokenProvider.getUsername(token), request.getRequestURI()));

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        // 3. 다음 필터로 진행
//        response.addHeader("Authorization", "Bearer "+ token);   // Jwt 토큰 응답
        filterChain.doFilter(request, response);
    }

    /**
     * 클라이언트에서 전송된 HttpServletRequest의 헤더에서 토큰 정보 추출
     */
    private String resolveTokenFormRequest(HttpServletRequest request) {
        String token = request.getHeader(TOKEN_HEADER); // 헤더에서 키("Authorization")에 해당하는 value 값을 추출

        if (!ObjectUtils.isEmpty(token) && token.startsWith(TOKEN_PREFIX)) {
            return token.substring(TOKEN_PREFIX.length());  // "Bearer"뒤의 토큰 부분을 떼어내서 반환
        }

        return null;
    }
}
