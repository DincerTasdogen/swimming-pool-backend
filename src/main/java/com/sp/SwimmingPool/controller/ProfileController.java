package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.MemberDTO;
import com.sp.SwimmingPool.dto.UserDTO;
import com.sp.SwimmingPool.service.MemberService;
import com.sp.SwimmingPool.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final MemberService memberService;
    private final UserService userService;

    public ProfileController(MemberService memberService, UserService userService) {
        this.memberService = memberService;
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProfileDetails(@PathVariable int id) {
        MemberDTO member = memberService.getMemberDetails(id);
        if (member != null) {
            return ResponseEntity.ok(member);
        }

        UserDTO user = userService.getUserDetails(id);
        if (user != null) {
            return ResponseEntity.ok(user);
        }

        return ResponseEntity.notFound().build();
    }
}