package com.example.demo.auth.dto;

import com.example.demo.auth.enums.OtpType;
import lombok.*;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class Otp implements Serializable {

    private String email;

    private OtpType type;

    private int value;

    private int tries;
}
