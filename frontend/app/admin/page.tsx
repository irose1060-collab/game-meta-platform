"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import type { AdminUser, DataCollectionLog, NoticeResponse, User } from "@/types";

type AdminTab = "notices" | "users" | "data";

export default function AdminPage() {
  const router = useRouter();

  const [user, setUser] = useState<User | null>(null);
  const [loaded, setLoaded] = useState(false);
  const [activeTab, setActiveTab] = useState<AdminTab>("notices");

  const [users, setUsers] = useState<AdminUser[]>([]);
  const [usersLoading, setUsersLoading] = useState(false);
  const [usersError, setUsersError] = useState("");

  const [notices, setNotices] = useState<NoticeResponse[]>([]);
  const [noticesLoading, setNoticesLoading] = useState(false);
  const [noticesError, setNoticesError] = useState("");

  const [noticeTitle, setNoticeTitle] = useState("");
  const [noticeContent, setNoticeContent] = useState("");
  const [isPinned, setIsPinned] = useState(false);
  const [editingNoticeId, setEditingNoticeId] = useState<number | null>(null);

  const [logs, setLogs] = useState<DataCollectionLog[]>([]);
  const [logsError, setLogsError] = useState("");

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

    setLoaded(true);

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

  useEffect(() => {
    if (user?.role !== "ADMIN") return;

    if (activeTab === "notices") fetchNotices();
    if (activeTab === "users") fetchUsers();
    if (activeTab === "data") fetchLogs();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab, user]);

  const goHome = () => router.push("/");

  const handleLogout = () => {
    localStorage.removeItem("metagg_user");
    localStorage.removeItem("metagg_token");
    router.push("/");
  };

  const fetchUsers = async () => {
    setUsersLoading(true);
    setUsersError("");

    try {
      const data = await apiFetch<AdminUser[]>("/api/admin/users");
      setUsers(data);
    } catch (err) {
      setUsersError(err instanceof Error ? err.message : "회원 목록 조회 실패");
    } finally {
      setUsersLoading(false);
    }
  };

  const fetchNotices = async () => {
    setNoticesLoading(true);
    setNoticesError("");

    try {
      const data = await apiFetch<NoticeResponse[]>("/api/admin/notices");
      setNotices(data);
    } catch (err) {
      setNoticesError(err instanceof Error ? err.message : "공지 목록 조회 실패");
    } finally {
      setNoticesLoading(false);
    }
  };

  const fetchLogs = async () => {
    setLogsError("");

    try {
      const data = await apiFetch<DataCollectionLog[]>("/api/admin/data/logs");
      setLogs(data);
    } catch (err) {
      setLogsError(err instanceof Error ? err.message : "수집 로그 조회 실패");
    }
  };

  const resetNoticeForm = () => {
    setNoticeTitle("");
    setNoticeContent("");
    setIsPinned(false);
    setEditingNoticeId(null);
  };

  const handleSubmitNotice = async () => {
    if (!noticeTitle.trim() || !noticeContent.trim()) {
      alert("공지 제목과 내용을 모두 입력해주세요.");
      return;
    }

    const body = JSON.stringify({
      title: noticeTitle,
      content: noticeContent,
      isPinned,
    });

    if (editingNoticeId) {
      await apiFetch<NoticeResponse>(`/api/admin/notices/${editingNoticeId}`, {
        method: "PUT",
        body,
      });
      alert("공지 수정이 완료되었습니다.");
    } else {
      await apiFetch<NoticeResponse>("/api/admin/notices", {
        method: "POST",
        body,
      });
      alert("공지 등록이 완료되었습니다.");
    }

    resetNoticeForm();
    fetchNotices();
  };

  const handleEditNotice = (notice: NoticeResponse) => {
    setEditingNoticeId(notice.id);
    setNoticeTitle(notice.title);
    setNoticeContent(notice.content);
    setIsPinned(notice.isPinned);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const handleDeleteNotice = async (id: number) => {
    const ok = confirm("정말 이 공지를 삭제하시겠습니까?");
    if (!ok) return;

    await apiFetch<{ message: string }>(`/api/admin/notices/${id}`, {
      method: "DELETE",
    });

    alert("공지사항이 삭제되었습니다.");
    fetchNotices();
  };

  const handleViewUser = (member: AdminUser) => {
    alert(
      `회원 상세 정보\n\n이메일: ${member.email}\n닉네임: ${member.nickname}\n권한: ${member.role}\n상태: ${member.status || "ACTIVE"}\n가입 방식: ${
        member.provider || "LOCAL"
      }\n가입일: ${member.createdAt?.slice(0, 10) || "-"}\n마지막 로그인: ${
        member.lastLoginAt?.slice(0, 19) || "-"
      }`
    );
  };

  const handleChangeRole = async (member: AdminUser) => {
    if (member.id === user?.id) {
      alert("현재 로그인한 관리자 자신의 권한은 변경할 수 없습니다.");
      return;
    }

    const nextRole = member.role === "ADMIN" ? "USER" : "ADMIN";
    const ok = confirm(`${member.nickname}님의 권한을 ${nextRole}로 변경하시겠습니까?`);

    if (!ok) return;

    try {
      await apiFetch<AdminUser>(`/api/admin/users/${member.id}/role`, {
        method: "PATCH",
        body: JSON.stringify({ role: nextRole }),
      });

      alert("권한이 변경되었습니다.");
      fetchUsers();
    } catch (err) {
      alert(err instanceof Error ? err.message : "권한 변경 중 오류가 발생했습니다.");
    }
  };

  const handleToggleStatus = async (member: AdminUser) => {
    if (member.id === user?.id) {
      alert("현재 로그인한 관리자 자신의 상태는 변경할 수 없습니다.");
      return;
    }

    const currentStatus = member.status || "ACTIVE";
    const nextStatus = currentStatus === "ACTIVE" ? "BLOCKED" : "ACTIVE";
    const ok = confirm(
      `${member.nickname}님의 상태를 ${nextStatus === "BLOCKED" ? "비활성화" : "활성화"}하시겠습니까?`
    );

    if (!ok) return;

    try {
      await apiFetch<AdminUser>(`/api/admin/users/${member.id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status: nextStatus }),
      });

      alert(nextStatus === "BLOCKED" ? "회원이 비활성화되었습니다." : "회원이 활성화되었습니다.");
      fetchUsers();
    } catch (err) {
      alert(err instanceof Error ? err.message : "회원 상태 변경 중 오류가 발생했습니다.");
    }
  };

  const handleDeleteUser = async (member: AdminUser) => {
    if (member.id === user?.id) {
      alert("현재 로그인한 관리자 계정은 삭제할 수 없습니다.");
      return;
    }

    const ok = confirm(`${member.nickname} 회원을 삭제하시겠습니까?`);
    if (!ok) return;

    try {
      await apiFetch<{ message: string }>(`/api/admin/users/${member.id}`, {
        method: "DELETE",
      });

      alert("회원이 삭제되었습니다.");
      fetchUsers();
    } catch (err) {
      alert(err instanceof Error ? err.message : "회원 삭제 중 오류가 발생했습니다.");
    }
  };

  const handleManualCollect = async () => {
    const ok = confirm("수동 재수집을 실행하시겠습니까?");
    if (!ok) return;

    try {
      const data = await apiFetch<{ message: string }>("/api/admin/data/collect", {
        method: "POST",
      });

      alert(data.message);
      fetchLogs();
    } catch (err) {
      alert(err instanceof Error ? err.message : "수동 재수집 중 오류가 발생했습니다.");
    }
  };

  if (!loaded) {
    return (
      <main className="admin-page">
        <div className="admin-panel">
          <h1>관리자 페이지 로딩 중...</h1>
        </div>
      </main>
    );
  }

  if (!user || user.role !== "ADMIN") {
    return (
      <main className="admin-page">
        <div className="admin-panel">
          <h1>관리자 권한이 필요합니다</h1>
          <p>관리자 계정으로 로그인 후 이용할 수 있습니다.</p>
          <button className="btn btn-gold" onClick={goHome}>
            홈으로 돌아가기
          </button>
        </div>
      </main>
    );
  }

  return (
    <main className="admin-page">
      <div className="admin-header">
        <button className="logo" onClick={goHome}>
          META GG
        </button>

        <div className="admin-header-actions">
          <button className="btn btn-ghost" onClick={goHome}>
            홈으로
          </button>
          <button className="btn btn-gold" onClick={handleLogout}>
            로그아웃
          </button>
        </div>
      </div>

      <section className="admin-hero">
        <span className="eyebrow">ADMIN DASHBOARD</span>
        <h1>관리자 페이지</h1>
        <p>공지사항, 회원, Riot API 데이터 수집 상태를 관리합니다.</p>
      </section>

      <section className="admin-layout">
        <aside className="admin-sidebar">
          <button className={activeTab === "notices" ? "active" : ""} onClick={() => setActiveTab("notices")}>
            공지사항 관리
          </button>
          <button className={activeTab === "users" ? "active" : ""} onClick={() => setActiveTab("users")}>
            회원 관리
          </button>
          <button className={activeTab === "data" ? "active" : ""} onClick={() => setActiveTab("data")}>
            데이터 수집 상태
          </button>
        </aside>

        <section className="admin-content">
          {activeTab === "notices" && (
            <div className="admin-section">
              <div className="card-tag">Notice</div>
              <h2>공지사항 관리</h2>
              <p>등록한 공지는 홈 화면의 최근 공지 영역에 바로 표시됩니다.</p>

              <div className="admin-form">
                <h3>{editingNoticeId ? "공지 수정" : "공지 등록"}</h3>

                <label>공지 제목</label>
                <input value={noticeTitle} onChange={(e) => setNoticeTitle(e.target.value)} placeholder="공지 제목을 입력하세요" />

                <label>공지 내용</label>
                <textarea value={noticeContent} onChange={(e) => setNoticeContent(e.target.value)} placeholder="공지 내용을 입력하세요" />

                <label className="admin-checkbox">
                  <input type="checkbox" checked={isPinned} onChange={(e) => setIsPinned(e.target.checked)} />
                  중요 공지로 고정
                </label>

                <div className="admin-form-actions">
                  <button className="btn btn-gold" onClick={handleSubmitNotice}>
                    {editingNoticeId ? "공지 수정" : "공지 등록"}
                  </button>

                  {editingNoticeId && (
                    <button className="btn btn-ghost" onClick={resetNoticeForm}>
                      수정 취소
                    </button>
                  )}
                </div>
              </div>

              <div className="admin-table-wrap">
                <h3>공지 목록</h3>

                {noticesLoading && <p className="success-message">공지 불러오는 중...</p>}
                {noticesError && <p className="error-message">{noticesError}</p>}

                <div className="admin-table">
                  <div className="admin-row admin-row-head admin-row-notice">
                    <span>제목</span>
                    <span>상태</span>
                    <span>작성일</span>
                    <span>관리</span>
                  </div>

                  {notices.map((notice) => (
                    <div className="admin-row admin-row-notice" key={notice.id}>
                      <span>{notice.title}</span>
                      <span>{notice.isPinned ? "중요 공지" : "일반"}</span>
                      <span>{notice.createdAt?.slice(0, 10)}</span>
                      <span className="admin-row-actions">
                        <button onClick={() => handleEditNotice(notice)}>수정</button>
                        <button onClick={() => handleDeleteNotice(notice.id)}>삭제</button>
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {activeTab === "users" && (
            <div className="admin-section">
              <div className="card-tag">Users</div>
              <h2>회원 관리</h2>
              <p>가입한 회원 목록을 DB에서 조회하고 권한 변경, 비활성화, 삭제를 수행합니다.</p>

              <div style={{ marginTop: "18px", marginBottom: "12px" }}>
                <button className="btn btn-gold" onClick={fetchUsers}>
                  회원 목록 새로고침
                </button>
              </div>

              {usersLoading && <p className="success-message">회원 목록 불러오는 중...</p>}
              {usersError && <p className="error-message">{usersError}</p>}

              <div className="admin-table">
                <div className="admin-row admin-row-head">
                  <span>이메일</span>
                  <span>닉네임</span>
                  <span>권한</span>
                  <span>상태</span>
                  <span>관리</span>
                </div>

                {users.length === 0 && !usersLoading ? (
                  <div className="admin-row">
                    <span>회원 데이터가 없습니다.</span>
                    <span>-</span>
                    <span>-</span>
                    <span>-</span>
                    <span>-</span>
                  </div>
                ) : (
                  users.map((member) => (
                    <div className="admin-row" key={member.id}>
                      <span>{member.email}</span>
                      <span>{member.nickname}</span>
                      <span>{member.role}</span>
                      <span>{member.status || "ACTIVE"}</span>
                      <span className="admin-row-actions admin-row-actions-vertical">
                        <div className="admin-action-top">
                          <button onClick={() => handleViewUser(member)}>상세</button>
                        </div>
                        <div className="admin-action-middle">
                          <button onClick={() => handleToggleStatus(member)}>
                            {(member.status || "ACTIVE") === "ACTIVE" ? "비활성화" : "활성화"}
                          </button>
                          <button onClick={() => handleDeleteUser(member)}>삭제</button>
                        </div>
                        <div className="admin-action-bottom">
                          <button className="role-change-button" onClick={() => handleChangeRole(member)}>
                            권한 변경
                          </button>
                        </div>
                      </span>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {activeTab === "data" && (
            <div className="admin-section">
              <div className="card-tag">Data</div>
              <h2>데이터 수집 상태</h2>
              <p>수동 재수집은 관리자가 Riot API 데이터 갱신을 직접 실행하는 기능입니다.</p>

              <div className="status-grid">
                <div className="status-box">
                  <strong>백엔드 상태</strong>
                  <span>Spring Boot 실행 중</span>
                </div>
                <div className="status-box">
                  <strong>DB 상태</strong>
                  <span>PostgreSQL 연결 성공</span>
                </div>
                <div className="status-box">
                  <strong>Riot API</strong>
                  <span>Account API 연동 성공</span>
                </div>
              </div>

              <button className="btn btn-gold" onClick={handleManualCollect}>
                수동 재수집 실행
              </button>

              {logsError && <p className="error-message">{logsError}</p>}

              <div className="admin-log-box">
                <h3>최근 수집 로그</h3>

                {logs.length === 0 ? (
                  <p>아직 수집 로그가 없습니다.</p>
                ) : (
                  logs.map((log) => (
                    <div key={log.id} style={{ marginBottom: "16px" }}>
                      <p>작업명: {log.jobName}</p>
                      <p>상태: {log.status}</p>
                      <p>총 건수: {log.totalCount}</p>
                      <p>성공: {log.successCount}</p>
                      <p>실패: {log.failCount}</p>
                      <p>생성일: {log.createdAt?.slice(0, 19)}</p>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </section>
      </section>
    </main>
  );
}
