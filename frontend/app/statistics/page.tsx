"use client";

import { useCurrentUser } from "@/hooks/useCurrentUser";
import { useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
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

function formatPercent2(value: number) {
  return `${value.toFixed(2)}%`;
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
  const { user, logout, goHomeForAuth } = useCurrentUser();

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

  const positionMaxPercentage = useMemo(() => {
    const values = overview?.positionDistribution.map((item) => item.percentage) ?? [];
    return Math.max(1, ...values);
  }, [overview]);

  const scatterStats = useMemo(() => {
    const points = overview?.scatterChampions ?? [];

    if (points.length === 0) {
      return {
        points,
        maxPickRate: 1,
        minWinRate: 40,
        maxWinRate: 60,
        avgPickRate: 0,
        avgWinRate: 0,
        maxGames: 1,
      };
    }

    const maxPickRate = Math.max(1, ...points.map((item) => item.pickRate));
    const rawMinWinRate = Math.min(...points.map((item) => item.winRate));
    const rawMaxWinRate = Math.max(...points.map((item) => item.winRate));
    const minWinRate = Math.max(0, Math.floor(rawMinWinRate - 4));
    const maxWinRate = Math.min(100, Math.ceil(rawMaxWinRate + 4));
    const avgPickRate = points.reduce((sum, item) => sum + item.pickRate, 0) / points.length;
    const avgWinRate = points.reduce((sum, item) => sum + item.winRate, 0) / points.length;
    const maxGames = Math.max(1, ...points.map((item) => item.games));

    return {
      points,
      maxPickRate,
      minWinRate,
      maxWinRate,
      avgPickRate,
      avgWinRate,
      maxGames,
    };
  }, [overview]);

  return (
    <>
      <Header
        user={user}
        onLoginClick={goHomeForAuth}
        onSignupClick={goHomeForAuth}
        onLogoutClick={logout}
      />

      <main className={styles.page}>
        <section className={styles.hero}>
          <div className={styles.container}>
            <div className={styles.heroInner}>
              <p className={styles.eyebrow}>META GG ANALYTICS</p>
              <h1 className={styles.title}>메타 통계 분석 대시보드</h1>
              <p className={styles.description}>
                챔피언 티어표가 결과를 보여준다면, 통계 분석 페이지는 수집된
                데이터의 규모, 포지션별 메타 다양성, 지표별 상위 챔피언, 승률과
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
                  <Panel
                    title="포지션별 메타 다양성"
                    sub={`최소 ${overview.summary.minGames}게임 이상 챔피언 분포`}
                  >
                    <div className={styles.positionGuide}>
                      참가자 수가 아니라, 최신 패치에서 최소 표본을 충족한 챔피언 수 기준입니다.
                    </div>

                    <div className={styles.positionBarList}>
                      {overview.positionDistribution.map((item) => {
                        const visualWidth =
                          positionMaxPercentage === 0
                            ? 0
                            : (item.percentage / positionMaxPercentage) * 100;

                        return (
                          <div key={item.position} className={styles.positionBarRow}>
                            <div className={styles.positionName}>
                              {POSITION_LABELS[item.position] ?? item.position}
                            </div>
                            <div className={styles.barTrack}>
                              <div
                                className={styles.barFill}
                                style={{
                                  width: `${Math.max(4, Math.min(100, visualWidth))}%`,
                                }}
                              />
                            </div>
                            <div className={styles.positionMeta}>
                              {formatNumber(item.pickCount)}개 · {formatPercent(item.percentage)}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </Panel>

                  <Panel
                    title="승률 × 픽률 메타 맵"
                    sub="점 크기 = 게임 수 / 오른쪽 위 = 핵심 메타"
                  >
                    <MetaScatterMap
                      champions={scatterStats.points}
                      nameMap={nameMap}
                      maxPickRate={scatterStats.maxPickRate}
                      minWinRate={scatterStats.minWinRate}
                      maxWinRate={scatterStats.maxWinRate}
                      avgPickRate={scatterStats.avgPickRate}
                      avgWinRate={scatterStats.avgWinRate}
                      maxGames={scatterStats.maxGames}
                    />
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
  children: ReactNode;
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


function MetaScatterMap({
  champions,
  nameMap,
  maxPickRate,
  minWinRate,
  maxWinRate,
  avgPickRate,
  avgWinRate,
  maxGames,
}: {
  champions: AnalyticsChampion[];
  nameMap: ChampionNameMap;
  maxPickRate: number;
  minWinRate: number;
  maxWinRate: number;
  avgPickRate: number;
  avgWinRate: number;
  maxGames: number;
}) {
  const yRange = maxWinRate - minWinRate || 1;
  const avgX = Math.min(94, Math.max(6, (avgPickRate / maxPickRate) * 88 + 6));
  const avgY = Math.min(
    90,
    Math.max(10, ((avgWinRate - minWinRate) / yRange) * 76 + 12)
  );

  if (champions.length === 0) {
    return <div className={styles.scatterEmpty}>산점도에 표시할 챔피언 데이터가 없습니다.</div>;
  }

  return (
    <div className={styles.scatterShell}>
      <div className={styles.scatterLegend}>
        <span>가로축: 픽률</span>
        <span>세로축: 승률</span>
        <span>점 크기: 게임 수</span>
      </div>

      <div className={styles.scatterWrap}>
        <div className={styles.scatterQuadrantTopLeft}>숨은 고승률 픽</div>
        <div className={styles.scatterQuadrantTopRight}>핵심 메타 픽</div>
        <div className={styles.scatterQuadrantBottomRight}>인기 대비 위험 픽</div>

        <div className={styles.scatterAvgVertical} style={{ left: `${avgX}%` }} />
        <div className={styles.scatterAvgHorizontal} style={{ bottom: `${avgY}%` }} />

        <div className={styles.scatterYAxisLabel}>승률</div>
        <div className={styles.scatterXAxisLabel}>픽률</div>

        {champions.map((champion) => {
          const x = Math.min(94, Math.max(6, (champion.pickRate / maxPickRate) * 88 + 6));
          const y = Math.min(
            90,
            Math.max(10, ((champion.winRate - minWinRate) / yRange) * 76 + 12)
          );
          const size = Math.round(7 + Math.sqrt(champion.games / Math.max(1, maxGames)) * 11);
          const tooltipClassName = [
            styles.scatterTooltip,
            x > 68 ? styles.scatterTooltipLeft : "",
            y > 68 ? styles.scatterTooltipDown : "",
          ]
            .filter(Boolean)
            .join(" ");

          return (
            <div
              key={`${champion.position}-${champion.championId}-${champion.championName}`}
              className={styles.scatterPoint}
              style={{
                left: `${x}%`,
                bottom: `${y}%`,
                width: size,
                height: size,
              }}
            >
              <div className={tooltipClassName}>
                <div className={styles.tooltipName}>
                  {getKoreanChampionName(champion.championName, nameMap)}
                </div>
                <div className={styles.tooltipSub}>
                  {POSITION_LABELS[champion.position] ?? champion.position} · {champion.tier} 티어
                </div>
                <div className={styles.tooltipGrid}>
                  <span>승률</span>
                  <b>{formatPercent2(champion.winRate)}</b>
                  <span>픽률</span>
                  <b>{formatPercent2(champion.pickRate)}</b>
                  <span>게임 수</span>
                  <b>{formatNumber(champion.games)}</b>
                  <span>티어 점수</span>
                  <b>{champion.tierScore.toFixed(2)}</b>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      <div className={styles.scatterAxisFooter}>
        <span>낮은 픽률</span>
        <span>높은 픽률</span>
      </div>
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
