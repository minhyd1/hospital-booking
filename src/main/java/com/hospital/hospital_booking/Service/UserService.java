package com.hospital.hospital_booking.Service;

import com.hospital.hospital_booking.DTO.DoctorResponseDTO;
import com.hospital.hospital_booking.DTO.PageResponseDTO;
import com.hospital.hospital_booking.DTO.RegisterRequestDTO;
import com.hospital.hospital_booking.DTO.UserResponseDTO;
import com.hospital.hospital_booking.Entity.User;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    // 1. Dành cho Bệnh nhân đăng ký tài khoản mới
    User registerPatient(RegisterRequestDTO request);

    // 2. Dành cho màn hình Đăng nhập (Tạm thời check cơ bản)
    User login(String email, String password);

    PageResponseDTO<? extends UserResponseDTO> getUsersByRole(String roleName, Pageable pageable);

    List<DoctorResponseDTO> getDoctorsBySpecialty(Long specialtyId);

    PageResponseDTO<UserResponseDTO> getAllUsers(Pageable pageable);

}
