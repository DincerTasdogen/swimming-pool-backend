package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.model.entity.User;
import com.sp.SwimmingPool.model.enums.UserRoleEnum;
import com.sp.SwimmingPool.repos.MemberRepository;
import com.sp.SwimmingPool.repos.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CoachAssignmentService {

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;

    public Integer findLeastLoadedCoachId() {
        // Get all coaches
        List<User> coaches = userRepository.findAll()
                .stream()
                .filter(user -> user.getRole() == UserRoleEnum.COACH)
                .toList();

        if (coaches.isEmpty()) {
            throw new IllegalStateException("No coaches available for assignment.");
        }

        // Find the coach with the fewest members
        return coaches.stream()
                .min(Comparator.comparingInt(
                        coach -> memberRepository.findByCoachId(coach.getId()).size()
                ))
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("No coaches found."));
    }
}
