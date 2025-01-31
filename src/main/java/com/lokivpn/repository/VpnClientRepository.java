package com.lokivpn.repository;

import com.lokivpn.model.VpnClient;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VpnClientRepository extends JpaRepository<VpnClient, Long> {
    Optional<VpnClient> findFirstByAssignedFalse();

    List<VpnClient> findByUserId(Long userId);

    @Query("SELECT COUNT(c) FROM VpnClient c WHERE c.userId = :userId")
    int countByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE VpnClient v SET v.assigned = false, v.userId = null WHERE v.userId = :userId")
    void unassignClientsByUserId(Long userId);

    boolean existsByEncryptedKey(String encryptedKey);
}
