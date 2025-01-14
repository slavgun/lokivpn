package com.lokivpn.repository;

import com.lokivpn.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.chatId = :chatId")
    Optional<User> findByChatId(Long chatId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.balance = :balance WHERE u.id = :userId")
    void updateBalanceByUserId(Long userId, int balance);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.clientsCount = u.clientsCount + 1 WHERE u.id = :userId")
    void incrementClientCount(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.clientsCount = u.clientsCount - 1 WHERE u.id = :userId")
    void decrementClientCount(Long userId);
}

