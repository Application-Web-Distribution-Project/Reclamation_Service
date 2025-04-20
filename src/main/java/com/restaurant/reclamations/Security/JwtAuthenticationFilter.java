package com.restaurant.reclamations.Security;

import com.restaurant.reclamations.Services.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            String token = extractToken(request);
            if (StringUtils.hasText(token)) {
                Map<String, Object> userData = authService.validateToken(token);
                
                if (userData != null && userData.containsKey("userId") && userData.containsKey("role")) {
                    String userId = (String) userData.get("userId");
                    String role = (String) userData.get("role");
                    
                    // Préfixer le rôle avec ROLE_ si ce n'est pas déjà le cas (exigé par Spring Security)
                    String securityRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    
                    // Créer un objet Authentication avec l'ID utilisateur et le rôle
                    var authentication = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority(securityRole))
                    );
                    
                    // Stocker dans le contexte de sécurité
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("Utilisateur authentifié: {} avec rôle {}", userId, securityRole);
                }
            }
        } catch (Exception e) {
            log.error("Erreur d'authentification JWT: {}", e.getMessage());
            // Ne pas lancer d'exception pour permettre à la chaîne de filtres de continuer
        }
        
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}