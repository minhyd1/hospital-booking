package com.hospital.hospital_booking.Repository;

import com.hospital.hospital_booking.Entity.DoctorDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorDetailRepository extends JpaRepository<DoctorDetail, Long> {
    List<DoctorDetail> findBySpecialtyId(Long specialtyId);
}
