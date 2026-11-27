package com.iyg16260.farmasterrae.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "persistentLogins")
@Getter @Setter @NoArgsConstructor
public class PersistentLogin {
    @Id @Column(length = 64) private String series;
    @Column(nullable = false, length = 64) private String username;
    @Column(nullable = false, length = 64) private String tokenValue;
    @Column(nullable = false) private LocalDateTime lastUsed;
}
