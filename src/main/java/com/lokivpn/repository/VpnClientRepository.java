package com.lokivpn.repository;

import com.lokivpn.model.VpnClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VpnClientRepository
        extends JpaRepository<VpnClient, Long>, JpaSpecificationExecutor<VpnClient> {

    // Подсчёт всех устройств для данного chatId
    int countByChatId(String chatId);

    // Подсчёт устройств определённого типа для данного chatId
    int countByChatIdAndDeviceType(String chatId, String deviceType);

    // Найти первого клиента по chatId
    Optional<VpnClient> findFirstByChatId(String chatId);

    // Найти всех клиентов по chatId
    List<VpnClient> findAllByChatId(String chatId);

    // Найти первого не назначенного клиента
    Optional<VpnClient> findFirstByIsAssignedFalse();

    // Найти первого не назначенного клиента, исключая определённый ID
    Optional<VpnClient> findFirstByIsAssignedFalseAndIdNot(Long id);

    // Сортировка по ID (используется в методе /users/sorted)
    List<VpnClient> findAllByOrderByIdAsc();

    // Фильтры (username, deviceType, isAssigned)
    @Query("SELECT v FROM VpnClient v WHERE " +
            "(:username IS NULL OR v.username LIKE %:username%) AND " +
            "(:deviceType IS NULL OR v.deviceType = :deviceType) AND " +
            "(:isAssigned IS NULL OR v.isAssigned = :isAssigned) " +
            "ORDER BY v.id ASC")
    List<VpnClient> findByFilters(
            @Param("username") String username,
            @Param("deviceType") String deviceType,
            @Param("isAssigned") Boolean isAssigned
    );

    // ======== Методы для расширенной статистики ========

    // Подсчёт занятых/свободных
    int countByIsAssigned(boolean isAssigned);

    // Подсчёт по конкретному deviceType, например "PC" или "Phone"
    int countByDeviceType(String deviceType);

    @Modifying
    @Query("UPDATE VpnClient v SET v.reservedUntil = NULL WHERE v.reservedUntil < CURRENT_TIMESTAMP")
    void clearExpiredReservations();

    Optional<VpnClient> findFirstByChatIdAndReservedUntilAfter(String chatId, LocalDateTime reservedUntil);

    void deleteByChatId(String chatId);

    Optional<VpnClient> findFirstByReservedUntilBeforeOrReservedUntilIsNull(LocalDateTime dateTime);

    @Query("SELECT vc FROM VpnClient vc WHERE vc.reservedUntil IS NOT NULL AND vc.reservedUntil < :currentTime")
    List<VpnClient> findAllExpiredReservations(@Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT vc FROM VpnClient vc WHERE vc.chatId = :chatId AND (vc.isAssigned = TRUE OR (vc.reservedUntil IS NOT NULL AND vc.reservedUntil > CURRENT_TIMESTAMP))")
    List<VpnClient> findActiveAndReservedClientsByChatId(@Param("chatId") String chatId);

    @Query("SELECT v FROM VpnClient v WHERE v.isAssigned = false AND (v.reservedUntil IS NULL OR v.reservedUntil < :now) ORDER BY v.id ASC")
    List<VpnClient> findAvailableClients(@Param("now") LocalDateTime now);

    // Найти клиента по chatId и плану с активной резервацией
    Optional<VpnClient> findFirstByChatIdAndPlanAndReservedUntilAfter(String chatId, String plan, LocalDateTime now);

    Optional<VpnClient> findFirstByChatIdAndIsAssignedTrueOrderByIdDesc(String chatId);
}


