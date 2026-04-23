package com.hospital.hospital_booking.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingRequestDTO {
    private Long patientId; // Giữ lại để tương thích, nhưng service sẽ dùng email từ token
    
    @NotNull(message = "Vui lòng chọn khung giờ khám")
    private Long scheduleId;
    
    private String symptoms;
}
