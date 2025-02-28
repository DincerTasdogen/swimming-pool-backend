package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.MemberDTO;
import com.sp.SwimmingPool.dto.UserDTO;
import com.sp.SwimmingPool.service.MemberService;
import com.sp.SwimmingPool.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:3000")
public class ProfileController {

    private final MemberService memberService;
    private final UserService userService;

    public ProfileController(MemberService memberService, UserService userService) {
        this.memberService = memberService;
        this.userService = userService;
    }

    @GetMapping("/member/{id}")
    public ResponseEntity<?> getMemberProfileDetails(@PathVariable int id) {
        MemberDTO member = memberService.getMemberDetails(id);
        if (member != null) {
            return ResponseEntity.ok(member);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/staff/{id}")
    public ResponseEntity<?> getStaffProfileDetails(@PathVariable int id) {
        UserDTO user = userService.getUserDetails(id);
        if (user != null) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/staff/{id}")
    public ResponseEntity<?> updateStaffProfileDetails(@PathVariable int id, @RequestBody UserDTO userDTO) {
        try {
            userService.updateUser(id, userDTO);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().build();
    }

    @PutMapping("/member/{id}")
    public ResponseEntity<?> updateMemberProfileDetails(@PathVariable int id, @RequestBody MemberDTO memberDTO) {
        try {
            memberService.updateMember(id, memberDTO);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().build();
    }
}