package com.hospital.hospital_booking.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingAppointmentDTO {
    private Long appointmentId;

    // Tên đối tác (Nếu là góc nhìn Bác sĩ thì hiển thị tên Bệnh nhân, và ngược lại)
    private String partnerName;
    private String specialtyName;

    private LocalDateTime appointmentDateTime;
    private String timeRemaining;
    private String meetingLink;
    private String symptoms;
    private String status;
    private String patientName;
    private String doctorName;
}
