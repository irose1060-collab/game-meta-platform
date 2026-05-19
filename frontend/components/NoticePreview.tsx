"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import type { Notice, NoticeResponse } from "@/types";

type NoticePreviewProps = {
  onNoticeClick: (notice: Notice) => void;
};

export default function NoticePreview({ onNoticeClick }: NoticePreviewProps) {
  const router = useRouter();
  const [notices, setNotices] = useState<NoticeResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchRecentNotices();
  }, []);

  const fetchRecentNotices = async () => {
    setLoading(true);

    try {
      const data = await apiFetch<NoticeResponse[]>("/api/notices/recent");
      setNotices(data);
    } catch {
      setNotices([]);
    } finally {
      setLoading(false);
    }
  };

  const handleNoticeClick = (notice: NoticeResponse) => {
    onNoticeClick({
      id: notice.id,
      title: notice.title,
      content: notice.content,
      date: notice.createdAt?.slice(0, 10) || "-",
      cat: notice.isPinned ? "중요" : "공지",
    });
  };

  return (
    <section className="container">
      <div className="section-head">
        <h2>NOTICE</h2>
        <div className="line" />
        <small>공지사항</small>
      </div>

      <div className="notice-list">
        {loading && (
          <div className="notice-item">
            <div className="notice-left">
              <span className="notice-title">공지사항을 불러오는 중...</span>
            </div>
          </div>
        )}

        {!loading && notices.length === 0 && (
          <div className="notice-item">
            <div className="notice-left">
              <span className="notice-title">등록된 공지사항이 없습니다.</span>
            </div>
          </div>
        )}

        {notices.map((notice) => (
          <button key={notice.id} className="notice-item" onClick={() => handleNoticeClick(notice)}>
            <div className="notice-left">
              <span className="notice-cat">{notice.isPinned ? "중요" : "공지"}</span>
              <span className="notice-title">{notice.title}</span>
            </div>
            <span className="notice-date">{notice.createdAt?.slice(0, 10)}</span>
          </button>
        ))}
      </div>

      <div className="view-all">
        <button onClick={() => router.push("/notices")}>전체 공지 보기 →</button>
      </div>
    </section>
  );
}
