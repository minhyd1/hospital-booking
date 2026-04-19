package com.hospital.hospital_booking.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "doctor_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_id")
    private Specialty specialty;

    @Column(name = "consultation_fee", nullable = false)
    private BigDecimal consultationFee = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String bio;
}
