package com.lokivpn.repository;

import com.lokivpn.model.VpnClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VpnClientRepository extends JpaRepository<VpnClient, Long> {
    Optional<VpnClient> findFirstByAssignedFalse();

    List<VpnClient> findByUserId(Long userId);

    @Query("SELECT DISTINCT v.userId FROM VpnClient v WHERE v.assigned = true")
    List<Long> findAllUserIdsWithClients();

    @Query("SELECT COUNT(c) FROM VpnClient c WHERE c.userId = :userId")
    int countByUserId(@Param("userId") Long userId);
}
