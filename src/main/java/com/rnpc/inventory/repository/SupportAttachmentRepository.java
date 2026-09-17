package com.rnpc.inventory.repository;
import com.rnpc.inventory.entity.SupportAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SupportAttachmentRepository extends JpaRepository<SupportAttachment,Long> {
    interface Summary { Long getId(); String getFilename(); long getSize(); }
    List<Summary> findByTicket_IdOrderByIdAsc(Long id);
    Optional<SupportAttachment> findByIdAndTicket_Id(Long id,Long ticketId);
}
