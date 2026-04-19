package com.hospital.hospital_booking.Repository;

import com.hospital.hospital_booking.Entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByDoctorIdAndWorkingDate(Long doctorId, LocalDate workingDate);
}
