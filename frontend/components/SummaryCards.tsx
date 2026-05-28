"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import type { HomeMetaResponse } from "@/types";

export default function SummaryCards() {
  const router = useRouter();
  const [patchOpen, setPatchOpen] = useState(false);
  const [meta, setMeta] = useState<HomeMetaResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    fetchHomeMeta();<div className="card" onClick={() => router.push("/statistics")}></div>
  }, []);

  const fetchHomeMeta = async () => {
    setLoading(true);
    setError("");

    try {
      const data = await apiFetch<HomeMetaResponse>("/api/home/meta");
      setMeta(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "오늘의 메타 정보를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <section className="container">
        <div className="section-head">
          <h2>TODAY&apos;S META</h2>
          <div className="line" />
          <small>백엔드 API 로딩 중</small>
        </div>
        <div className="notice-board-empty">홈 메타 정보를 백엔드 API에서 불러오는 중입니다...</div>
      </section>
    );
  }

  if (error || !meta) {
    return (
      <section className="container">
        <div className="section-head">
          <h2>TODAY&apos;S META</h2>
          <div className="line" />
          <small>API 오류</small>
        </div>
        <p className="error-message">{error || "오늘의 메타 정보를 표시할 수 없습니다."}</p>
        <button className="btn btn-gold" onClick={fetchHomeMeta}>다시 불러오기</button>
      </section>
    );
  }

  return (
    <section className="container">
      <div className="section-head">
        <h2>TODAY&apos;S META</h2>
        <div className="line" />
        <small>Riot Data Dragon + 백엔드 API 기반</small>
      </div>

      <div className="cards">
        <div className="card" onClick={() => router.push("/champions")}>
          <div className="card-tag">Hot Champion</div>
          <h3>오늘의 인기 챔피언</h3>
          <div className="body">
            <div className="champ-row">
              <div className="champ-portrait champ-portrait-image">
                {meta.hotChampion.imageUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={meta.hotChampion.imageUrl} alt={meta.hotChampion.nameKr} />
                ) : (
                  meta.hotChampion.nameKr.slice(0, 1)
                )}
              </div>
              <div>
                <div style={{ fontSize: 16, color: "#fff", fontWeight: 700 }}>
                  {meta.hotChampion.nameKr || meta.hotChampion.name}
                </div>
                <div style={{ color: "var(--text-dim)", fontSize: 12 }}>
                  {meta.hotChampion.position} · {meta.hotChampion.source || "API DATA"}
                </div>
              </div>
            </div>
            <div className="champ-stats">
              <div><small>승률</small><b className="stat-win">{meta.hotChampion.winRate}%</b></div>
              <div><small>픽률</small><b>{meta.hotChampion.pickRate}%</b></div>
              <div><small>밴률</small><b>{meta.hotChampion.banRate}%</b></div>
            </div>
          </div>
        </div>

        <div className="card" onClick={(e) => e.stopPropagation()}>
          <div className="card-tag">Latest Patch</div>
          <h3>최근 패치 요약</h3>
          <div className="body">
            <span className="patch-ver">{meta.patchSummary.version}</span>
            <p style={{ fontSize: 13, color: "#c5cbd9" }}>{meta.patchSummary.summary}</p>
            <button className="patch-more" onClick={() => setPatchOpen((prev) => !prev)}>
              {patchOpen ? "접기 ▴" : "더보기 ▾"}
            </button>
            <div className={patchOpen ? "patch-detail open" : "patch-detail"}>
              <ul>
                <li>{meta.patchSummary.detail1}</li>
                <li>{meta.patchSummary.detail2}</li>
                <li>{meta.patchSummary.detail3}</li>
              </ul>
            </div>
          </div>
        </div>

        <div className="card" onClick={() => router.push("/statistics")}>
          <div className="card-tag">Team Composition</div>
          <h3>팀 조합 분석</h3>
          <div className="body">
            <div className="meter"><div className="label">AP 비율</div><div className="bar"><i style={{ width: `${meta.teamCompSummary.apRatio}%` }} /></div><div className="v">{meta.teamCompSummary.apStatus}</div></div>
            <div className="meter"><div className="label">CC 보유량</div><div className="bar"><i style={{ width: `${meta.teamCompSummary.ccScore}%` }} /></div><div className="v">{meta.teamCompSummary.ccStatus}</div></div>
            <div className="meter"><div className="label">예상 승률</div><div className="bar"><i style={{ width: `${meta.teamCompSummary.expectedWinRate}%` }} /></div><div className="v">{meta.teamCompSummary.expectedWinRate}%</div></div>
            <div style={{ marginTop: 10, fontSize: 12, color: "var(--teal-2)" }}>API 기반 분석 보기 →</div>
          </div>
        </div>

        <div className="card" onClick={() => router.push("/statistics")}>
          <span className="ai-badge">AI</span>
          <div className="card-tag">Personal AI</div>
          <h3>개인 AI 피드백</h3>
          <div className="body">
            <ul className="ai-bullets">
              <li>{meta.aiFeedbackSummary.feedback1}</li>
              <li>{meta.aiFeedbackSummary.feedback2}</li>
              <li>{meta.aiFeedbackSummary.feedback3}</li>
            </ul>
          </div>
        </div>
      </div>
    </section>
  );
}
