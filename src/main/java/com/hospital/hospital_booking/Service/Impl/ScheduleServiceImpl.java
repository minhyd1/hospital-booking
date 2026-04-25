package com.hospital.hospital_booking.Service.Impl;

import com.hospital.hospital_booking.DTO.ScheduleRequestDTO;
import com.hospital.hospital_booking.Entity.AppointmentStatus;
import com.hospital.hospital_booking.Entity.Schedule;
import com.hospital.hospital_booking.Entity.User;
import com.hospital.hospital_booking.Repository.AppointmentRepository;
import com.hospital.hospital_booking.Repository.ScheduleRepository;
import com.hospital.hospital_booking.Repository.UserRepository;
import com.hospital.hospital_booking.Service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    // [SỬA LỖI ĐỎ 2] Cần AppointmentRepository để kiểm tra appointment PENDING
    private final AppointmentRepository appointmentRepository;

    @Override
    public List<Schedule> getAvailableSlots(Long doctorId, LocalDate date) {
        return scheduleRepository.findByDoctorIdAndWorkingDate(doctorId, date)
                .stream()
                .filter(slot -> !slot.getIsBooked())
                .collect(Collectors.toList());
    }

    @Override
    public List<Schedule> getDoctorSlots(Long doctorId, LocalDate date) {
        return scheduleRepository.findByDoctorIdAndWorkingDate(doctorId, date);
    }

    @Override
    public Schedule createSchedule(ScheduleRequestDTO dto) {
        User doctor = userRepository.findById(dto.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bác sĩ"));

        Schedule schedule = new Schedule();
        schedule.setDoctor(doctor);
        schedule.setWorkingDate(dto.getWorkingDate());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setIsBooked(false);

        return scheduleRepository.save(schedule);
    }

    @Override
    public void deleteSchedule(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch làm việc"));

        // [SỬA LỖI ĐỎ 2] Chặn xoá nếu isBooked = true (đã CONFIRMED)
        if (schedule.getIsBooked()) {
            throw new RuntimeException("Không thể xoá lịch đã có người đặt!");
        }

        // [SỬA LỖI ĐỎ 2] Chặn xoá nếu vẫn còn appointment PENDING trên slot này
        // (PENDING chưa set isBooked = true nên cần check thêm)
        boolean hasPendingAppointment = appointmentRepository
                .existsByScheduleIdAndStatus(id, AppointmentStatus.PENDING);
        if (hasPendingAppointment) {
            throw new RuntimeException("Không thể xoá lịch đang có bệnh nhân chờ xác nhận!");
        }

        scheduleRepository.deleteById(id);
    }

    @Override
    public int batchCreateSchedules(List<ScheduleRequestDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) return 0;
        List<Schedule> schedules = dtos.stream().map(dto -> {
            User doctor = userRepository.findById(dto.getDoctorId())
                    .orElseThrow(() -> new RuntimeException(
                            "Không tìm thấy bác sĩ ID: " + dto.getDoctorId()));
            Schedule s = new Schedule();
            s.setDoctor(doctor);
            s.setWorkingDate(dto.getWorkingDate());
            s.setStartTime(dto.getStartTime());
            s.setEndTime(dto.getEndTime());
            s.setIsBooked(false);
            return s;
        }).collect(Collectors.toList());

        scheduleRepository.saveAll(schedules);
        return schedules.size();
    }
}
