package com.giit.campusbite;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final StudentUserRepository users;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   StudentUserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            Map<String, Object> claims = jwtService.validateAndRead(
                    authorization.substring(7));
            if (claims != null) {
                String email = (String) claims.get("sub");
                Object tokenVersion = claims.getOrDefault("ver", 0);
                StudentUser user = users.findByEmailIgnoreCase(email).orElse(null);
                if (user != null && tokenVersion instanceof Number version
                        && version.intValue() == user.getTokenVersion()) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            user.getEmail(),
                            null,
                            List.of(new SimpleGrantedAuthority(
                                    "ROLE_" + user.getRole())));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
