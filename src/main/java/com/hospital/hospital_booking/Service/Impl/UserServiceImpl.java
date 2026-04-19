package com.hospital.hospital_booking.Service.Impl;

import com.hospital.hospital_booking.DTO.*;
import com.hospital.hospital_booking.Entity.DoctorDetail;
import com.hospital.hospital_booking.Entity.Role;
import com.hospital.hospital_booking.Entity.User;
import com.hospital.hospital_booking.Repository.DoctorDetailRepository;
import com.hospital.hospital_booking.Repository.UserRepository;
import com.hospital.hospital_booking.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final DoctorDetailRepository doctorDetailRepository;

    @Override
    public User registerPatient(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email này đã được sử dụng!");
        }
        User newPatient = new User();
        newPatient.setEmail(request.getEmail());
        newPatient.setPassword(request.getPassword());

        newPatient.setFullName(request.getFullName());
        newPatient.setPhone(request.getPhone());
        newPatient.setRole(Role.PATIENT);

        return userRepository.save(newPatient);
    }

    @Override
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Sai mật khẩu!");
        }

        return user;
    }

    @Override
    public List<DoctorResponseDTO> getDoctorsBySpecialty(Long specialtyId) {

        // 1. Đã xóa orElseThrow vì đây là List
        List<DoctorDetail> doctors = doctorDetailRepository.findBySpecialtyId(specialtyId);

        return doctors.stream().map(detail -> DoctorResponseDTO.builder()
                // 2. Đổi doctorId thành id (thừa hưởng từ class cha UserResponseDTO)
                .id(detail.getUser().getId())
                .fullName("Bác sĩ " + detail.getUser().getFullName())
                .email(detail.getUser().getEmail())

                // Đừng quên 2 trường này cũng được thừa hưởng từ class cha nhé:
                .phone(detail.getUser().getPhone())
                .role(detail.getUser().getRole().name())

                // Các trường riêng của Bác sĩ:
                .specialtyName(detail.getSpecialty().getName())
                .consultationFee(detail.getConsultationFee())
                .bio(detail.getBio())
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    public List<? extends UserResponseDTO> getUsersByRole(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            List<User> allUsers = userRepository.findAll();
            // Trả về danh sách cơ bản cho tất cả mọi người
            return allUsers.stream().map(user -> UserResponseDTO.builder()
                    .id(user.getId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .role(user.getRole().name())
                    .build()
            ).collect(Collectors.toList());
        }
        Role roleEnum;
        try {
            roleEnum = Role.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Quyền không hợp lệ! Chỉ nhận PATIENT, DOCTOR, ADMIN.");
        }

        // 1. Lấy danh sách từ Database
        List<User> users = userRepository.findByRole(roleEnum);

        // 2. Dùng Switch-Case để "biến hình" Entity thành DTO tương ứng
        switch (roleEnum) {
            case DOCTOR:
                // Nếu là Bác sĩ -> Map sang DoctorResponseDTO
                return users.stream().map(user -> {
                    DoctorResponseDTO dto = DoctorResponseDTO.builder()
                            .id(user.getId())
                            .fullName("Bác sĩ " + user.getFullName())
                            .email(user.getEmail())
                            .phone(user.getPhone())
                            .role(user.getRole().name())
                            .build();

                    // Tránh lỗi NullPointerException nếu bác sĩ chưa có chi tiết hồ sơ
                    if (user.getDoctorDetail() != null) {
                        dto.setSpecialtyName(user.getDoctorDetail().getSpecialty().getName());
                        dto.setConsultationFee(user.getDoctorDetail().getConsultationFee());
                        dto.setBio(user.getDoctorDetail().getBio());
                    }
                    return dto;
                }).collect(Collectors.toList());

            case PATIENT:
                // Nếu là Bệnh nhân -> Map sang PatientResponseDTO
                return users.stream().map(user -> PatientResponseDTO.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole().name())
                        // Giả sử sau này bạn thêm trường địa chỉ vào bảng User
                        // .address(user.getAddress())
                        .build()
                ).collect(Collectors.toList());

            case ADMIN:
                // Nếu là Admin -> Map sang AdminResponseDTO
                return users.stream().map(user -> AdminResponseDTO.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole().name())
                        .adminLevel("SUPER_ADMIN") // Giả lập dữ liệu
                        .build()
                ).collect(Collectors.toList());

            default:
                // Mặc định trả về UserResponseDTO cơ bản
                return users.stream().map(user -> UserResponseDTO.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole().name())
                        .build()
                ).collect(Collectors.toList());
        }
    }
}
