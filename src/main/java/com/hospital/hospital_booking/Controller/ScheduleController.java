package com.hospital.hospital_booking.Controller;

import com.hospital.hospital_booking.Entity.Schedule;
import com.hospital.hospital_booking.Service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/schedule")
@RequiredArgsConstructor
public class ScheduleController {
    private final ScheduleService scheduleService;
    // URL: GET http://localhost:8080/api/schedules/available?doctorId=2&date=2026-04-20
    @GetMapping("/available")
    public ResponseEntity<List<Schedule>> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<Schedule> availableSlots = scheduleService.getAvailableSlots(doctorId, date);
        return ResponseEntity.ok(availableSlots);
    }
}
