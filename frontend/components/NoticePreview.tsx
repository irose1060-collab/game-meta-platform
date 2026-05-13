"use client";

import type { Notice } from "@/types";

const NOTICES: Notice[] = [
  {
    id: 1,
    cat: "업데이트",
    title: "14.21 패치 데이터 반영 완료 안내",
    date: "2026-05-04",
    content:
      "14.21 패치에 따른 챔피언 통계, 티어 데이터가 모두 반영되었습니다. 일부 챔피언의 승률 산정 방식이 개선되었으며, 상세 변경 내역은 패치 정보 페이지에서 확인하실 수 있습니다.",
  },
  {
    id: 2,
    cat: "점검",
    title: "5월 8일 새벽 정기 서버 점검 안내",
    date: "2026-05-03",
    content:
      "5월 8일 04:00 ~ 06:00 (KST) 동안 서버 정기 점검이 진행됩니다. 점검 시간 중에는 전적 검색 및 통계 분석 기능 이용이 일시적으로 제한될 수 있습니다.",
  },
  {
    id: 3,
    cat: "공지",
    title: "AI 피드백 베타 서비스 사전 신청 오픈",
    date: "2026-05-01",
    content:
      "개인 맞춤형 AI 피드백 베타 서비스의 사전 신청이 오픈되었습니다. 로그인 후 마이페이지에서 신청하실 수 있으며, 선정된 분께는 개별 안내 메일이 발송됩니다.",
  },
];

type NoticePreviewProps = {
  onNoticeClick: (notice: Notice) => void;
};

export default function NoticePreview({ onNoticeClick }: NoticePreviewProps) {
  return (
    <section className="container">
      <div className="section-head">
        <h2>NOTICE</h2>
        <div className="line" />
        <small>공지사항</small>
      </div>

      <div className="notice-list">
        {NOTICES.map((notice) => (
          <button
            key={notice.id}
            className="notice-item"
            onClick={() => onNoticeClick(notice)}
          >
            <div className="notice-left">
              <span className="notice-cat">{notice.cat || notice.category || "공지"}</span>
              <span className="notice-title">{notice.title}</span>
            </div>
            <span className="notice-date">{notice.date}</span>
          </button>
        ))}
      </div>

      <div className="view-all">
        <button onClick={() => alert("공지 목록 페이지로 이동합니다.")}>
          전체 공지 보기 →
        </button>
      </div>
    </section>
  );
}
