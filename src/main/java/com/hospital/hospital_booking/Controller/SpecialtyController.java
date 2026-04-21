package com.hospital.hospital_booking.Controller;

import com.hospital.hospital_booking.DTO.SpecialtyDTO;
import com.hospital.hospital_booking.Entity.Specialty;
import com.hospital.hospital_booking.Service.SpecialtyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/specialties")
@RequiredArgsConstructor
public class SpecialtyController {
    private final SpecialtyService specialtyService;

    @GetMapping
    public ResponseEntity<List<SpecialtyDTO>> getAllSpecialties() {
        return ResponseEntity.ok(specialtyService.getAllSpecialties());
    }

    @PostMapping
    public ResponseEntity<SpecialtyDTO> createSpecialty(@RequestBody Specialty specialty) {
        return ResponseEntity.ok(specialtyService.createSpecialty(specialty));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSpecialty(@PathVariable Long id) {
        specialtyService.deleteSpecialty(id);
        return ResponseEntity.ok("Xóa chuyên khoa thành công!");
    }
}
