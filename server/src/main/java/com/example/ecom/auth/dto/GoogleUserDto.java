package com.example.ecom.auth.dto;

import lombok.Data;

@Data
public class GoogleUserDto {

    private String email;

    private boolean verified_email;

    private String name;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isVerified_email() { return verified_email; }
    public void setVerified_email(boolean verified_email) { this.verified_email = verified_email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
