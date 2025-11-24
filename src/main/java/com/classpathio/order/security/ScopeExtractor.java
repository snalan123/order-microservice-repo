package com.classpathio.order.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;

public class ScopeExtractor {

    public static Collection<SimpleGrantedAuthority> extract(Jwt jwt) {

        Set<SimpleGrantedAuthority> authorities = new HashSet<>();

        String scopeString = jwt.getClaimAsString("scope");
        if (scopeString != null) {

            for (String scope : scopeString.split(" ")) {

                if (scope.startsWith("https://api.ekart.com/orders/")) {
                    String simple = scope.substring(scope.lastIndexOf("/") + 1);  // ex: reader
                    authorities.add(new SimpleGrantedAuthority("SCOPE_orders." + simple));
                }
            }
        }
        return authorities;
    }
}
