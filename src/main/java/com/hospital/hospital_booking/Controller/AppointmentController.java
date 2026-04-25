package com.hospital.hospital_booking.Controller;

import com.hospital.hospital_booking.DTO.BookingRequestDTO;
import com.hospital.hospital_booking.DTO.UpcomingAppointmentDTO;
import com.hospital.hospital_booking.DTO.UserResponseDTO;
import com.hospital.hospital_booking.Entity.Appointment;
import com.hospital.hospital_booking.Service.AppointmentService;
import com.hospital.hospital_booking.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserService userService;

    // Lấy email người đang đăng nhập từ JWT
    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // Kiểm tra người dùng hiện tại có role ADMIN không
    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // Kiểm tra người dùng hiện tại có role DOCTOR không
    private boolean isDoctor() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
    }

    // ==================== PATIENT ENDPOINTS ====================

    // POST /api/appointments/book
    @PostMapping("/book")
    public ResponseEntity<?> createBooking(@Valid @RequestBody BookingRequestDTO request) {
        try {
            Appointment appointment = appointmentService.createBooking(getCurrentUserEmail(), request);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đặt lịch thành công!");
            response.put("appointmentId", appointment.getId());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE /api/appointments/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelAppointment(@PathVariable Long id) {
        try {
            appointmentService.cancelAppointment(getCurrentUserEmail(), id);
            return ResponseEntity.ok("Huỷ lịch hẹn thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /api/appointments/upcoming/patient/{patientId}
    // [SỬA LỖI ĐỎ 3] Chỉ bệnh nhân đúng chính mình hoặc ADMIN mới được xem
    @GetMapping("/upcoming/patient/{patientId}")
    public ResponseEntity<?> getUpcomingForPatient(@PathVariable Long patientId) {
        if (!isAdmin()) {
            UserResponseDTO currentUser = userService.getMyInfo(getCurrentUserEmail());
            if (!currentUser.getId().equals(patientId)) {
                return ResponseEntity.status(403)
                        .body("Bạn không có quyền xem lịch hẹn của người khác!");
            }
        }
        List<UpcomingAppointmentDTO> upcoming = appointmentService.getUpcomingForPatient(patientId);
        return ResponseEntity.ok(upcoming);
    }

    // GET /api/appointments/history/patient/{patientId}
    // [SỬA LỖI ĐỎ 3] Chỉ bệnh nhân đúng chính mình hoặc ADMIN mới được xem
    @GetMapping("/history/patient/{patientId}")
    public ResponseEntity<?> getHistoryForPatient(@PathVariable Long patientId) {
        if (!isAdmin()) {
            UserResponseDTO currentUser = userService.getMyInfo(getCurrentUserEmail());
            if (!currentUser.getId().equals(patientId)) {
                return ResponseEntity.status(403)
                        .body("Bạn không có quyền xem lịch sử của người khác!");
            }
        }
        return ResponseEntity.ok(appointmentService.getHistoryForPatient(patientId));
    }

    // ==================== DOCTOR ENDPOINTS ====================

    // GET /api/appointments/upcoming/doctor/{doctorId}
    // [SỬA LỖI ĐỎ 4] Chỉ đúng bác sĩ đó hoặc ADMIN mới được xem
    @GetMapping("/upcoming/doctor/{doctorId}")
    public ResponseEntity<?> getUpcomingForDoctor(@PathVariable Long doctorId) {
        if (!isAdmin()) {
            UserResponseDTO currentUser = userService.getMyInfo(getCurrentUserEmail());
            if (!currentUser.getId().equals(doctorId)) {
                return ResponseEntity.status(403)
                        .body("Bạn không có quyền xem lịch hẹn của bác sĩ khác!");
            }
        }
        List<UpcomingAppointmentDTO> upcoming = appointmentService.getUpcomingForDoctor(doctorId);
        return ResponseEntity.ok(upcoming);
    }

    // GET /api/appointments/history/doctor/{doctorId}
    // [SỬA LỖI ĐỎ 4] Chỉ đúng bác sĩ đó hoặc ADMIN mới được xem
    @GetMapping("/history/doctor/{doctorId}")
    public ResponseEntity<?> getHistoryForDoctor(@PathVariable Long doctorId) {
        if (!isAdmin()) {
            UserResponseDTO currentUser = userService.getMyInfo(getCurrentUserEmail());
            if (!currentUser.getId().equals(doctorId)) {
                return ResponseEntity.status(403)
                        .body("Bạn không có quyền xem lịch sử của bác sĩ khác!");
            }
        }
        return ResponseEntity.ok(appointmentService.getHistoryForDoctor(doctorId));
    }

    // ==================== ADMIN / SHARED ENDPOINTS ====================

    // PATCH /api/appointments/{id}/status
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            appointmentService.updateStatus(id, status);
            return ResponseEntity.ok("Cập nhật trạng thái thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /api/appointments  (ADMIN only - xem tất cả)
    @GetMapping
    public ResponseEntity<List<UpcomingAppointmentDTO>> getAllAppointments(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long doctorId) {
        return ResponseEntity.ok(appointmentService.getAllAppointments(date, status, doctorId));
    }
}