package com.hospital.hospital_booking.Service.Impl;

import com.hospital.hospital_booking.Repository.AppointmentRepository;
import com.hospital.hospital_booking.Repository.UserRepository;
import com.hospital.hospital_booking.Repository.SpecialtyRepository;
import com.hospital.hospital_booking.Service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;

    @Override
    public Map<String, Object> getOverallStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAppointments", appointmentRepository.count());
        stats.put("totalUsers", userRepository.count());
        stats.put("totalSpecialties", specialtyRepository.count());
        // Có thể thêm doanh thu nếu có thông tin giá khám
        return stats;
    }
}
