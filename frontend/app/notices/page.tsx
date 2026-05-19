"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import Header from "@/components/Header";
import type { NoticeResponse, User } from "@/types";

const PAGE_SIZE = 4;

export default function NoticesPage() {
  const router = useRouter();
  const detailRef = useRef<HTMLElement | null>(null);

  const [user, setUser] = useState<User | null>(null);
  const [notices, setNotices] = useState<NoticeResponse[]>([]);
  const [selectedNotice, setSelectedNotice] = useState<NoticeResponse | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const savedUser = localStorage.getItem("metagg_user");

    if (savedUser) {
      try {
        setUser(JSON.parse(savedUser));
      } catch {
        localStorage.removeItem("metagg_user");
        localStorage.removeItem("metagg_token");
      }
    }

    fetchNotices();

    const resetUiState = () => {
      document.body.style.overflow = "";
    };

    window.addEventListener("pageshow", resetUiState);
    window.addEventListener("popstate", resetUiState);

    return () => {
      window.removeEventListener("pageshow", resetUiState);
      window.removeEventListener("popstate", resetUiState);
    };
  }, []);

  const sortedNotices = useMemo(() => {
    return [...notices].sort((a, b) => {
      if (a.isPinned !== b.isPinned) {
        return a.isPinned ? -1 : 1;
      }

      const aTime = new Date(a.createdAt || "").getTime();
      const bTime = new Date(b.createdAt || "").getTime();

      return bTime - aTime;
    });
  }, [notices]);

  const totalPages = Math.max(1, Math.ceil(sortedNotices.length / PAGE_SIZE));

  const pagedNotices = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return sortedNotices.slice(start, start + PAGE_SIZE);
  }, [sortedNotices, currentPage]);

  const fetchNotices = async () => {
    setLoading(true);
    setError("");

    try {
      const data = await apiFetch<NoticeResponse[]>("/api/notices");
      setNotices(data);

      if (data.length > 0) {
        const sorted = [...data].sort((a, b) => {
          if (a.isPinned !== b.isPinned) return a.isPinned ? -1 : 1;
          return new Date(b.createdAt || "").getTime() - new Date(a.createdAt || "").getTime();
        });
        setSelectedNotice(sorted[0]);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "공지사항을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const handleNoticeClick = async (noticeId: number) => {
    setDetailLoading(true);
    setError("");

    try {
      const data = await apiFetch<NoticeResponse>(`/api/notices/${noticeId}`);
      setSelectedNotice(data);
      setTimeout(() => {
        detailRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
      }, 100);
    } catch (err) {
      setError(err instanceof Error ? err.message : "공지 상세를 불러오지 못했습니다.");
    } finally {
      setDetailLoading(false);
    }
  };

  const handlePageChange = (page: number) => {
    if (page < 1 || page > totalPages) return;
    setCurrentPage(page);
  };

  const handleLogout = () => {
    localStorage.removeItem("metagg_user");
    localStorage.removeItem("metagg_token");
    setUser(null);
    router.push("/");
  };

  const goHome = () => {
    router.push("/");
  };

  return (
    <>
      <Header
        user={user}
        onLoginClick={goHome}
        onSignupClick={goHome}
        onLogoutClick={handleLogout}
      />

      <main className="notice-board-page">
        <section className="notice-board-hero">
          <span className="eyebrow">NOTICE CENTER</span>
          <h1>공지사항</h1>
          <p>
            META GG 서비스 업데이트, 점검 안내, 패치 데이터 반영 내역을 확인할 수 있습니다.
          </p>
        </section>

        <section className="notice-detail-full notice-detail-top-position" ref={detailRef}>
          {!selectedNotice && !detailLoading && (
            <div className="notice-detail-empty">
              <h2>공지 상세</h2>
              <p>아래 공지 목록에서 공지를 선택하면 이곳에 상세 내용이 표시됩니다.</p>
            </div>
          )}

          {detailLoading && (
            <div className="notice-detail-empty">
              <h2>불러오는 중...</h2>
              <p>공지 상세 내용을 가져오고 있습니다.</p>
            </div>
          )}

          {selectedNotice && !detailLoading && (
            <>
              <div className="notice-detail-top">
                <span className="notice-cat">
                  {selectedNotice.isPinned ? "중요" : "공지"}
                </span>

                <button className="btn btn-ghost" onClick={() => setSelectedNotice(null)}>
                  닫기
                </button>
              </div>

              <h2>{selectedNotice.title}</h2>

              <div className="notice-detail-meta">
                <span>작성일 {selectedNotice.createdAt?.slice(0, 10) || "-"}</span>
                {selectedNotice.updatedAt && (
                  <span>수정일 {selectedNotice.updatedAt.slice(0, 10)}</span>
                )}
                <span>조회수 {selectedNotice.viewCount ?? 0}</span>
              </div>

              <div className="notice-detail-content">{selectedNotice.content}</div>

              <div className="notice-detail-actions">
                <button className="btn btn-ghost" onClick={goHome}>
                  홈으로
                </button>
                <button className="btn btn-gold" onClick={() => setSelectedNotice(null)}>
                  상세 닫기
                </button>
              </div>
            </>
          )}
        </section>

        <section className="notice-board">
          <div className="notice-board-top">
            <div>
              <div className="card-tag">NOTICE LIST</div>
              <h2>전체 공지</h2>
            </div>

            <button className="btn btn-gold" onClick={fetchNotices}>
              새로고침
            </button>
          </div>

          {loading && <div className="notice-board-empty">공지사항을 불러오는 중입니다...</div>}
          {error && <p className="error-message">{error}</p>}

          {!loading && !error && pagedNotices.length === 0 && (
            <div className="notice-board-empty">등록된 공지사항이 없습니다.</div>
          )}

          <div className="notice-card-list">
            {pagedNotices.map((notice) => (
              <button
                key={notice.id}
                className={selectedNotice?.id === notice.id ? "notice-board-card active" : "notice-board-card"}
                onClick={() => handleNoticeClick(notice.id)}
              >
                <div className="notice-board-card-left">
                  <span className="notice-cat">{notice.isPinned ? "중요" : "공지"}</span>

                  <div>
                    <h3>{notice.title}</h3>
                    <p>
                      {notice.content.length > 80
                        ? `${notice.content.slice(0, 80)}...`
                        : notice.content}
                    </p>
                  </div>
                </div>

                <div className="notice-board-card-right">
                  <span>{notice.createdAt?.slice(0, 10) || "-"}</span>
                  <span>조회 {notice.viewCount ?? 0}</span>
                </div>
              </button>
            ))}
          </div>

          {totalPages > 1 && (
            <div className="notice-pagination">
              <button className="page-button" onClick={() => handlePageChange(currentPage - 1)} disabled={currentPage <= 1}>
                이전
              </button>

              {Array.from({ length: totalPages }).map((_, index) => {
                const page = index + 1;
                return (
                  <button
                    key={page}
                    className={page === currentPage ? "page-button active" : "page-button"}
                    onClick={() => handlePageChange(page)}
                  >
                    {page}
                  </button>
                );
              })}

              <button className="page-button" onClick={() => handlePageChange(currentPage + 1)} disabled={currentPage >= totalPages}>
                다음
              </button>
            </div>
          )}

          <p className="page-info">
            총 {sortedNotices.length}개 공지 · {currentPage} / {totalPages} 페이지
          </p>
        </section>
      </main>
    </>
  );
}
