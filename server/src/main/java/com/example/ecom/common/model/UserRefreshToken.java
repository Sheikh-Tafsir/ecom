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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    private String jti;

    @Enumerated(EnumType.STRING)
    private UserRefreshTokenStatus status = UserRefreshTokenStatus.ACTIVE;

    public boolean isInvalid() {
        return status == UserRefreshTokenStatus.REVOKED;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }
    public UserRefreshTokenStatus getStatus() { return status; }
    public void setStatus(UserRefreshTokenStatus status) { this.status = status; }
}
