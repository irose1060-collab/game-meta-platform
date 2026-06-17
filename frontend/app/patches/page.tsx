"use client";

import { useCurrentUser } from "@/hooks/useCurrentUser";
import { useEffect, useState } from "react";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import { fetchPatchNotes } from "@/lib/patchNotesApi";
import type { PatchNote } from "@/types/patch-note";
import styles from "./PatchesPage.module.css";

function formatDate(value: string) {
  if (!value) return "-";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value.split("T")[0] || value;
  }

  return date.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
}

export default function PatchesPage() {
  const { user, logout, goHomeForAuth } = useCurrentUser();
  const [notes, setNotes] = useState<PatchNote[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadPatchNotes() {
      try {
        setLoading(true);
        setError("");

        const data = await fetchPatchNotes();
        setNotes(data);
      } catch (err) {
        setError(
          err instanceof Error
            ? err.message
            : "패치노트를 불러오지 못했습니다."
        );
      } finally {
        setLoading(false);
      }
    }

    loadPatchNotes();
  }, []);

  const latest = notes[0];

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
              <p className={styles.eyebrow}>RIOT OFFICIAL PATCH NOTES</p>
              <h1 className={styles.title}>패치 정보</h1>
              <p className={styles.description}>
                Riot 공식 리그 오브 레전드 패치노트를 불러와 요약하고,
                각 항목에서 공식 패치노트 페이지로 바로 이동할 수 있습니다.
              </p>
            </div>
          </div>
        </section>

        <section className={styles.content}>
          <div className={styles.container}>
            {loading && (
              <div className={styles.stateBox}>
                Riot 공식 패치노트를 불러오는 중입니다.
              </div>
            )}

            {!loading && error && <div className={styles.stateBox}>{error}</div>}

            {!loading && !error && latest && (
              <>
                <section className={styles.latestCard}>
                  <span className={styles.cardTag}>Latest Patch</span>
                  <h2 className={styles.latestTitle}>{latest.title}</h2>
                  <p className={styles.latestSummary}>{latest.summary}</p>
                  <div className={styles.meta}>
                    {latest.category} · {formatDate(latest.publishedAt)}
                  </div>
                  <a
                    href={latest.officialUrl}
                    target="_blank"
                    rel="noreferrer"
                    className={styles.officialButton}
                  >
                    Riot 공식 패치노트 보기 →
                  </a>
                </section>

                <div className={styles.list}>
                  {notes.map((note) => (
                    <article key={note.officialUrl} className={styles.patchCard}>
                      <div className={styles.versionBox}>
                        <div className={styles.version}>
                          {note.patchVersion || "PATCH"}
                        </div>
                        <div className={styles.versionLabel}>Version</div>
                      </div>

                      <div>
                        <h3 className={styles.patchTitle}>{note.title}</h3>
                        <p className={styles.patchSummary}>{note.summary}</p>
                        <div className={styles.patchMeta}>
                          {note.category} · {formatDate(note.publishedAt)}
                        </div>
                      </div>

                      <a
                        href={note.officialUrl}
                        target="_blank"
                        rel="noreferrer"
                        className={styles.linkButton}
                      >
                        공식 페이지 →
                      </a>
                    </article>
                  ))}
                </div>
              </>
            )}

            {!loading && !error && notes.length === 0 && (
              <div className={styles.stateBox}>
                표시할 패치노트가 없습니다.
              </div>
            )}
          </div>
        </section>
      </main>

      <Footer />
    </>
  );
}
