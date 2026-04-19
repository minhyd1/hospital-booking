package com.hospital.hospital_booking.Service;

import com.hospital.hospital_booking.DTO.BookingRequestDTO;
import com.hospital.hospital_booking.DTO.UpcomingAppointmentDTO;
import com.hospital.hospital_booking.Entity.Appointment;

import java.util.List;

public interface AppointmentService {
    Appointment createBooking(BookingRequestDTO request);
    List<UpcomingAppointmentDTO> getUpcomingForDoctor(Long doctorId);
    List<UpcomingAppointmentDTO> getUpcomingForPatient(Long patientId);
}
