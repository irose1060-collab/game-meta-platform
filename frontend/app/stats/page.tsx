"use client";

import { useRouter } from "next/navigation";

export default function Page() {
  const router = useRouter();

  return (
    <main className="notice-page">
      <section className="notice-hero">
        <span className="eyebrow">ANALYSIS DASHBOARD</span>
        <h1>통계 분석</h1>
        <p>팀 조합 분석, 예상 승률, 개인 AI 피드백을 제공하는 분석 페이지입니다.</p>
      </section>

      <section className="notice-layout">
        <div className="notice-list-panel">
          <div className="card-tag">Coming Soon</div>
          <h2>기능 구현 예정</h2>
          <div className="notice-empty">
            상담 후 우선순위에 따라 상세 기능을 구현할 예정입니다.
          </div>

          <button className="btn btn-gold" onClick={() => router.push("/")}>
            홈으로
          </button>
        </div>

        <div className="notice-detail-panel">
          <h2>통계 분석 설명</h2>
          <p className="notice-detail-content">팀 조합 분석, 예상 승률, 개인 AI 피드백을 제공하는 분석 페이지입니다.</p>
        </div>
      </section>
    </main>
  );
}
