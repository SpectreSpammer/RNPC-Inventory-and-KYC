package com.rnpc.inventory.repository;
import com.rnpc.inventory.entity.SupportReply;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SupportReplyRepository extends JpaRepository<SupportReply,Long> {
    List<SupportReply> findByTicket_IdOrderByIdAsc(Long id);
}
