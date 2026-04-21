package com.hospital.hospital_booking.Controller;

import com.hospital.hospital_booking.DTO.DoctorResponseDTO;
import com.hospital.hospital_booking.DTO.PageResponseDTO;
import com.hospital.hospital_booking.DTO.RegisterRequestDTO;
import com.hospital.hospital_booking.DTO.UserResponseDTO;
import com.hospital.hospital_booking.Entity.User;
import com.hospital.hospital_booking.Service.UserService;
import com.hospital.hospital_booking.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
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

    // URL: POST http://localhost:8080/api/users/register
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDTO request) {
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
            response.put("role", user.getRole());
            response.put("fullName", user.getFullName());

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
    public ResponseEntity<?> getMyInfo(@RequestHeader("Authorization") String token) {
        String email = tokenProvider.getEmailFromJWT(token.substring(7));
        return ResponseEntity.ok(userService.getMyInfo(email));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestHeader("Authorization") String token, @RequestBody Map<String, String> data) {
        String email = tokenProvider.getEmailFromJWT(token.substring(7));
        userService.changePassword(email, data.get("oldPassword"), data.get("newPassword"));
        return ResponseEntity.ok("Đổi mật khẩu thành công!");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> data) {
        userService.forgotPassword(data.get("email"));
        return ResponseEntity.ok("Yêu cầu đã được gửi! Vui lòng kiểm tra email.");
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String token) {
        String jwt = token.substring(7);
        if (tokenProvider.validateToken(jwt)) {
            String email = tokenProvider.getEmailFromJWT(jwt);
            String role = tokenProvider.getRoleFromJWT(jwt);
            String newToken = tokenProvider.generateToken(email, role);
            Map<String, String> response = new HashMap<>();
            response.put("token", newToken);
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body("Token không hợp lệ!");
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestHeader("Authorization") String token, @RequestBody UserResponseDTO profileDTO) {
        String email = tokenProvider.getEmailFromJWT(token.substring(7));
        return ResponseEntity.ok(userService.updateProfile(email, profileDTO));
    }

    @PutMapping("/doctor-profile")
    public ResponseEntity<?> updateDoctorProfile(@RequestHeader("Authorization") String token, @RequestBody DoctorResponseDTO profileDTO) {
        String email = tokenProvider.getEmailFromJWT(token.substring(7));
        return ResponseEntity.ok(userService.updateDoctorProfile(email, profileDTO));
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
