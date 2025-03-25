package com.nicednb.svrgate.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SourceObjectDto {
    private Long id;
    private String name;
    private String type; // SERVER, GENERAL, NETWORK
    private String ipAddress;
    private String zone; // Zone 이름 (있는 경우)
}