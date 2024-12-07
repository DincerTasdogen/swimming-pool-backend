package com.sp.SwimmingPool;

import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.entity.User;
import com.sp.SwimmingPool.model.enums.MemberGenderEnum;
import com.sp.SwimmingPool.model.enums.StatusEnum;
import com.sp.SwimmingPool.model.enums.UserRoleEnum;
import com.sp.SwimmingPool.repos.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@SpringBootTest
public class MemberCreationTests {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Test
    public void saveMemberToDB() {
        String memberEmail = "member@member.com";

        String password = "member123";  

        if(memberRepository.findByEmail(memberEmail).isEmpty()) {
            Member member = Member.builder()
                    .identityNumber("11111111111")
                    .email(memberEmail)
                    .password(passwordEncoder.encode(password))
                    .name("Member")
                    .surname("Membran")
                    .birthDate(LocalDate.of(2024, 11, 25))
                    .gender(MemberGenderEnum.FEMALE)
                    .weight(100.0)
                    .height(180.5)
                    .phoneNumber("+905555555555")
                    .idPhotoBack("IMGPATH")
                    .idPhotoFront("IMGPATH")
                    .photo("IMGPATH")
                    .canSwim(false)
                    .coachId(1)
                    .status(StatusEnum.ACTIVE)
                    .photoVerified(true)
                    .idVerified(true)
                    .registrationDate(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            memberRepository.save(member);
            System.out.println("Member created successfully!");
            System.out.println("Email = " + member.getEmail());
            System.out.println("Password = " + password);
        } else {
            System.out.println("Member Already Exists");
        }
    }
}
