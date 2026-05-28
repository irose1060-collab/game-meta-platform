# META GG 데이터 수집 기능 사용법

## 1. 추가된 핵심 기능

- Riot ID로 최근 솔로랭크 매치 ID 조회
- Match-V5 상세 JSON 조회
- `matches` 테이블에 경기 단위 저장
- `match_participants` 테이블에 참가자 10명 단위 저장
- `champion_stats` 테이블에 챔피언별 승률/픽률/KDA/딜량/티어 점수 집계
- `match_id` 중복 저장 방지
- 429 Rate Limit 발생 시 `Retry-After` 기준 1회 재시도

## 2. application.properties 추가 설정

```properties
riot.match.base-url=https://asia.api.riotgames.com
riot.request-delay-ms=1300
```

개발용 Riot personal key는 100 requests / 2 minutes 제한이 있으므로 `1300ms` 정도로 천천히 호출하는 것을 기본값으로 잡았다.

## 3. Postman 테스트 순서

### 계정 PUUID 확인

```http
GET http://localhost:8080/api/riot/account?gameName=Hide%20on%20bush&tagLine=KR1
```

### 최근 매치 수집

```http
POST http://localhost:8080/api/riot/collect?gameName=Hide%20on%20bush&tagLine=KR1&count=10
```

처음에는 `count=3` 정도로 테스트하고, 정상 저장 확인 후 `count=10`으로 늘리는 것을 추천한다.

### 챔피언 통계 재집계

```http
POST http://localhost:8080/api/riot/stats/rebuild
```

### 포지션별 챔피언 통계 조회

```http
GET http://localhost:8080/api/riot/stats/champions?position=MIDDLE
GET http://localhost:8080/api/riot/stats/champions?position=JUNGLE
GET http://localhost:8080/api/riot/stats/champions?position=TOP
GET http://localhost:8080/api/riot/stats/champions?position=BOTTOM
GET http://localhost:8080/api/riot/stats/champions?position=UTILITY
```

## 4. pgAdmin에서 확인할 SQL

```sql
SELECT COUNT(*) FROM matches;
SELECT COUNT(*) FROM match_participants;
SELECT * FROM matches ORDER BY id DESC LIMIT 10;
SELECT * FROM match_participants ORDER BY id DESC LIMIT 20;
SELECT * FROM champion_stats ORDER BY tier_score DESC LIMIT 30;
```

## 5. 수집 기준

현재 수집 기준은 MVP 안정성을 위해 다음으로 고정했다.

- Region route: ASIA
- Queue: 420, 솔로랭크
- 한 번 수집: 최대 20경기
- 경기 1개당 참가자 10명 저장

## 6. 주의

- `.backup`, `.sql`, Riot API Key, DB password는 GitHub에 올리지 말 것.
- DB는 Git이 아니라 `pg_dump` / `pg_restore`로 옮길 것.
