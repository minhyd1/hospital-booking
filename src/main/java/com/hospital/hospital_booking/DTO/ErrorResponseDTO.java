package com.hospital.hospital_booking.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponseDTO {
    private LocalDateTime timestamp; // Thời gian xảy ra lỗi
    private int status;              // Mã lỗi (VD: 400, 404)
    private String error;            // Loại lỗi (VD: Bad Request)
    private String message;          // Lời nhắn chi tiết (VD: "Sai mật khẩu!")
}