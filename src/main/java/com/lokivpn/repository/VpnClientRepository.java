package com.lokivpn.repository;

import com.lokivpn.model.VpnClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VpnClientRepository extends JpaRepository<VpnClient, Long> {
    Optional<VpnClient> findFirstByAssignedFalse();
    List<VpnClient> findByUserId(Long userId);
}
