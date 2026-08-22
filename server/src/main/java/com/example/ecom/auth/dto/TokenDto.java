package com.example.ecom.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenDto {

    private String accessToken;

    private String refreshToken;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public static TokenDtoBuilder builder() {
        return new TokenDtoBuilder();
    }

    public static class TokenDtoBuilder {
        private String accessToken;
        private String refreshToken;

        public TokenDtoBuilder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public TokenDtoBuilder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public TokenDto build() {
            return new TokenDto(accessToken, refreshToken);
        }
    }
}
