package com.lokivpn.repository;

import com.lokivpn.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByRole(String role);
    Optional<User> findById(Long id);
    Optional<User> findByChatId(String chatId);
    Optional<User> findByPlan(String plan);
}
