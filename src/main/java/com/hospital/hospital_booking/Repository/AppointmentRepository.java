package com.hospital.hospital_booking.Repository;

import com.hospital.hospital_booking.Entity.Appointment;
import com.hospital.hospital_booking.Entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByScheduleDoctorIdAndStatusAndScheduleWorkingDateGreaterThanEqualOrderByScheduleStartTimeAsc(
            Long doctorId, AppointmentStatus status, LocalDate date);

    List<Appointment> findByPatientIdAndStatusAndScheduleWorkingDateGreaterThanEqualOrderByScheduleStartTimeAsc(
            Long patientId, AppointmentStatus status, LocalDate date);
}
