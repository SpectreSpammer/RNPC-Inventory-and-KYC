package com.rnpc.inventory.repository;
import com.rnpc.inventory.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SupportTicketRepository extends JpaRepository<SupportTicket,Long> {
    List<SupportTicket> findByUser_UsernameOrderByIdDesc(String username);
    List<SupportTicket> findAllByOrderByIdDesc();
    Optional<SupportTicket> findByIdAndUser_Username(Long id,String username);
}
