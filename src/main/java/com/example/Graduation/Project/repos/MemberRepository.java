package com.example.Graduation.Project.repos;

import com.example.Graduation.Project.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Integer> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByIdentityNumber(String identityNumber);
}
