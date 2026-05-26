package com.example.demo.common.model;

import com.example.demo.common.enums.OtpType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "spring_user_otp",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "value"})}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Otp {

    private static final Duration validity = Duration.ofMinutes(5);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private OtpType type;

    @OneToOne
    @JoinColumn(nullable = false)
    private User user;

    private int value;

    private LocalDateTime expiresAt;

    @PrePersist
    @PreUpdate
    private void calculateExpiry() {
        this.expiresAt = LocalDateTime.now().plus(validity);
    }
}
