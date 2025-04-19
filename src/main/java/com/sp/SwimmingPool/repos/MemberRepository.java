package com.sp.SwimmingPool.repos;

import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Integer> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByIdentityNumber(String identityNumber);
    List<Member> findByCoachId(Integer coachId);
    List<Member> findByStatus(StatusEnum status);

}
