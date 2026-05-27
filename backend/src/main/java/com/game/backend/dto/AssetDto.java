package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssetDto {
    private String id;
    private String name;
    private String description;
    private String imageUrl;
}
