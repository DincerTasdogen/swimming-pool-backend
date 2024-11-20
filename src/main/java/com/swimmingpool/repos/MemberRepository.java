package com.swimmingpool.repos;

import com.swimmingpool.model.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Integer> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByIdentityNumber(String identityNumber);
}
