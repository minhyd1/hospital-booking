package com.hospital.hospital_booking.Service.Impl;

import com.hospital.hospital_booking.DTO.SpecialtyDTO;
import com.hospital.hospital_booking.Entity.Specialty;
import com.hospital.hospital_booking.Repository.SpecialtyRepository;
import com.hospital.hospital_booking.Service.SpecialtyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpecialtyServiceImpl implements SpecialtyService {
    private final SpecialtyRepository specialtyRepository;

    @Override
    public List<SpecialtyDTO> getAllSpecialties() {
        return specialtyRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SpecialtyDTO createSpecialty(Specialty specialty) {
        Specialty savedSpecialty = specialtyRepository.save(specialty);
        return mapToDTO(savedSpecialty);
    }

    private SpecialtyDTO mapToDTO(Specialty specialty) {
        return SpecialtyDTO.builder()
                .id(specialty.getId())
                .name(specialty.getName())
                .description(specialty.getDescription())
                .build();
    }

    @Override
    public void deleteSpecialty(Long id) {
        specialtyRepository.deleteById(id);
    }
}
