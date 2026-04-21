package com.hospital.hospital_booking.Service;

import com.hospital.hospital_booking.DTO.SpecialtyDTO;
import com.hospital.hospital_booking.Entity.Specialty;
import java.util.List;

public interface SpecialtyService {
    List<SpecialtyDTO> getAllSpecialties();
    SpecialtyDTO createSpecialty(Specialty specialty);
    void deleteSpecialty(Long id);
}
