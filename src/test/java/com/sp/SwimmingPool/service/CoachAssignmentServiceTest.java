package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.User;
import com.sp.SwimmingPool.model.enums.UserRoleEnum;
import com.sp.SwimmingPool.repos.MemberRepository;
import com.sp.SwimmingPool.repos.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoachAssignmentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CoachAssignmentService coachAssignmentService;

    private User coach1;
    private User coach2;
    private User coach3;
    private User notACoach;

    @BeforeEach
    void setUp() {
        coach1 = new User();
        coach1.setId(1);
        coach1.setName("Coach");
        coach1.setSurname("One");
        coach1.setEmail("coach1@example.com");
        coach1.setRole(UserRoleEnum.COACH);

        coach2 = new User();
        coach2.setId(2);
        coach2.setName("Coach");
        coach2.setSurname("Two");
        coach2.setEmail("coach2@example.com");
        coach2.setRole(UserRoleEnum.COACH);

        coach3 = new User();
        coach3.setId(3);
        coach3.setName("Coach");
        coach3.setSurname("Three");
        coach3.setEmail("coach3@example.com");
        coach3.setRole(UserRoleEnum.COACH);

        notACoach = new User();
        notACoach.setId(4);
        notACoach.setName("Admin");
        notACoach.setSurname("User");
        notACoach.setEmail("admin@example.com");
        notACoach.setRole(UserRoleEnum.ADMIN);
    }

    @Test
    void findLeastLoadedCoachId_noCoachesAvailable_throwsIllegalStateException() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            coachAssignmentService.findLeastLoadedCoachId();
        });
        assertEquals("No coaches available for assignment.", exception.getMessage());
    }

    @Test
    void findLeastLoadedCoachId_onlyNonCoachUsers_throwsIllegalStateException() {
        when(userRepository.findAll()).thenReturn(List.of(notACoach));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> coachAssignmentService.findLeastLoadedCoachId());
        assertEquals("No coaches available for assignment.", exception.getMessage());
    }

    @Test
    void findLeastLoadedCoachId_singleCoach_returnsCoachId() {
        when(userRepository.findAll()).thenReturn(List.of(coach1, notACoach));

        Integer leastLoadedCoachId = coachAssignmentService.findLeastLoadedCoachId();

        assertNotNull(leastLoadedCoachId);
        assertEquals(coach1.getId(), leastLoadedCoachId);
        verify(memberRepository, never()).findByCoachId(coach1.getId());
    }

    @Test
    void findLeastLoadedCoachId_multipleCoaches_returnsIdOfLeastLoaded() {
        when(userRepository.findAll()).thenReturn(List.of(coach1, coach2, coach3, notACoach));

        // coach1 has 2 members
        when(memberRepository.findByCoachId(coach1.getId())).thenReturn(List.of(new Member(), new Member()));
        // coach2 has 1 member (least loaded)
        when(memberRepository.findByCoachId(coach2.getId())).thenReturn(List.of(new Member()));
        // coach3 has 3 members
        when(memberRepository.findByCoachId(coach3.getId())).thenReturn(List.of(new Member(), new Member(), new Member()));

        Integer leastLoadedCoachId = coachAssignmentService.findLeastLoadedCoachId();

        assertNotNull(leastLoadedCoachId);
        assertEquals(coach2.getId(), leastLoadedCoachId);
    }

    @Test
    void findLeastLoadedCoachId_multipleCoachesWithEqualLoad_returnsOneOfThem() {
        when(userRepository.findAll()).thenReturn(List.of(coach1, coach2));

        // Both coaches have 1 member
        when(memberRepository.findByCoachId(coach1.getId())).thenReturn(List.of(new Member()));
        when(memberRepository.findByCoachId(coach2.getId())).thenReturn(List.of(new Member()));

        Integer leastLoadedCoachId = coachAssignmentService.findLeastLoadedCoachId();

        assertNotNull(leastLoadedCoachId);
        // The specific one returned depends on the stream's internal ordering for min when values are equal.
        // We can assert it's one of the expected IDs.
        assertTrue(leastLoadedCoachId == coach1.getId() || leastLoadedCoachId == coach2.getId());
    }

    @Test
    void findLeastLoadedCoachId_oneCoachWithZeroMembers_othersWithMembers_returnsZeroMemberCoach() {
        when(userRepository.findAll()).thenReturn(List.of(coach1, coach2, coach3));

        // coach1 has 2 members
        when(memberRepository.findByCoachId(coach1.getId())).thenReturn(List.of(new Member(), new Member()));
        // coach2 has 0 members (least loaded)
        when(memberRepository.findByCoachId(coach2.getId())).thenReturn(Collections.emptyList());
        // coach3 has 3 members
        when(memberRepository.findByCoachId(coach3.getId())).thenReturn(List.of(new Member(), new Member(), new Member()));

        Integer leastLoadedCoachId = coachAssignmentService.findLeastLoadedCoachId();

        assertNotNull(leastLoadedCoachId);
        assertEquals(coach2.getId(), leastLoadedCoachId);
    }
}