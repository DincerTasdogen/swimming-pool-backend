package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.ReservationDTO;
import com.sp.SwimmingPool.model.entity.Reservation;
import com.sp.SwimmingPool.model.enums.ReservationStatusEnum;
import com.sp.SwimmingPool.repos.ReservationRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReservationService {

    private ReservationRepository reservationRepository;

    public ReservationDTO createReservation(ReservationDTO reservationDTO) {
        Reservation reservation = new Reservation();
        reservation.setMemberId(reservationDTO.getMemberId());
        reservation.setSessionId(reservationDTO.getSessionId());
        reservation.setMemberPackageId(reservationDTO.getMemberPackageId());
        reservation.setStatus(reservationDTO.getStatus());

        reservationRepository.save(reservation);
        return reservationDTO;
    }
    public ReservationDTO updateReservationStatus(int id, ReservationStatusEnum status) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found with ID: " + id));
        reservation.setStatus(status);
        reservationRepository.save(reservation);

        ReservationDTO reservationDTO=new ReservationDTO();

        reservationDTO.setId(reservation.getId());
        reservationDTO.setMemberId(reservation.getMemberId());
        reservationDTO.setSessionId(reservation.getSessionId());
        reservationDTO.setMemberPackageId(reservation.getMemberPackageId());
        reservationDTO.setStatus(reservation.getStatus());

        return reservationDTO;
    }
   public List<ReservationDTO> listAllReservations() {
       List<Reservation> reservations = reservationRepository.findAll();
       List<ReservationDTO> reservationDTOs = new ArrayList<>();
       for (Reservation reservation : reservations) {
           ReservationDTO reservationDTO = new ReservationDTO();
           reservationDTO.setId(reservation.getId());
           reservationDTO.setMemberId(reservation.getMemberId());
           reservationDTO.setSessionId(reservation.getSessionId());
           reservationDTO.setMemberPackageId(reservation.getMemberPackageId());
           reservationDTO.setStatus(reservation.getStatus());
           reservationDTOs.add(reservationDTO);
       }
       return reservationDTOs;
    }
public ReservationDTO getReservationDetails(int id) {
    Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found with ID: " + id));

    ReservationDTO reservationDTO = new ReservationDTO();
    reservationDTO.setId(reservation.getId());
    reservationDTO.setMemberId(reservation.getMemberId());
    reservationDTO.setSessionId(reservation.getSessionId());
    reservationDTO.setMemberPackageId(reservation.getMemberPackageId());
    reservationDTO.setStatus(reservation.getStatus());

    return reservationDTO;
}

    public void deleteReservation(int id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found with ID: " + id));
        reservationRepository.delete(reservation);
    }
}

