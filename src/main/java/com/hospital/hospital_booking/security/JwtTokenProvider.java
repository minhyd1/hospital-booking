package com.hospital.hospital_booking.security;

import jakarta.annotation.PostConstruct;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationInMs}")
    private long jwtExpirationInMs;

    private Key signingKey;

    @PostConstruct
    void init() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("Missing required property: app.jwtSecret");
        }
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private Key getSigningKey() {
        return signingKey;
    }

    // 1. Sinh ra Token từ Email (hoặc ID) của User
    public String generateToken(String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(email) // Đưa email vào trong payload của thẻ
                .claim("role", role)
                .setIssuedAt(new Date()) // Ngày cấp
                .setExpiration(expiryDate) // Ngày hết hạn
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // Đóng dấu bảo mật
                .compact();
    }

    public String getRoleFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("role", String.class);
    }

    // 2. Lấy Email ngược ra từ Token
    public String getEmailFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    // 3. Kiểm tra xem Token có hợp lệ không (Có bị hết hạn, bị sửa đổi, hay làm giả không?)
    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (SecurityException | MalformedJwtException ex) {
            System.out.println("Lỗi: Token không đúng định dạng!");
        } catch (ExpiredJwtException ex) {
            System.out.println("Lỗi: Token đã hết hạn!");
        } catch (UnsupportedJwtException ex) {
            System.out.println("Lỗi: Token không được hỗ trợ!");
        } catch (IllegalArgumentException ex) {
            System.out.println("Lỗi: JWT claims string is empty.");
        }
        return false;
    }
}
