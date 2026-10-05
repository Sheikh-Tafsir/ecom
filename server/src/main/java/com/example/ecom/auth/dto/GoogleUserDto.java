package com.example.ecom.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Collection;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleUserDto {

    @JsonAlias({"sub", "user_id", "id"})
    private String sub;

    private String email;

    @JsonAlias({"verified_email", "email_verified"})
    private boolean emailVerified;

    private String name;

    @JsonAlias({"aud", "audience", "issued_to"})
    private String audience;

    @JsonAlias({"azp", "authorized_party"})
    private String azp;

    public boolean matchesAudience(Collection<String> allowedClientIds) {
        if (allowedClientIds == null || allowedClientIds.isEmpty()) {
            return false;
        }

        return (audience != null && allowedClientIds.contains(audience.trim()))
                || (azp != null && allowedClientIds.contains(azp.trim()));
    }
}
