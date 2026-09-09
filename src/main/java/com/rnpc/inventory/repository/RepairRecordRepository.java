package com.rnpc.inventory.repository;

import com.rnpc.inventory.entity.RepairRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepairRecordRepository extends JpaRepository<RepairRecord, Long> {
    List<RepairRecord> findByClient_ClientIdOrderByRepairDateDesc(Long clientId);

    long countByClient_ClientId(Long clientId);

    List<RepairRecord> findByClient_User_UsernameOrderByRepairDateDesc(String username);
}
