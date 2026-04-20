package com.hospital.hospital_booking.Service.Impl;

import com.hospital.hospital_booking.DTO.BookingRequestDTO;
import com.hospital.hospital_booking.DTO.UpcomingAppointmentDTO;
import com.hospital.hospital_booking.Entity.Appointment;
import com.hospital.hospital_booking.Entity.AppointmentStatus;
import com.hospital.hospital_booking.Entity.Schedule;
import com.hospital.hospital_booking.Entity.User;
import com.hospital.hospital_booking.Repository.AppointmentRepository;
import com.hospital.hospital_booking.Repository.ScheduleRepository;
import com.hospital.hospital_booking.Repository.UserRepository;
import com.hospital.hospital_booking.Service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;



    @Override
    @Transactional
    public Appointment createBooking(BookingRequestDTO request) {
            User patient = userRepository.findById(request.getPatientId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Bệnh nhân"));

            Schedule schedule = scheduleRepository.findByIdWithLock(request.getScheduleId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Khung giờ khám"));

            if (schedule.getIsBooked()) {
                throw new RuntimeException("Rất tiếc, khung giờ này đã có người đặt!");
            }

            schedule.setIsBooked(true);
            scheduleRepository.save(schedule);

            Appointment appointment = new Appointment();
            appointment.setPatient(patient);
            appointment.setSchedule(schedule);
            appointment.setSymptoms(request.getSymptoms());
            appointment.setStatus(AppointmentStatus.CONFIRMED);

            return appointmentRepository.save(appointment);
    }

    @Override
    public List<UpcomingAppointmentDTO> getUpcomingForDoctor(Long doctorId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        List<Appointment> appointments = appointmentRepository
                .findByScheduleDoctorIdAndStatusAndScheduleWorkingDateGreaterThanEqualOrderByScheduleStartTimeAsc(
                        doctorId, AppointmentStatus.CONFIRMED, today);

        return appointments.stream()
                .filter(app -> getAppointmentDateTime(app).isAfter(now))
                .map(app -> buildUpcomingDTO(app, now, true))
                .collect(Collectors.toList());
    }

    @Override
    public List<UpcomingAppointmentDTO> getUpcomingForPatient(Long patientId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        List<Appointment> appointments = appointmentRepository
                .findByPatientIdAndStatusAndScheduleWorkingDateGreaterThanEqualOrderByScheduleStartTimeAsc(
                        patientId, AppointmentStatus.CONFIRMED, today);

        return appointments.stream()
                .filter(app -> getAppointmentDateTime(app).isAfter(now))
                .map(app -> buildUpcomingDTO(app, now, false))
                .collect(Collectors.toList());
    }
    private LocalDateTime getAppointmentDateTime(Appointment app) {
        return LocalDateTime.of(
                app.getSchedule().getWorkingDate(),
                app.getSchedule().getStartTime()
        );
    }

    private UpcomingAppointmentDTO buildUpcomingDTO(Appointment app, LocalDateTime now, boolean isDoctorView) {
        LocalDateTime appDateTime = getAppointmentDateTime(app);

        String partnerName = isDoctorView ?
                app.getPatient().getFullName() :
                "Bác sĩ " + app.getSchedule().getDoctor().getFullName();

        String specialtyName = app.getSchedule().getDoctor().getDoctorDetail().getSpecialty().getName();
        String meetingLink = "https://meet.google.com/room-" + app.getId();

        return UpcomingAppointmentDTO.builder()
                .appointmentId(app.getId())
                .partnerName(partnerName)
                .specialtyName(specialtyName)
                .appointmentDateTime(appDateTime)
                .timeRemaining(calculateTimeRemaining(now, appDateTime))
                .meetingLink(meetingLink)
                .symptoms(app.getSymptoms())
                .build();
    }

    private String calculateTimeRemaining(LocalDateTime now, LocalDateTime appTime) {
        Duration duration = Duration.between(now, appTime);
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        if (days > 0) {
            return "Còn " + days + " ngày " + hours + " giờ";
        } else if (hours > 0) {
            return "Còn " + hours + " giờ " + minutes + " phút";
        } else {
            return "Còn " + minutes + " phút";
        }
    }
}
