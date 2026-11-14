package com.example.ecom.auth.dto;

import com.example.ecom.auth.enums.OtpType;
import lombok.*;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Otp implements Serializable {

    private String email;

    private OtpType type;

    private int value;

    private int tries;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public OtpType getType() { return type; }
    public void setType(OtpType type) { this.type = type; }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
    public int getTries() { return tries; }
    public void setTries(int tries) { this.tries = tries; }
}
