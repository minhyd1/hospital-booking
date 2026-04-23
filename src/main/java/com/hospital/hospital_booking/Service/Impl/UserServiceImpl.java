package com.hospital.hospital_booking.Service.Impl;

import com.hospital.hospital_booking.DTO.*;
import com.hospital.hospital_booking.Entity.DoctorDetail;
import com.hospital.hospital_booking.Entity.Role;
import com.hospital.hospital_booking.Entity.User;
import com.hospital.hospital_booking.Repository.DoctorDetailRepository;
import com.hospital.hospital_booking.Repository.UserRepository;
import com.hospital.hospital_booking.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final DoctorDetailRepository doctorDetailRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User registerPatient(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email này đã được sử dụng!");
        }
        User newPatient = new User();
        newPatient.setEmail(request.getEmail());

        newPatient.setFullName(request.getFullName());
        newPatient.setPhone(request.getPhone());
        newPatient.setRole(Role.PATIENT);

        newPatient.setPassword(passwordEncoder.encode(request.getPassword()));
        return userRepository.save(newPatient);
    }

    @Override
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
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
    public PageResponseDTO<UserResponseDTO> getAllUsers(Pageable pageable) {

        // 👉 2. DÙNG THẲNG pageable, KHÔNG CẦN TẠO PageRequest NỮA
        Page<User> userPage = userRepository.findAll(pageable);

        // 3. Map Entity sang DTO
        List<UserResponseDTO> dtoList = userPage.getContent().stream()
                .map(user -> UserResponseDTO.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole().name())
                        .build())
                .collect(Collectors.toList());

        // 4. Trả về khay PageResponseDTO
        return PageResponseDTO.<UserResponseDTO>builder()
                .currentPage(userPage.getNumber())
                .totalPages(userPage.getTotalPages())
                .totalElements(userPage.getTotalElements())
                .pageSize(userPage.getSize())
                .data(dtoList)
                .build();
    }

    @Override
    public PageResponseDTO<? extends UserResponseDTO> getUsersByRole(String roleName, Pageable pageable) {

        // Nếu không truyền role, tự động gọi hàm lấy tất cả ở trên
        if (roleName == null || roleName.trim().isEmpty()) {
            return getAllUsers(pageable);
        }

        Role roleEnum;
        try {
            roleEnum = Role.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Quyền không hợp lệ! Chỉ nhận PATIENT, DOCTOR, ADMIN.");
        }

        // Lấy dữ liệu phân trang từ Database
        Page<User> userPage = userRepository.findByRole(roleEnum, pageable);

        // Biến hình từ Entity sang DTO
        List<? extends UserResponseDTO> dtoList;

        switch (roleEnum) {
            case DOCTOR:
                dtoList = userPage.getContent().stream().map(user -> {
                    DoctorResponseDTO dto = DoctorResponseDTO.builder()
                            .id(user.getId())
                            .fullName("Bác sĩ " + user.getFullName())
                            .email(user.getEmail())
                            .phone(user.getPhone())
                            .role(user.getRole().name())
                            .build();

                    if (user.getDoctorDetail() != null) {
                        dto.setSpecialtyName(user.getDoctorDetail().getSpecialty().getName());
                        dto.setConsultationFee(user.getDoctorDetail().getConsultationFee());
                        dto.setBio(user.getDoctorDetail().getBio());
                    }
                    return dto;
                }).collect(Collectors.toList());
                break;

            case PATIENT:
                dtoList = userPage.getContent().stream().map(user -> PatientResponseDTO.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole().name())
                        .build()
                ).collect(Collectors.toList());
                break;

            case ADMIN:
                dtoList = userPage.getContent().stream().map(user -> AdminResponseDTO.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole().name())
                        .adminLevel("SUPER_ADMIN")
                        .build()
                ).collect(Collectors.toList());
                break;

            default:
                dtoList = userPage.getContent().stream().map(user -> UserResponseDTO.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole().name())
                        .build()
                ).collect(Collectors.toList());
                break;
        }

        // Đóng gói vào khay trả về
        return PageResponseDTO.<UserResponseDTO>builder()
                .currentPage(userPage.getNumber())
                .totalPages(userPage.getTotalPages())
                .totalElements(userPage.getTotalElements())
                .pageSize(userPage.getSize())
                .data((List<UserResponseDTO>) dtoList)
                .build();
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));
    }

    @Override
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = getUserByEmail(email);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác!");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void forgotPassword(String email) {
        User user = getUserByEmail(email);
        // Trong thực tế sẽ gửi email chứa mã reset hoặc link
        // Ở đây chúng ta chỉ log ra console để giả lập
        System.out.println("Gửi email reset mật khẩu đến: " + email);
    }

    @Override
    public UserResponseDTO getMyInfo(String email) {
        User user = getUserByEmail(email);
        return mapToDTO(user);
    }

    @Override
    public UserResponseDTO updateProfile(String email, UserResponseDTO profileDTO) {
        User user = getUserByEmail(email);
        user.setFullName(profileDTO.getFullName());
        user.setPhone(profileDTO.getPhone());
        // Cập nhật các trường khác nếu có
        User updatedUser = userRepository.save(user);
        return mapToDTO(updatedUser);
    }

    @Override
    public DoctorResponseDTO updateDoctorProfile(String email, DoctorResponseDTO profileDTO) {
        User user = getUserByEmail(email);
        if (user.getRole() != Role.DOCTOR) {
            throw new RuntimeException("Chỉ bác sĩ mới có thể cập nhật hồ sơ bác sĩ!");
        }
        user.setFullName(profileDTO.getFullName());
        user.setPhone(profileDTO.getPhone());

        DoctorDetail detail = user.getDoctorDetail();
        if (detail == null) {
            detail = new DoctorDetail();
            detail.setUser(user);
        }
        detail.setBio(profileDTO.getBio());
        detail.setConsultationFee(profileDTO.getConsultationFee());
        doctorDetailRepository.save(detail);
        userRepository.save(user);

        // Fix ép kiểu không an toàn: gọi build trực tiếp thay vì cast từ mapToDTO
        return DoctorResponseDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .bio(detail.getBio())
                .consultationFee(detail.getConsultationFee())
                .specialtyName(detail.getSpecialty() != null ? detail.getSpecialty().getName() : null)
                .build();
    }

    @Override
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));
        return mapToDTO(user);
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy người dùng để xóa!");
        }
        userRepository.deleteById(id);
    }

    private UserResponseDTO mapToDTO(User user) {
        if (user.getRole() == Role.DOCTOR) {
            DoctorResponseDTO dto = DoctorResponseDTO.builder()
                    .id(user.getId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .role(user.getRole().name())
                    .build();
            if (user.getDoctorDetail() != null) {
                dto.setBio(user.getDoctorDetail().getBio());
                dto.setConsultationFee(user.getDoctorDetail().getConsultationFee());
                if (user.getDoctorDetail().getSpecialty() != null) {
                    dto.setSpecialtyName(user.getDoctorDetail().getSpecialty().getName());
                }
            }
            return dto;
        } else if (user.getRole() == Role.PATIENT) {
            return PatientResponseDTO.builder()
                    .id(user.getId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .role(user.getRole().name())
                    .build();
        } else {
            return UserResponseDTO.builder()
                    .id(user.getId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .role(user.getRole().name())
                    .build();
        }
    }
}
