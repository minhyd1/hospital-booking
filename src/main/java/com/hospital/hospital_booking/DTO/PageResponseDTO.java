package com.hospital.hospital_booking.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageResponseDTO<T> {
    private int currentPage;    // Trang hiện tại (Bắt đầu từ 0)
    private int totalPages;     // Tổng số trang
    private long totalElements; // Tổng số phần tử trong toàn bộ DB
    private int pageSize;       // Số phần tử trên 1 trang
    private List<T> data;       // Dữ liệu thực tế của trang hiện tại
}