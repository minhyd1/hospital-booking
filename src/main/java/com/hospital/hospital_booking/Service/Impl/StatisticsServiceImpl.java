package com.hospital.hospital_booking.Service.Impl;

import com.hospital.hospital_booking.Entity.AppointmentStatus;
import com.hospital.hospital_booking.Repository.AppointmentRepository;
import com.hospital.hospital_booking.Repository.UserRepository;
import com.hospital.hospital_booking.Repository.SpecialtyRepository;
import com.hospital.hospital_booking.Service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
        
        // 1. Đếm tổng cơ bản
        long totalAppointments = appointmentRepository.count();
        stats.put("totalAppointments", totalAppointments);
        stats.put("totalUsers", userRepository.count());
        stats.put("totalSpecialties", specialtyRepository.count());

        // 2. Thống kê theo trạng thái
        stats.put("completedAppointments", appointmentRepository.findByStatus(AppointmentStatus.COMPLETED).size());
        stats.put("cancelledAppointments", appointmentRepository.findByStatus(AppointmentStatus.CANCELLED).size());
        stats.put("pendingAppointments", appointmentRepository.findByStatus(AppointmentStatus.PENDING).size());

        // 3. Tính doanh thu (tổng consultationFee của các appointment COMPLETED)
        BigDecimal totalRevenue = appointmentRepository.findByStatus(AppointmentStatus.COMPLETED).stream()
                .map(app -> app.getSchedule().getDoctor().getDoctorDetail().getConsultationFee())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalRevenue", totalRevenue);

        return stats;
    }
}
