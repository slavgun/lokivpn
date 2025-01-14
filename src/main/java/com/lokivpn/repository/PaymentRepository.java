package com.lokivpn.repository;

import com.lokivpn.model.PaymentRecord;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentRecord, Long> {
    @Query("SELECT SUM(p.amount) FROM PaymentRecord p WHERE p.userId = :userId")
    Integer findBalanceByUserId(@Param("userId") Long userId);

    @Transactional
    @Modifying
    @Query("UPDATE PaymentRecord p SET p.amount = :balance WHERE p.userId = :userId")
    void updateBalanceByUserId(@Param("userId") Long userId, @Param("balance") int balance);

    @Query("SELECT MAX(p.paymentDate) FROM PaymentRecord p WHERE p.userId = :userId")
    LocalDateTime findLastPaymentDateByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT p.userId FROM PaymentRecord p")
    List<Long> findAllUserIds();

}

