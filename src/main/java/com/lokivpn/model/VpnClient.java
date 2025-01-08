package com.lokivpn.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vpn_clients")
public class VpnClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_name", nullable = false, unique = true)
    private String clientName;

    @Column(name = "server_name")
    private String serverName;

    @Column(name = "config_file")
    private String configFile;

    @Column(name = "qr_code_path")
    private String qrCodePath;

    @Column(name = "is_assigned")
    private boolean isAssigned;

    @Column(name = "chat_id", nullable = true)
    private String chatId;

    @Column(name = "username", nullable = true)
    private String username;

    @Column(name = "client_public_key", nullable = true)
    private String clientPublicKey;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "assigned_at", nullable = true)
    private LocalDateTime assignedAt;

    @Column(name = "reserved_until")
    private LocalDateTime reservedUntil;

    @Column(name = "blockedUntil")
    private LocalDateTime blockedUntil;

    @Column(name = "os_type")
    private String osType;

    @Column(name = "plan")
    private String plan;


    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public LocalDateTime getBlockedUntil() {
        return blockedUntil;
    }

    public void setBlockedUntil(LocalDateTime blockedUntil) {
        this.blockedUntil = blockedUntil;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public String getQrCodePath() {
        return qrCodePath;
    }

    public void setQrCodePath(String qrCodePath) {
        this.qrCodePath = qrCodePath;
    }

    public boolean isAssigned() {
        return isAssigned;
    }

    public void setAssigned(boolean assigned) {
        isAssigned = assigned;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getClientPublicKey() {
        return clientPublicKey;
    }

    public void setClientPublicKey(String clientPublicKey) {
        this.clientPublicKey = clientPublicKey;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getReservedUntil() {
        return reservedUntil;
    }

    public void setReservedUntil(LocalDateTime reservedUntil) {
        this.reservedUntil = reservedUntil;
    }
}
