package com.hospital.hospital_booking.Service;

import com.hospital.hospital_booking.Entity.Schedule;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleService {
    List<Schedule> getAvailableSlots(Long doctorId, LocalDate date);

    Schedule createSchedule(Schedule schedule);

    void deleteSchedule(Long id);

    void batchCreateSchedules(List<Schedule> schedules);
}
