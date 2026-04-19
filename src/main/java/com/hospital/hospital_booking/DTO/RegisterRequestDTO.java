package com.hospital.hospital_booking.DTO;

import lombok.Data;

@Data
public class RegisterRequestDTO {
    private String email;
    private String password;
    private String fullName;
    private String phone;
}
