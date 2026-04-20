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
import org.springframework.security.access.prepost.PreAuthorize;
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

}
