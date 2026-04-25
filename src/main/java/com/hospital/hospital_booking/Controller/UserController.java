package com.hospital.hospital_booking.Controller;

import com.hospital.hospital_booking.DTO.DoctorResponseDTO;
import com.hospital.hospital_booking.DTO.PageResponseDTO;
import com.hospital.hospital_booking.DTO.RegisterRequestDTO;
import com.hospital.hospital_booking.DTO.UserResponseDTO;
import com.hospital.hospital_booking.Entity.User;
import com.hospital.hospital_booking.Service.UserService;
import com.hospital.hospital_booking.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // URL: POST http://localhost:8080/api/users/register
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        try {
            User newUser = userService.registerPatient(request);
            return ResponseEntity.ok(newUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // URL: POST http://localhost:8080/api/users/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        try {
            String email = loginData.get("email");
            String password = loginData.get("password");

            User user = userService.login(email, password);
            // Sửa dòng tạo Token cũ thành dòng này:
            String jwt = tokenProvider.generateToken(user.getEmail(), user.getRole().name());

            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("id", user.getId());
            response.put("role", user.getRole());
            response.put("fullName", user.getFullName());
            response.put("email", user.getEmail());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // URL: GET http://localhost:8080/api/users/doctors?specialtyId=1
    @GetMapping("/doctors")
    public ResponseEntity<List<DoctorResponseDTO>> getDoctorsBySpecialty(@RequestParam Long specialtyId) {
        List<DoctorResponseDTO> doctors = userService.getDoctorsBySpecialty(specialtyId);
        return ResponseEntity.ok(doctors);
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<? extends UserResponseDTO>> getUsers(
            @RequestParam(required = false) String role,
            @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(userService.getUsersByRole(role, pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo() {
        return ResponseEntity.ok(userService.getMyInfo(getCurrentUserEmail()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> data) {
        userService.changePassword(getCurrentUserEmail(), data.get("oldPassword"), data.get("newPassword"));
        return ResponseEntity.ok("Đổi mật khẩu thành công!");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> data) {
        userService.forgotPassword(data.get("email"));
        return ResponseEntity.ok("Yêu cầu đã được gửi! Vui lòng kiểm tra email.");
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken() {
        // Lấy thông tin từ SecurityContextHolder thay vì parse token thô
        String email = getCurrentUserEmail();
        // Cần lấy thêm role từ SecurityContextHolder (đã được Filter nạp vào authorities)
        String role = SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        
        String newToken = tokenProvider.generateToken(email, role);
        Map<String, String> response = new HashMap<>();
        response.put("token", newToken);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UserResponseDTO profileDTO) {
        return ResponseEntity.ok(userService.updateProfile(getCurrentUserEmail(), profileDTO));
    }

    @PutMapping("/doctor-profile")
    public ResponseEntity<?> updateDoctorProfile(@RequestBody DoctorResponseDTO profileDTO) {
        return ResponseEntity.ok(userService.updateDoctorProfile(getCurrentUserEmail(), profileDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("Xóa người dùng thành công!");
    }

    @GetMapping("/doctors/{id}")
    public ResponseEntity<?> getDoctorById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserResponseDTO profileDTO) {
        // Sử dụng getUserById để lấy email sau đó gọi updateProfile hoặc thêm method mới vào service
        UserResponseDTO user = userService.getUserById(id);
        return ResponseEntity.ok(userService.updateProfile(user.getEmail(), profileDTO));
    }
}
