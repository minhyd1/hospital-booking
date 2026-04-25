package com.hospital.hospital_booking.Controller;

import com.hospital.hospital_booking.DTO.ScheduleRequestDTO;
import com.hospital.hospital_booking.Entity.Schedule;
import com.hospital.hospital_booking.Entity.User;
import com.hospital.hospital_booking.Repository.UserRepository;
import com.hospital.hospital_booking.Service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final UserRepository userRepository;

    private Map<String, Object> toSlimSchedule(Schedule s) {
        return Map.of(
                "id", s.getId(),
                "workingDate", s.getWorkingDate(),
                "startTime", s.getStartTime(),
                "endTime", s.getEndTime(),
                "isBooked", s.getIsBooked()
        );
    }

    // GET /api/schedule/available?doctorId=2&date=2026-04-20  (PUBLIC)
    @GetMapping("/available")
    public ResponseEntity<List<Map<String, Object>>> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<Schedule> availableSlots = scheduleService.getAvailableSlots(doctorId, date);
        return ResponseEntity.ok(availableSlots.stream().map(this::toSlimSchedule).collect(Collectors.toList()));
    }

    // GET /api/schedule/doctor?date=2026-04-20  (DOCTOR ONLY — xem lịch của chính mình, bao gồm cả đã đặt)
    @GetMapping("/doctor")
    public ResponseEntity<List<Map<String, Object>>> getDoctorSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User doctor = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        List<Schedule> slots = scheduleService.getDoctorSlots(doctor.getId(), date);
        return ResponseEntity.ok(slots.stream().map(this::toSlimSchedule).collect(Collectors.toList()));
    }

    // POST /api/schedule  (DOCTOR tạo từng slot cho lịch của chính mình)
    @PostMapping
    public ResponseEntity<?> createSchedule(@RequestBody ScheduleRequestDTO scheduleDTO) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User doctor = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            if (scheduleDTO.getDoctorId() == null) {
                scheduleDTO.setDoctorId(doctor.getId());
            } else if (!scheduleDTO.getDoctorId().equals(doctor.getId())) {
                return ResponseEntity.badRequest().body("Không thể tạo lịch cho bác sĩ khác.");
            }

            Schedule created = scheduleService.createSchedule(scheduleDTO);
            return ResponseEntity.ok(Map.of(
                    "message", "Tạo lịch làm việc thành công!",
                    "schedule", toSlimSchedule(created)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE /api/schedule/{id}  (DOCTOR xoá slot của chính mình)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSchedule(@PathVariable Long id) {
        try {
            scheduleService.deleteSchedule(id);
            return ResponseEntity.ok(Map.of("message", "Xoá lịch làm việc thành công!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // [SỬA VẤN ĐỀ VÀNG 6]
    // POST /api/schedule/admin/batch  (ADMIN ONLY — tạo hàng loạt cho nhiều bác sĩ)
    // Đổi từ /batch sang /admin/batch để tách biệt rõ ràng khỏi endpoint của DOCTOR
    // SecurityConfig cần cập nhật: hasRole("ADMIN") cho /api/schedule/admin/batch
    @PostMapping("/admin/batch")
    public ResponseEntity<?> batchCreateSchedules(@RequestBody List<ScheduleRequestDTO> scheduleDTOs) {
        try {
            int created = scheduleService.batchCreateSchedules(scheduleDTOs);
            return ResponseEntity.ok(Map.of(
                    "message", "Tạo lịch làm việc hàng loạt thành công!",
                    "created", created
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
