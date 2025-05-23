package com.sp.SwimmingPool.repos;

import com.sp.SwimmingPool.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPasswordResetToken(String token);
}
