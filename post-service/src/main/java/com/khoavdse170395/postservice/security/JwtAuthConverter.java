package com.khoavdse170395.postservice.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Custom JWT Authentication Converter để map roles từ Keycloak sang Spring Security.
 * 
 * Vấn đề: Keycloak trả về roles như "STUDENT", "TEACHER"
 * Spring Security cần: "ROLE_STUDENT", "ROLE_TEACHER" (có prefix ROLE_)
 * 
 * Giải pháp: Extract roles từ realm_access.roles và resource_access trong JWT,
 * thêm prefix ROLE_ vào mỗi role, convert thành GrantedAuthority.
 */
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // Lấy authorities từ scope (nếu có) - default behavior
        Collection<GrantedAuthority> authorities = defaultConverter.convert(jwt);
        
        // Extract roles từ Keycloak token và thêm vào authorities
        Collection<GrantedAuthority> keycloakRoles = extractKeycloakRoles(jwt);
        
        // Merge cả 2 collections
        Set<GrantedAuthority> allAuthorities = Stream.concat(
                authorities.stream(),
                keycloakRoles.stream()
        ).collect(Collectors.toSet());

        return new JwtAuthenticationToken(jwt, allAuthorities);
    }

    /**
     * Extract roles từ Keycloak JWT token.
     * Keycloak trả về roles trong 2 places:
     * 1. realm_access.roles - realm roles (ROLE_STUDENT, ROLE_TEACHER...)
     * 2. resource_access.<client_id>.roles - client roles (nếu có)
     */
    private Collection<GrantedAuthority> extractKeycloakRoles(Jwt jwt) {
        Set<GrantedAuthority> roles = new java.util.HashSet<>();

        // 1. Extract realm roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            @SuppressWarnings("unchecked")
            Collection<String> realmRoles = (Collection<String>) realmAccess.get("roles");
            if (realmRoles != null) {
                roles.addAll(realmRoles.stream()
                        .map(role -> new SimpleGrantedAuthority(ensureRolePrefix(role)))
                        .collect(Collectors.toSet()));
            }
        }

        // 2. Extract client roles (resource_access.<client_id>.roles)
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            // Lặp qua tất cả clients trong resource_access
            for (Map.Entry<String, Object> clientEntry : resourceAccess.entrySet()) {
                Object clientData = clientEntry.getValue();
                if (clientData instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> clientMap = (Map<String, Object>) clientData;
                    Object rolesObj = clientMap.get("roles");
                    if (rolesObj instanceof Collection) {
                        @SuppressWarnings("unchecked")
                        Collection<String> clientRoles = (Collection<String>) rolesObj;
                        roles.addAll(clientRoles.stream()
                                .map(role -> new SimpleGrantedAuthority(ensureRolePrefix(role)))
                                .collect(Collectors.toSet()));
                    }
                }
            }
        }

        return roles;
    }

    /**
     * Đảm bảo role có prefix "ROLE_" (Spring Security convention).
     * Nếu role đã có prefix ROLE_ thì giữ nguyên, nếu chưa thì thêm vào.
     * 
     * Ví dụ:
     * - "STUDENT" → "ROLE_STUDENT"
     * - "ROLE_STUDENT" → "ROLE_STUDENT" (giữ nguyên)
     */
    private String ensureRolePrefix(String role) {
        if (role == null || role.isEmpty()) {
            return role;
        }
        
        // Nếu đã có prefix ROLE_ thì giữ nguyên
        if (role.startsWith("ROLE_")) {
            return role;
        }
        
        // Nếu chưa có thì thêm prefix
        return "ROLE_" + role;
    }
}
