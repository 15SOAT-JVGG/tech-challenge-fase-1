package br.com.fiap.postech.soat16.fase1.auth.domain.model;

import java.time.LocalDateTime;

public class AppUser {

    private Long id;

    private String username;

    private String password;

    private String role;

    private Boolean active = true;

    private LocalDateTime createdAt;

    protected AppUser() {
    }

    public AppUser(String username, String hashedPassword, String role) {
        this.username = username;
        this.password = hashedPassword;
        this.role = role;
        this.active = true;
    }

    /**
     * Reidrata um usuário já persistido, preservando identidade e datas geradas pela infraestrutura.
     */
    public static AppUser restore(Long id, String username, String hashedPassword, String role,
            Boolean active, LocalDateTime createdAt) {
        var user = new AppUser(username, hashedPassword, role);
        user.id = id;
        user.active = active;
        user.createdAt = createdAt;
        return user;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public void changePassword(String newHashedPassword) {
        this.password = newHashedPassword;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public Boolean getActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
