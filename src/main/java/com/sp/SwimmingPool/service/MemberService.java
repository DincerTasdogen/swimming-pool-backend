package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.MemberDTO;
import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.model.enums.MemberGenderEnum;
import com.sp.SwimmingPool.repos.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MemberService {

    @Autowired
    private MemberRepository memberRepository;

    public MemberDTO createMember(MemberDTO memberDTO) {
        Member member = new Member();
        member.setName(memberDTO.getName());
        member.setSurname(memberDTO.getSurname());
        member.setEmail(memberDTO.getEmail());
        member.setPassword(memberDTO.getPassword());
        member.setIdentityNumber(memberDTO.getIdentityNumber());
        member.setGender(MemberGenderEnum.valueOf(memberDTO.getGender().toUpperCase()));
        member.setWeight(memberDTO.getWeight());
        member.setHeight(memberDTO.getHeight());
        member.setPhoneNumber(memberDTO.getPhoneNumber());
        member.setIdPhotoFront(memberDTO.getIdPhotoFront());
        member.setIdPhotoBack(memberDTO.getIdPhotoBack());
        member.setPhoto(memberDTO.getPhoto());
        member.setCanSwim(memberDTO.isCanSwim());

        memberRepository.save(member);
        return memberDTO;
    }

    public MemberDTO updateMember(int id, MemberDTO memberDTO) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + id));
        member.setName(memberDTO.getName());
        member.setSurname(memberDTO.getSurname());
        member.setEmail(memberDTO.getEmail());
        member.setPassword(memberDTO.getPassword());
        member.setIdentityNumber(memberDTO.getIdentityNumber());
        member.setGender(MemberGenderEnum.valueOf(memberDTO.getGender().toUpperCase()));
        member.setWeight(memberDTO.getWeight());
        member.setHeight(memberDTO.getHeight());
        member.setPhoneNumber(memberDTO.getPhoneNumber());
        member.setIdPhotoFront(memberDTO.getIdPhotoFront());
        member.setIdPhotoBack(memberDTO.getIdPhotoBack());
        member.setPhoto(memberDTO.getPhoto());
        member.setCanSwim(memberDTO.isCanSwim());

        memberRepository.save(member);
        return memberDTO;
    }

    public void deleteMember(int id){
        if (memberRepository.existsById(id)) {
            memberRepository.deleteById(id);
        } else {
            throw new RuntimeException("Member with id " + id + " not found");
        }

    }
    public MemberDTO getMemberDetails(int id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + id));
        MemberDTO memberDTO = new MemberDTO();
        memberDTO.setId(member.getId());
        memberDTO.setName(member.getName());
        memberDTO.setSurname(member.getSurname());
        memberDTO.setEmail(member.getEmail());
        memberDTO.setIdentityNumber(member.getIdentityNumber());
        memberDTO.setGender(member.getGender().name());
        memberDTO.setWeight(member.getWeight());
        memberDTO.setHeight(member.getHeight());
        memberDTO.setPhoneNumber(member.getPhoneNumber());
        memberDTO.setIdPhotoFront(member.getIdPhotoFront());
        memberDTO.setIdPhotoBack(member.getIdPhotoBack());
        memberDTO.setPhoto(member.getPhoto());
        memberDTO.setCanSwim(member.isCanSwim());

        return memberDTO;
    }
    public List<MemberDTO> listAllMembers() {
        List<Member> members = memberRepository.findAll();
        List<MemberDTO> memberDTOs = new ArrayList<>();
        for (Member member : members) {
            MemberDTO memberDTO = new MemberDTO();
            memberDTO.setId(member.getId());
            memberDTO.setName(member.getName());
            memberDTO.setSurname(member.getSurname());
            memberDTO.setEmail(member.getEmail());
            memberDTO.setIdentityNumber(member.getIdentityNumber());
            memberDTO.setGender(member.getGender().name());
            memberDTO.setWeight(member.getWeight());
            memberDTO.setHeight(member.getHeight());
            memberDTO.setPhoneNumber(member.getPhoneNumber());
            memberDTO.setIdPhotoFront(member.getIdPhotoFront());
            memberDTO.setIdPhotoBack(member.getIdPhotoBack());
            memberDTO.setPhoto(member.getPhoto());
            memberDTO.setCanSwim(member.isCanSwim());
            memberDTOs.add(memberDTO);
        }
        return memberDTOs;
    }
    }


