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
        String memberEmail = "member2@member.com"; //"member@member.com";

        String password = "member123456";//"member123";

        if(memberRepository.findByEmail(memberEmail).isEmpty()) {
            Member member = Member.builder()
                    .identityNumber("11111111110")
                    .email(memberEmail)
                    .password(passwordEncoder.encode(password))
                    .name("Member2")
                    .surname("member123456")
                    .birthDate(LocalDate.of(2000, 1, 1))
                    .gender(MemberGenderEnum.MALE)
                    .weight(62.0)
                    .height(165.0)
                    .phoneNumber("+905000000000")
                    .idPhotoBack("IMGPATH")
                    .idPhotoFront("IMGPATH")
                    .photo("https://cdn-icons-png.flaticon.com/512/5556/5556468.png")
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
