package com.restaurant.reclamations.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignClientInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_TYPE = "Bearer";

    @Override
    public void apply(RequestTemplate template) {
        // First try to get the token from the current request
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            String authHeader = requestAttributes.getRequest().getHeader(AUTHORIZATION_HEADER);
            if (authHeader != null && authHeader.startsWith(TOKEN_TYPE)) {
                template.header(AUTHORIZATION_HEADER, authHeader);
                return;
            }
        }

        // Fallback to getting the token from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof String) {
            String token = (String) authentication.getCredentials();
            template.header(AUTHORIZATION_HEADER, TOKEN_TYPE + " " + token);
        }
    }
}