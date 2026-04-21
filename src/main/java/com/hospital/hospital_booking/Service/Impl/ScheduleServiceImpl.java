package com.hospital.hospital_booking.Service.Impl;

import com.hospital.hospital_booking.Entity.Schedule;
import com.hospital.hospital_booking.Repository.ScheduleRepository;
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
    @Override
    public List<Schedule> getAvailableSlots(Long doctorId, LocalDate date) {
        List<Schedule> allSlots = scheduleRepository.findByDoctorIdAndWorkingDate(doctorId, date);

        return allSlots.stream()
                .filter(slot -> !slot.getIsBooked())
                .collect(Collectors.toList());
    }

    @Override
    public Schedule createSchedule(Schedule schedule) {
        return scheduleRepository.save(schedule);
    }

    @Override
    public void deleteSchedule(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch làm việc"));
        if (schedule.getIsBooked()) {
            throw new RuntimeException("Không thể xóa lịch đã có người đặt!");
        }
        scheduleRepository.deleteById(id);
    }

    @Override
    public void batchCreateSchedules(List<Schedule> schedules) {
        scheduleRepository.saveAll(schedules);
    }
}
