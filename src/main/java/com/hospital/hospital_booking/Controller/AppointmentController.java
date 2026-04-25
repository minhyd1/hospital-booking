package com.hospital.hospital_booking.Controller;

import com.hospital.hospital_booking.DTO.BookingRequestDTO;
import com.hospital.hospital_booking.DTO.UpcomingAppointmentDTO;
import com.hospital.hospital_booking.Entity.Appointment;
import com.hospital.hospital_booking.Service.AppointmentService;
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

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // URL: POST http://localhost:8080/api/appointments/book
    @PostMapping("/book")
    public ResponseEntity<?> createBooking(@Valid @RequestBody BookingRequestDTO request) {
        try {
            Appointment appointment = appointmentService.createBooking(getCurrentUserEmail(), request);
            Map<String, Object> resp = new HashMap<>();
            resp.put("message", "Đặt lịch thành công!");
            resp.put("appointmentId", appointment.getId());
            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // Báo lỗi nếu trùng slot
        }
    }
    // URL: GET http://localhost:8080/api/appointments/upcoming/doctor/2
    @GetMapping("/upcoming/doctor/{doctorId}")
    public ResponseEntity<List<UpcomingAppointmentDTO>> getUpcomingForDoctor(@PathVariable Long doctorId) {
        List<UpcomingAppointmentDTO> upcoming = appointmentService.getUpcomingForDoctor(doctorId);
        return ResponseEntity.ok(upcoming);
    }
    // URL: GET http://localhost:8080/api/appointments/upcoming/patient/1
    @GetMapping("/upcoming/patient/{patientId}")
    public ResponseEntity<List<UpcomingAppointmentDTO>> getUpcomingForPatient(@PathVariable Long patientId) {
        List<UpcomingAppointmentDTO> upcoming = appointmentService.getUpcomingForPatient(patientId);
        return ResponseEntity.ok(upcoming);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelAppointment(@PathVariable Long id) {
        appointmentService.cancelAppointment(getCurrentUserEmail(), id);
        return ResponseEntity.ok(Map.of("message", "Huỷ lịch hẹn thành công!"));
    }

    @GetMapping("/history/patient/{patientId}")
    public ResponseEntity<List<UpcomingAppointmentDTO>> getHistoryForPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(appointmentService.getHistoryForPatient(patientId));
    }

    @GetMapping("/history/doctor/{doctorId}")
    public ResponseEntity<List<UpcomingAppointmentDTO>> getHistoryForDoctor(@PathVariable Long doctorId) {
        return ResponseEntity.ok(appointmentService.getHistoryForDoctor(doctorId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        appointmentService.updateStatus(id, status);
        return ResponseEntity.ok(Map.of("message", "Cập nhật trạng thái thành công!"));
    }

    @GetMapping
    public ResponseEntity<List<UpcomingAppointmentDTO>> getAllAppointments(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long doctorId) {
        return ResponseEntity.ok(appointmentService.getAllAppointments(date, status, doctorId));
    }
}