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

    User findByReferralCode(String referralCode);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.balance = u.balance + :amount WHERE u.chatId = :userId")
    void incrementBalance(@Param("userId") Long userId, @Param("amount") int amount);
}

