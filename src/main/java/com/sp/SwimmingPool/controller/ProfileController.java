package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.MemberDTO;
import com.sp.SwimmingPool.dto.UserDTO;
import com.sp.SwimmingPool.service.MemberService;
import com.sp.SwimmingPool.service.StorageService;
import com.sp.SwimmingPool.service.UserService;
import com.sp.SwimmingPool.util.FilePathUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final MemberService memberService;
    private final UserService userService;
    private final StorageService storageService;
    private final FilePathUtil filePathUtil;

    public ProfileController(
            MemberService memberService,
            UserService userService,
            StorageService storageService,
            FilePathUtil filePathUtil) {
        this.memberService = memberService;
        this.userService = userService;
        this.storageService = storageService;
        this.filePathUtil = filePathUtil;
    }

    @GetMapping("/member/{id}")
    public ResponseEntity<?> getMemberProfileDetails(@PathVariable int id) {
        MemberDTO member = memberService.getMemberDetails(id);
        if (member != null) {
            // Convert all file paths to proper URLs
            member.setPhoto(filePathUtil.getFileUrl(member.getPhoto()));
            member.setIdPhotoFront(filePathUtil.getFileUrl(member.getIdPhotoFront()));
            member.setIdPhotoBack(filePathUtil.getFileUrl(member.getIdPhotoBack()));
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
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/member/{id}")
    public ResponseEntity<?> updateMemberProfileDetails(@PathVariable int id, @RequestBody MemberDTO memberDTO) {
        try {
            memberService.updateMember(id, memberDTO);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/member/{id}/photo")
    public ResponseEntity<?> uploadMemberPhoto(
            @PathVariable int id,
            @RequestParam("photo") MultipartFile photo) {

        try {
            // Store the file
            String photoPath = storageService.storeFile(photo, "members/photos");

            // Update the member record
            MemberDTO memberDTO = new MemberDTO();
            memberDTO.setPhoto(photoPath);
            memberService.updateMember(id, memberDTO);

            // Return the new URL
            Map<String, String> response = new HashMap<>();
            response.put("photoUrl", filePathUtil.getFileUrl(photoPath));

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to upload photo: " + e.getMessage());
        }
    }
}