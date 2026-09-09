package com.rnpc.inventory.repository;

import com.rnpc.inventory.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPreferredDateBetweenOrderByPreferredDateAscPreferredTimeAsc(
            LocalDate start, LocalDate end);

    List<Appointment> findByClient_User_UsernameOrderByPreferredDateDescPreferredTimeDesc(String username);
}
