"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import type { Notice } from "@/types";

type NoticeModalProps = {
  notice: Notice | null;
  onClose: () => void;
};

export default function NoticeModal({ notice, onClose }: NoticeModalProps) {
  const router = useRouter();

  useEffect(() => {
    if (!notice) {
      document.body.style.overflow = "";
      return;
    }

    document.body.style.overflow = "hidden";

    const resetModalState = () => {
      document.body.style.overflow = "";
      onClose();
    };

    window.addEventListener("pageshow", resetModalState);
    window.addEventListener("popstate", resetModalState);

    return () => {
      document.body.style.overflow = "";
      window.removeEventListener("pageshow", resetModalState);
      window.removeEventListener("popstate", resetModalState);
    };
  }, [notice, onClose]);

  if (!notice) return null;

  const goNoticeList = () => {
    document.body.style.overflow = "";
    onClose();
    router.push("/notices");
  };

  const close = () => {
    document.body.style.overflow = "";
    onClose();
  };

  return (
    <div className="modal-backdrop" onClick={close}>
      <div className="modal notice-modal" onClick={(event) => event.stopPropagation()}>
        <button className="close" onClick={close}>✕</button>
        <h3>{notice.title}</h3>
        <div className="meta">{notice.cat || notice.category || "공지"} · {notice.date}</div>
        <div className="content">{notice.content}</div>
        <div className="actions">
          <button className="btn-line" onClick={goNoticeList}>목록으로</button>
          <button className="btn btn-gold" onClick={close}>닫기</button>
        </div>
      </div>
    </div>
  );
}
