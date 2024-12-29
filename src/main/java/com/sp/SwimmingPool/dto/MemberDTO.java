package com.sp.SwimmingPool.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberDTO {
    private int id;
    private String name;
    private String surname;
    private String email;
    private String identityNumber;
    private String gender;
    private double weight;
    private double height;
    private String phoneNumber;
    private String idPhotoFront;
    private String idPhotoBack;
    private String photo;
    private boolean canSwim;
}
