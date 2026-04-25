package com.hospital.hospital_booking.DTO;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ScheduleRequestDTO {
    private Long doctorId;
    private LocalDate workingDate;
    private LocalTime startTime;
    private LocalTime endTime;
}
