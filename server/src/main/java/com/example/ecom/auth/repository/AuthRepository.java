package com.example.ecom.auth.repository;

import com.example.ecom.common.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);
}
