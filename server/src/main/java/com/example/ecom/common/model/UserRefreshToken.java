package com.example.ecom.common.model;

import com.example.ecom.common.enums.UserRefreshTokenStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_refresh_tokens", uniqueConstraints = {@UniqueConstraint(columnNames = {"jti"})})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserRefreshToken extends BaseEntity {

    @ManyToOne
    private User user;

    private String jti;

    @Enumerated(EnumType.STRING)
    private UserRefreshTokenStatus status = UserRefreshTokenStatus.ACTIVE;

    public boolean isInvalid() {
        return status == UserRefreshTokenStatus.REVOKED;
    }
}
