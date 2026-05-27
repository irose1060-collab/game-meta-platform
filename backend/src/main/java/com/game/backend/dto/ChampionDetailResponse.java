package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChampionDetailResponse {
    private String id;
    private String key;
    private String nameKr;
    private String title;
    private String lore;
    private String blurb;
    private String imageUrl;
    private List<String> tags;
    private String partype;
    private String position;
    private String tier;
    private Double winRate;
    private Double pickRate;
    private Double banRate;
    private Info info;
    private Stats stats;
    private Passive passive;
    private List<Skill> spells;
    private List<String> allytips;
    private List<String> enemytips;
    private Recommendation recommendation;

    @Getter @Builder
    public static class Info {
        private Integer attack;
        private Integer defense;
        private Integer magic;
        private Integer difficulty;
    }

    @Getter @Builder
    public static class Stats {
        private Double hp;
        private Double hpPerLevel;
        private Double mp;
        private Double mpPerLevel;
        private Double moveSpeed;
        private Double armor;
        private Double armorPerLevel;
        private Double spellBlock;
        private Double attackRange;
        private Double attackDamage;
        private Double attackSpeed;
    }

    @Getter @Builder
    public static class Passive {
        private String name;
        private String description;
        private String imageUrl;
    }

    @Getter @Builder
    public static class Skill {
        private String id;
        private String name;
        private String description;
        private String cooldown;
        private String cost;
        private String imageUrl;
    }

    @Getter @Builder
    public static class Recommendation {
        private List<AssetDto> summonerSpells;
        private List<AssetDto> runes;
        private List<AssetDto> items;
        private List<AssetDto> counters;
        private String note;
    }
}
