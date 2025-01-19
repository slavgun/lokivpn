package com.lokivpn.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "vpn_clients")
public class VpnClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "config_file", nullable = false)
    private String configFile;

    @Column(name = "qr_code_path", nullable = false)
    private String qrCodePath;

    @Column(name = "assigned", nullable = false)
    private boolean assigned;

    @Column(name = "server", nullable = false)
    private String server;

    @Column(name = "user_id")
    private Long userId;

    @Transient
    private String publicKey;

}
