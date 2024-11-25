package com.sp.SwimmingPool;

import com.sp.SwimmingPool.model.entity.User;
import com.sp.SwimmingPool.model.enums.UserRoleEnum;
import com.sp.SwimmingPool.repos.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@SpringBootTest
public class UserCreationTests {


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
	@Test
	public void saveCoachToDB() {

        String password = "coach123";
        if(userRepository.findByEmail("coach@swim.com").isEmpty()) {
            User coach = User.builder()
                    .email("coach@swim.com")
                    .password(passwordEncoder.encode(password))
                    .name("Coach")
                    .surname("Coacher")
                    .role(UserRoleEnum.COACH)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .memberCount(0)
                    .build();
            userRepository.save(coach);
            System.out.println("Coach created successfully!");
            System.out.println("Email = " + coach.getEmail());
            System.out.println("Password = " + password);
        } else {
            System.out.println("Coach User Already Exists");
        }
	}

    @Test
    public void saveDoctorToDB() {

        String password = "doctor123";

        if(userRepository.findByEmail("doctor@swim.com").isEmpty()) {
            User doctor = User.builder()
                    .email("doctor@swim.com")
                    .password(passwordEncoder.encode(password))
                    .name("Doctor")
                    .surname("Doctor")
                    .role(UserRoleEnum.DOCTOR)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .memberCount(0)
                    .build();
            userRepository.save(doctor);
            System.out.println("Doctor created successfully!");
            System.out.println("Email = " + doctor.getEmail());
            System.out.println("Password = " + password);
        } else {
            System.out.println("Doctor User Already Exists");
        }
    }
}
