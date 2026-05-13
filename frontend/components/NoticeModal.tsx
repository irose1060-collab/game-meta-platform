"use client";

import type { Notice } from "@/types";

type NoticeModalProps = {
  notice: Notice | null;
  onClose: () => void;
};

export default function NoticeModal({ notice, onClose }: NoticeModalProps) {
  if (!notice) return null;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal notice-modal" onClick={(event) => event.stopPropagation()}>
        <button className="close" onClick={onClose}>
          ✕
        </button>
        <h3>{notice.title}</h3>
        <div className="meta">
          {notice.cat || notice.category || "공지"} · {notice.date}
        </div>
        <div className="content">{notice.content}</div>
        <div className="actions">
          <button className="btn-line" onClick={() => alert("공지 목록 페이지로 이동합니다.")}>
            목록으로
          </button>
          <button className="btn btn-gold" onClick={onClose}>
            닫기
          </button>
        </div>
      </div>
    </div>
  );
}
