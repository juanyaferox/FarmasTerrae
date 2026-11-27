package com.iyg16260.farmasterrae.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name = "passwordResetTokens")
@Getter @Setter @NoArgsConstructor
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 64) private String tokenHash;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "userId", nullable = false) private User user;
    @Column(nullable = false) private Instant expiresAt;
    private Instant usedAt;
    @Column(nullable = false) private Instant createdAt;
}
