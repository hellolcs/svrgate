package com.nicednb.svrgate.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "system_settings")
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "setting_key", nullable = false, unique = true)
    private String key;
    
    @Column(name = "setting_value")
    private String value;
    
    @Column(name = "setting_description")
    private String description;
    
    @Column(name = "setting_group")
    private String group;
}