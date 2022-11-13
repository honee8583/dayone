package com.dayone.sample.security;

import com.dayone.sample.service.MemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TokenProvider {

    private static final long TOKEN_EXPIRE_TIME = 1000 * 60 * 60;   // 1 hour
    private static final String KEY_ROLES = "roles";

    private final MemberService memberService;

    @Value("{spring.jwt.secret}")
    private String secretKey;

    /**
     * 토큰 생성(발급)
     */
    public String generateToken(String username, List<String> roles) {
        Claims claims = Jwts.claims().setSubject(username);
        claims.put(KEY_ROLES, roles);

        var now = new Date();   // 현재시간
        var expiredDate = new Date(now.getTime() + TOKEN_EXPIRE_TIME);   // 만료시간

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)   // 생성된 시간
                .setExpiration(expiredDate) // 토큰 만료시간
                .signWith(SignatureAlgorithm.HS512, this.secretKey) // 사용할 암호화 알고리즘, 비밀키
                .compact();
    }

    /**
     * 인증정보를 가져와 스프링에서 지원하는 토큰으로 반환
     */
    public Authentication getAuthentication(String jwt) {   // jwt 토큰으로부터 인증정보를 가져오는 메소드
        UserDetails userDetails = this.memberService.loadUserByUsername(this.getUsername(jwt));
        return new UsernamePasswordAuthenticationToken(userDetails, ""
                , userDetails.getAuthorities());    // 스프링에서 지원하는 토큰(사용자정보, 사용자권한정보를 포함하게 됨)
    }

    /**
     * 토큰에서 로그인한 유저의 아이디 추출
     */
    public String getUsername(String token) {
        return this.parseClaims(token).getSubject();
    }

    /**
     * 토큰 유효성 검사
     */
    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) return false;  // token 이 존재하지 않을경우

        var claims = this.parseClaims(token);
        return !claims.getExpiration().before(new Date());   // 만료시간이 현재시간보다 전일경우(즉, 지났을경우)
    }

    /**
     * 토큰에서 Claim 정보 추출
     */
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser().setSigningKey(this.secretKey).parseClaimsJws(token).getBody(); // Claim 정보 반환
        } catch(ExpiredJwtException e) {    // 만료시간이 지난경우 ExpiredJwtException 이 발생
            // TODO
            return e.getClaims();
        }
    }

}
