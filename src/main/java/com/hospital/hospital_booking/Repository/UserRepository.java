package com.hospital.hospital_booking.Repository;

import com.hospital.hospital_booking.DTO.DoctorResponseDTO;
import com.hospital.hospital_booking.Entity.DoctorDetail;
import com.hospital.hospital_booking.Entity.Role;
import com.hospital.hospital_booking.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    List<User> findByRole (Role role);
    Optional<User> getUsersByRole(Role role);
    Page<User> findByRole(Role role, Pageable pageable);
}
