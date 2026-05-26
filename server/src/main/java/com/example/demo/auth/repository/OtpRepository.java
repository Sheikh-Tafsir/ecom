package com.example.demo.auth.repository;

import com.example.demo.common.enums.OtpType;
import com.example.demo.common.model.User;
import com.example.demo.common.model.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {
    Otp findByTypeAndUser_Email(OtpType type, String email);

    Optional<Otp> findByUserAndType(User user, OtpType type);
}
