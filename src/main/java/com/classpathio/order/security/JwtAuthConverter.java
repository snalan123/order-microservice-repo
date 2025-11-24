package com.classpathio.order.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.*;

public class JwtAuthConverter {

    public AbstractAuthenticationToken convert(Jwt jwt) {

        Set<GrantedAuthority> authorities = new HashSet<>();

       List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        if (groups != null) {
            groups.forEach(g ->
                authorities.add(new SimpleGrantedAuthority("ROLE_" + g.toUpperCase()))
            );
        }

        authorities.addAll(ScopeExtractor.extract(jwt));

        System.out.println("\n===== JWT PAYLOAD =====");
        System.out.println(jwt.getClaims());
        System.out.println("========================\n");

        System.out.println("\n===== AUTHORITIES =====");
        System.out.println(authorities);
        System.out.println("========================\n");

        return new JwtAuthenticationToken(jwt, authorities);
    }
}
