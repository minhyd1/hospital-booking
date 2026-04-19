package com.hospital.hospital_booking.Controller;

import com.hospital.hospital_booking.DTO.BookingRequestDTO;
import com.hospital.hospital_booking.DTO.UpcomingAppointmentDTO;
import com.hospital.hospital_booking.Entity.Appointment;
import com.hospital.hospital_booking.Service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService appointmentService;
    // URL: POST http://localhost:8080/api/appointments/book
    @PostMapping("/book")
    public ResponseEntity<?> createBooking(@RequestBody BookingRequestDTO request) {
        try {
            Appointment appointment = appointmentService.createBooking(request);
            return ResponseEntity.ok("Đặt lịch thành công! Mã lịch hẹn của bạn là: " + appointment.getId());
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
}
