-- META GG 통계 수집용 PostgreSQL 테이블 수동 생성 SQL
-- Spring JPA ddl-auto=update를 쓰면 Entity 기준으로 자동 생성되지만,
-- pgAdmin에서 직접 확인/생성하고 싶을 때 이 파일을 사용하면 된다.

CREATE TABLE IF NOT EXISTS matches (
    id BIGSERIAL PRIMARY KEY,
    match_id VARCHAR(100) UNIQUE NOT NULL,
    game_version VARCHAR(50),
    queue_id INTEGER,
    game_creation BIGINT,
    game_duration INTEGER,
    platform_id VARCHAR(30),
    winning_team_id INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_matches_queue_version ON matches(queue_id, game_version);
CREATE INDEX IF NOT EXISTS idx_matches_game_creation ON matches(game_creation);

CREATE TABLE IF NOT EXISTS match_participants (
    id BIGSERIAL PRIMARY KEY,
    match_id VARCHAR(100) NOT NULL,
    puuid TEXT NOT NULL,
    riot_game_name VARCHAR(100),
    riot_tag_line VARCHAR(30),
    summoner_name VARCHAR(100),
    champion_id INTEGER,
    champion_name VARCHAR(100),
    team_id INTEGER,
    team_position VARCHAR(30),
    individual_position VARCHAR(30),
    win BOOLEAN,
    kills INTEGER,
    deaths INTEGER,
    assists INTEGER,
    total_damage_dealt_to_champions INTEGER,
    total_damage_taken INTEGER,
    gold_earned INTEGER,
    total_minions_killed INTEGER,
    neutral_minions_killed INTEGER,
    vision_score INTEGER,
    wards_placed INTEGER,
    wards_killed INTEGER,
    summoner1_id INTEGER,
    summoner2_id INTEGER,
    item0 INTEGER,
    item1 INTEGER,
    item2 INTEGER,
    item3 INTEGER,
    item4 INTEGER,
    item5 INTEGER,
    item6 INTEGER,
    primary_style_id INTEGER,
    sub_style_id INTEGER,
    main_rune_id INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_match_participant_match_puuid UNIQUE(match_id, puuid)
);

CREATE INDEX IF NOT EXISTS idx_participants_champion_position ON match_participants(champion_id, team_position);
CREATE INDEX IF NOT EXISTS idx_participants_puuid ON match_participants(puuid);
CREATE INDEX IF NOT EXISTS idx_participants_match_id ON match_participants(match_id);

CREATE TABLE IF NOT EXISTS champion_stats (
    id BIGSERIAL PRIMARY KEY,
    patch VARCHAR(50),
    queue_id INTEGER,
    position VARCHAR(30),
    champion_id INTEGER,
    champion_name VARCHAR(100),
    games INTEGER,
    wins INTEGER,
    win_rate DOUBLE PRECISION,
    pick_rate DOUBLE PRECISION,
    avg_kda DOUBLE PRECISION,
    avg_damage DOUBLE PRECISION,
    avg_gold DOUBLE PRECISION,
    avg_cs DOUBLE PRECISION,
    avg_vision_score DOUBLE PRECISION,
    tier_score DOUBLE PRECISION,
    tier VARCHAR(10),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_champion_stats_scope UNIQUE(patch, queue_id, position, champion_id)
);

CREATE INDEX IF NOT EXISTS idx_champion_stats_position_tier ON champion_stats(position, tier);
CREATE INDEX IF NOT EXISTS idx_champion_stats_score ON champion_stats(tier_score);
