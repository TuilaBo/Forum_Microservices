package com.khoavdse170395.commentservice.security;

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
 */
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = defaultConverter.convert(jwt);
        Collection<GrantedAuthority> keycloakRoles = extractKeycloakRoles(jwt);
        
        Set<GrantedAuthority> allAuthorities = Stream.concat(
                authorities.stream(),
                keycloakRoles.stream()
        ).collect(Collectors.toSet());

        return new JwtAuthenticationToken(jwt, allAuthorities);
    }

    private Collection<GrantedAuthority> extractKeycloakRoles(Jwt jwt) {
        Set<GrantedAuthority> roles = new java.util.HashSet<>();

        // Extract realm roles
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

        // Extract client roles
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
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

    private String ensureRolePrefix(String role) {
        if (role == null || role.isEmpty()) {
            return role;
        }
        
        if (role.startsWith("ROLE_")) {
            return role;
        }
        
        return "ROLE_" + role;
    }
}
