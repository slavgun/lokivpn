package com.lokivpn.security;

import com.lokivpn.model.Admin;
import com.lokivpn.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomAdminDetailsService implements UserDetailsService {

    @Autowired
    private AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found with username: " + username));

        return User.builder()
                .username(admin.getUsername())
                .password(admin.getPassword())
                .roles("ADMIN") // Укажите роли, если нужно
                .build();
    }

    public Long getCurrentAdminId() {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return adminRepository.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Admin not found"))
                .getId();
    }
}

