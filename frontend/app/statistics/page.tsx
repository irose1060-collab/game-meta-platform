"use client";

import { useEffect, useMemo, useState } from "react";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import { fetchAnalyticsOverview } from "@/lib/analyticsApi";
import {
  fetchKoreanChampionNameMap,
  getKoreanChampionName,
  type ChampionNameMap,
} from "@/lib/ddragonChampionApi";
import type { AnalyticsChampion, AnalyticsOverview } from "@/types/analytics";
import styles from "./StatisticsPage.module.css";

const DDRAGON_VERSION =
  process.env.NEXT_PUBLIC_DDRAGON_VERSION ?? "15.24.1";

const POSITION_LABELS: Record<string, string> = {
  TOP: "탑",
  JUNGLE: "정글",
  MIDDLE: "미드",
  BOTTOM: "원딜",
  UTILITY: "서포터",
};

function formatNumber(value: number) {
  return new Intl.NumberFormat("ko-KR").format(Math.round(value));
}

function formatPercent(value: number) {
  return `${value.toFixed(1)}%`;
}

function championImage(championName: string) {
  return `https://ddragon.leagueoflegends.com/cdn/${DDRAGON_VERSION}/img/champion/${championName}.png`;
}

export default function StatisticsPage() {
  const [overview, setOverview] = useState<AnalyticsOverview | null>(null);
  const [nameMap, setNameMap] = useState<ChampionNameMap>({});
  const [minGames, setMinGames] = useState(10);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function loadOverview(nextMinGames = minGames) {
    try {
      setLoading(true);
      setError("");

      const data = await fetchAnalyticsOverview({
        minGames: nextMinGames,
      });

      setOverview(data);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "통계 분석 데이터를 불러오지 못했습니다."
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadOverview(10);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    let ignore = false;

    async function loadNames() {
      try {
        const data = await fetchKoreanChampionNameMap();

        if (!ignore) {
          setNameMap(data);
        }
      } catch {
        if (!ignore) {
          setNameMap({});
        }
      }
    }

    loadNames();

    return () => {
      ignore = true;
    };
  }, []);

  const scatterBounds = useMemo(() => {
    const points = overview?.scatterChampions ?? [];

    const maxPickRate = Math.max(1, ...points.map((item) => item.pickRate));
    const minWinRate = Math.min(45, ...points.map((item) => item.winRate));
    const maxWinRate = Math.max(55, ...points.map((item) => item.winRate));

    return {
      maxPickRate,
      minWinRate,
      maxWinRate,
    };
  }, [overview]);

  return (
    <>
      <Header />

      <main className={styles.page}>
        <section className={styles.hero}>
          <div className={styles.container}>
            <div className={styles.heroInner}>
              <p className={styles.eyebrow}>META GG ANALYTICS</p>
              <h1 className={styles.title}>메타 통계 분석 대시보드</h1>
              <p className={styles.description}>
                챔피언 티어표가 결과를 보여준다면, 통계 분석 페이지는 수집된
                데이터의 규모, 포지션별 표본 균형, 지표별 상위 챔피언, 승률과
                픽률의 관계를 시각화합니다.
              </p>
            </div>
          </div>
        </section>

        <section className={styles.content}>
          <div className={styles.container}>
            <div className={styles.toolbar}>
              <div>
                <select
                  value={minGames}
                  onChange={(event) => {
                    const value = Number(event.target.value);
                    setMinGames(value);
                    loadOverview(value);
                  }}
                  className={styles.select}
                >
                  <option value={3}>최소 3게임 이상</option>
                  <option value={5}>최소 5게임 이상</option>
                  <option value={10}>최소 10게임 이상</option>
                  <option value={20}>최소 20게임 이상</option>
                  <option value={30}>최소 30게임 이상</option>
                </select>
              </div>

              <button
                type="button"
                onClick={() => loadOverview(minGames)}
                className={styles.reloadButton}
              >
                새로고침
              </button>
            </div>

            {loading && <div className={styles.stateBox}>통계 데이터를 불러오는 중입니다.</div>}
            {!loading && error && <div className={styles.stateBox}>{error}</div>}

            {!loading && !error && overview && (
              <>
                <div className={styles.summaryGrid}>
                  <SummaryCard label="수집 경기" value={formatNumber(overview.summary.matchCount)} />
                  <SummaryCard label="참가자 데이터" value={formatNumber(overview.summary.participantCount)} />
                  <SummaryCard label="통계 Row" value={formatNumber(overview.summary.championStatCount)} />
                  <SummaryCard label="Seed 유저" value={formatNumber(overview.summary.seedPlayerCount)} />
                  <SummaryCard label="랭킹 유저" value={formatNumber(overview.summary.rankingPlayerCount)} />
                  <SummaryCard label="분석 패치" value={overview.summary.latestPatch || "-"} />
                </div>

                <div className={styles.gridTwo}>
                  <Panel title="포지션별 표본 분포" sub="최신 패치 기준 참가자 포지션 분포">
                    <div className={styles.positionBarList}>
                      {overview.positionDistribution.map((item) => (
                        <div key={item.position} className={styles.positionBarRow}>
                          <div className={styles.positionName}>
                            {POSITION_LABELS[item.position] ?? item.position}
                          </div>
                          <div className={styles.barTrack}>
                            <div
                              className={styles.barFill}
                              style={{ width: `${Math.min(100, item.percentage)}%` }}
                            />
                          </div>
                          <div className={styles.positionMeta}>
                            {formatNumber(item.pickCount)} · {formatPercent(item.percentage)}
                          </div>
                        </div>
                      ))}
                    </div>
                  </Panel>

                  <Panel title="승률 × 픽률 산점도" sub="오른쪽 위일수록 메타 핵심 챔피언">
                    <div className={styles.scatterWrap}>
                      {overview.scatterChampions.map((champion) => {
                        const x = (champion.pickRate / scatterBounds.maxPickRate) * 92 + 4;
                        const yRange = scatterBounds.maxWinRate - scatterBounds.minWinRate || 1;
                        const y =
                          ((champion.winRate - scatterBounds.minWinRate) / yRange) * 86 + 7;

                        return (
                          <div
                            key={`${champion.position}-${champion.championId}-${champion.championName}`}
                            className={styles.scatterPoint}
                            style={{
                              left: `${x}%`,
                              bottom: `${y}%`,
                            }}
                          >
                            <div className={styles.scatterTooltip}>
                              <strong>
                                {getKoreanChampionName(champion.championName, nameMap)}
                              </strong>
                              <br />
                              {champion.position} · 승률 {formatPercent(champion.winRate)}
                              <br />
                              픽률 {formatPercent(champion.pickRate)} · {champion.games}게임
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </Panel>
                </div>

                <Panel title="포지션별 TOP 5 챔피언" sub="티어 점수 기준">
                  <div className={styles.positionTopGrid}>
                    {overview.positionTopChampions.map((group) => (
                      <div key={group.position} className={styles.positionCard}>
                        <div className={styles.positionCardTitle}>
                          {POSITION_LABELS[group.position] ?? group.position}
                        </div>
                        <div className={styles.champMiniList}>
                          {group.champions.length === 0 ? (
                            <div className={styles.champMini}>
                              <span className={styles.champMeta}>데이터 부족</span>
                            </div>
                          ) : (
                            group.champions.map((champion) => (
                              <ChampionMini
                                key={`${group.position}-${champion.championId}-${champion.championName}`}
                                champion={champion}
                                nameMap={nameMap}
                              />
                            ))
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </Panel>

                <div className={styles.rankingGrid}>
                  <RankingPanel
                    title="승률 TOP 10"
                    champions={overview.topWinRateChampions}
                    nameMap={nameMap}
                    value={(champion) => formatPercent(champion.winRate)}
                  />
                  <RankingPanel
                    title="픽률 TOP 10"
                    champions={overview.topPickRateChampions}
                    nameMap={nameMap}
                    value={(champion) => formatPercent(champion.pickRate)}
                  />
                  <RankingPanel
                    title="딜량 TOP 10"
                    champions={overview.topDamageChampions}
                    nameMap={nameMap}
                    value={(champion) => formatNumber(champion.avgDamage)}
                  />
                  <RankingPanel
                    title="KDA TOP 10"
                    champions={overview.topKdaChampions}
                    nameMap={nameMap}
                    value={(champion) => champion.avgKda.toFixed(2)}
                  />
                </div>
              </>
            )}
          </div>
        </section>
      </main>

      <Footer />
    </>
  );
}

function SummaryCard({ label, value }: { label: string; value: string }) {
  return (
    <div className={styles.summaryCard}>
      <div className={styles.summaryLabel}>{label}</div>
      <div className={styles.summaryValue}>{value}</div>
    </div>
  );
}

function Panel({
  title,
  sub,
  children,
}: {
  title: string;
  sub?: string;
  children: React.ReactNode;
}) {
  return (
    <section className={styles.panel}>
      <div className={styles.panelHeader}>
        <h2 className={styles.panelTitle}>{title}</h2>
        {sub && <div className={styles.panelSub}>{sub}</div>}
      </div>
      <div className={styles.panelBody}>{children}</div>
    </section>
  );
}

function ChampionMini({
  champion,
  nameMap,
}: {
  champion: AnalyticsChampion;
  nameMap: ChampionNameMap;
}) {
  return (
    <div className={styles.champMini}>
      <img
        src={championImage(champion.championName)}
        alt={champion.championName}
        className={styles.champIcon}
      />
      <div>
        <div className={styles.champName}>
          {getKoreanChampionName(champion.championName, nameMap)}
        </div>
        <div className={styles.champMeta}>
          승률 {formatPercent(champion.winRate)} · {champion.games}게임
        </div>
      </div>
      <span className={styles.tierBadge}>{champion.tier}</span>
    </div>
  );
}

function RankingPanel({
  title,
  champions,
  nameMap,
  value,
}: {
  title: string;
  champions: AnalyticsChampion[];
  nameMap: ChampionNameMap;
  value: (champion: AnalyticsChampion) => string;
}) {
  return (
    <Panel title={title}>
      <div className={styles.rankList}>
        {champions.map((champion, index) => (
          <div
            key={`${title}-${champion.position}-${champion.championId}-${champion.championName}`}
            className={styles.rankRow}
          >
            <div className={styles.rankNo}>#{index + 1}</div>
            <div>
              <div className={styles.champName}>
                {getKoreanChampionName(champion.championName, nameMap)}
              </div>
              <div className={styles.champMeta}>
                {POSITION_LABELS[champion.position] ?? champion.position} · {champion.games}게임
              </div>
            </div>
            <div className={styles.rankValue}>{value(champion)}</div>
          </div>
        ))}
      </div>
    </Panel>
  );
}
