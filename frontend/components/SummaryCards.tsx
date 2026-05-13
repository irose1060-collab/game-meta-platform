"use client";

import { useState } from "react";

export default function SummaryCards() {
  const [patchOpen, setPatchOpen] = useState(false);

  const goToPage = (pathName: string) => {
    alert(`[이동] ${pathName} 페이지로 이동합니다.`);
  };

  return (
    <section className="container">
      <div className="section-head">
        <h2>TODAY&apos;S META</h2>
        <div className="line" />
        <small>실시간 메타 요약</small>
      </div>

      <div className="cards">
        <div className="card" onClick={() => goToPage("/champions")}>
          <div className="card-tag">Hot Champion</div>
          <h3>오늘의 인기 챔피언</h3>
          <div className="body">
            <div className="champ-row">
              <div className="champ-portrait">A</div>
              <div>
                <div style={{ fontSize: 16, color: "#fff", fontWeight: 700 }}>아트록스</div>
                <div style={{ color: "var(--text-dim)", fontSize: 12 }}>Top · Fighter</div>
              </div>
            </div>
            <div className="champ-stats">
              <div>
                <small>승률</small>
                <b className="stat-win">53.4%</b>
              </div>
              <div>
                <small>픽률</small>
                <b>14.2%</b>
              </div>
              <div>
                <small>밴률</small>
                <b>8.7%</b>
              </div>
            </div>
          </div>
        </div>

        <div className="card" onClick={(e) => e.stopPropagation()}>
          <div className="card-tag">Latest Patch</div>
          <h3>최근 패치 요약</h3>
          <div className="body">
            <span className="patch-ver">14.21</span>
            <p style={{ fontSize: 13, color: "#c5cbd9" }}>
              정글 경험치 조정 및 바텀 라인 ADC 메타 변동. 일부 서포터 챔피언 너프 적용.
            </p>
            <button className="patch-more" onClick={() => setPatchOpen((prev) => !prev)}>
              {patchOpen ? "접기 ▴" : "더보기 ▾"}
            </button>
            <div className={patchOpen ? "patch-detail open" : "patch-detail"}>
              <ul>
                <li>아트록스 Q 스킬 데미지 5% 너프</li>
                <li>야스오 패시브 치명타 보정 조정</li>
                <li>정글 몬스터 경험치 +3%</li>
                <li>서포터 아이템 골드 효율 변경</li>
              </ul>
            </div>
          </div>
        </div>

        <div className="card" onClick={() => goToPage("/stats")}>
          <div className="card-tag">Team Composition</div>
          <h3>팀 조합 분석</h3>
          <div className="body">
            <div className="meter">
              <div className="label">AP 비율</div>
              <div className="bar"><i style={{ width: "35%" }} /></div>
              <div className="v">부족</div>
            </div>
            <div className="meter">
              <div className="label">CC 보유량</div>
              <div className="bar"><i style={{ width: "60%" }} /></div>
              <div className="v">보통</div>
            </div>
            <div className="meter">
              <div className="label">예상 승률</div>
              <div className="bar"><i style={{ width: "48%" }} /></div>
              <div className="v">48%</div>
            </div>
            <div style={{ marginTop: 10, fontSize: 12, color: "var(--teal-2)" }}>
              상세 분석 보기 →
            </div>
          </div>
        </div>

        <div className="card" onClick={() => alert("AI 피드백은 로그인 후 이용 가능합니다. 준비 중입니다.")}>
          <span className="ai-badge">AI</span>
          <div className="card-tag">Personal AI</div>
          <h3>개인 AI 피드백</h3>
          <div className="body">
            <ul className="ai-bullets">
              <li>최근 경기 기반 플레이 스타일 분석</li>
              <li>패배 원인 자동 요약 제공</li>
              <li>챔피언별 맞춤 개선 포인트</li>
            </ul>
          </div>
        </div>
      </div>
    </section>
  );
}
