package com.hospital.hospital_booking.DTO;

import lombok.Data;

@Data
public class BookingRequestDTO {
    private Long patientId;
    private Long scheduleId;
    private String symptoms;
}
