"use client";

import { useEffect, useMemo, useState } from "react";
import { apiFetch } from "@/lib/api";
import type { AuthResponse, User } from "@/types";

type AuthModalProps = {
  isOpen: boolean;
  initialMode: "login" | "signup";
  onClose: () => void;
  onLoginSuccess: (user: User) => void;
};

type CheckResponse = {
  available: boolean;
  message: string;
};

export default function AuthModal({
  isOpen,
  initialMode,
  onClose,
  onLoginSuccess,
}: AuthModalProps) {
  const [mode, setMode] = useState<"login" | "signup">(initialMode);
  const [email, setEmail] = useState("");
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [emailChecked, setEmailChecked] = useState(false);
  const [emailAvailable, setEmailAvailable] = useState(false);
  const [nicknameChecked, setNicknameChecked] = useState(false);
  const [nicknameAvailable, setNicknameAvailable] = useState(false);
  const [message, setMessage] = useState("");
  const [isError, setIsError] = useState(false);
  const [loading, setLoading] = useState(false);

  const passwordRuleMessage = useMemo(() => {
    if (!password) return "";
    if (password.length < 8) return "비밀번호는 8자 이상이어야 합니다.";
    if (!/[A-Za-z]/.test(password) || !/\d/.test(password)) {
      return "비밀번호는 영문과 숫자를 포함해야 합니다.";
    }
    return "";
  }, [password]);

  const passwordConfirmMessage = useMemo(() => {
    if (mode !== "signup") return "";
    if (!passwordConfirm) return "";
    if (password !== passwordConfirm) return "비밀번호가 일치하지 않습니다.";
    return "";
  }, [mode, password, passwordConfirm]);

  useEffect(() => {
    if (isOpen) {
      setMode(initialMode);
      setMessage("");
      setIsError(false);
    }
  }, [isOpen, initialMode]);

  useEffect(() => {
    if (!isOpen) {
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
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const clearInputs = () => {
    setEmail("");
    setNickname("");
    setPassword("");
    setPasswordConfirm("");
    setEmailChecked(false);
    setEmailAvailable(false);
    setNicknameChecked(false);
    setNicknameAvailable(false);
  };

  const closeModal = () => {
    clearInputs();
    setMessage("");
    setIsError(false);
    document.body.style.overflow = "";
    onClose();
  };

  const switchMode = () => {
    clearInputs();
    setMessage("");
    setIsError(false);
    setMode((prev) => (prev === "login" ? "signup" : "login"));
  };

  const handleGoogleLogin = () => {
    document.body.style.overflow = "";
    window.location.href = "http://localhost:8080/oauth2/authorization/google";
  };

  const handleSocialComingSoon = (provider: string) => {
    alert(`${provider} 로그인은 Google 로그인 구현 후 같은 OAuth2 구조로 확장 예정입니다.`);
  };

  const handleEmailChange = (value: string) => {
    setEmail(value);
    setEmailChecked(false);
    setEmailAvailable(false);
  };

  const handleNicknameChange = (value: string) => {
    setNickname(value);
    setNicknameChecked(false);
    setNicknameAvailable(false);
  };

  const checkEmailDuplicate = async () => {
    const targetEmail = email.trim();

    if (!targetEmail) {
      setIsError(true);
      setMessage("이메일을 입력해주세요.");
      return;
    }

    if (!targetEmail.includes("@")) {
      setIsError(true);
      setMessage("올바른 이메일 형식을 입력해주세요.");
      return;
    }

    try {
      const data = await apiFetch<CheckResponse>(
        `/api/auth/check-email?email=${encodeURIComponent(targetEmail)}`
      );
      setEmailChecked(true);
      setEmailAvailable(data.available);
      setIsError(!data.available);
      setMessage(data.message);
    } catch (err) {
      setIsError(true);
      setMessage(err instanceof Error ? err.message : "이메일 중복확인에 실패했습니다.");
    }
  };

  const checkNicknameDuplicate = async () => {
    const targetNickname = nickname.trim();

    if (!targetNickname) {
      setIsError(true);
      setMessage("닉네임을 입력해주세요.");
      return;
    }

    if (targetNickname.length < 2) {
      setIsError(true);
      setMessage("닉네임은 2자 이상이어야 합니다.");
      return;
    }

    try {
      const data = await apiFetch<CheckResponse>(
        `/api/auth/check-nickname?nickname=${encodeURIComponent(targetNickname)}`
      );
      setNicknameChecked(true);
      setNicknameAvailable(data.available);
      setIsError(!data.available);
      setMessage(data.message);
    } catch (err) {
      setIsError(true);
      setMessage(err instanceof Error ? err.message : "닉네임 중복확인에 실패했습니다.");
    }
  };

  const validateSignup = () => {
    if (!email.trim() || !nickname.trim() || !password || !passwordConfirm) {
      setIsError(true);
      setMessage("모든 항목을 입력해주세요.");
      return false;
    }

    if (!emailChecked || !emailAvailable) {
      setIsError(true);
      setMessage("이메일 중복확인을 완료해주세요.");
      return false;
    }

    if (!nicknameChecked || !nicknameAvailable) {
      setIsError(true);
      setMessage("닉네임 중복확인을 완료해주세요.");
      return false;
    }

    if (passwordRuleMessage) {
      setIsError(true);
      setMessage(passwordRuleMessage);
      return false;
    }

    if (password !== passwordConfirm) {
      setIsError(true);
      setMessage("비밀번호가 일치하지 않습니다.");
      return false;
    }

    return true;
  };

  const handleSignup = async () => {
    if (!validateSignup()) return;

    await apiFetch<{ message?: string }>("/api/auth/signup", {
      method: "POST",
      body: JSON.stringify({ email, nickname, password }),
    });

    clearInputs();
    setIsError(false);
    setMessage("회원가입이 완료되었습니다. 로그인해주세요.");
    setMode("login");
  };

  const handleLogin = async () => {
    if (!email.trim() || !password) {
      setIsError(true);
      setMessage("이메일과 비밀번호를 입력해주세요.");
      return;
    }

    const authData = await apiFetch<AuthResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });

    if (!authData.token) {
      setIsError(true);
      setMessage("로그인 응답에 토큰이 없습니다.");
      return;
    }

    const user: User = {
      id: authData.id,
      email: authData.email,
      nickname: authData.nickname,
      role: authData.role,
      token: authData.token,
      provider: authData.provider,
      profileImageUrl: authData.profileImageUrl,
    };

    localStorage.setItem("metagg_token", authData.token);
    localStorage.setItem("metagg_user", JSON.stringify(user));

    setIsError(false);
    setMessage(`${user.nickname}님 로그인 성공`);

    onLoginSuccess(user);

    setTimeout(() => {
      clearInputs();
      document.body.style.overflow = "";
      onClose();
    }, 600);
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setMessage("");
    setIsError(false);
    setLoading(true);

    try {
      if (mode === "signup") {
        await handleSignup();
      } else {
        await handleLogin();
      }
    } catch (err) {
      setIsError(true);
      setMessage(err instanceof Error ? err.message : "서버 연결에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={closeModal}>
      <div className="modal" onClick={(event) => event.stopPropagation()}>
        <button className="close" onClick={closeModal}>✕</button>

        <h3>{mode === "login" ? "LOGIN" : "SIGN UP"}</h3>
        <div className="modal-sub">
          {mode === "login" ? "META GG 계정으로 로그인하세요" : "META GG에 합류하세요"}
        </div>

        <div className="social-login-box">
          <button type="button" className="social-button google" onClick={handleGoogleLogin}>
            Google로 계속하기
          </button>
          <button type="button" className="social-button kakao" onClick={() => handleSocialComingSoon("Kakao")}>
            Kakao
          </button>
          <button type="button" className="social-button naver" onClick={() => handleSocialComingSoon("Naver")}>
            Naver
          </button>
        </div>

        <div className="auth-divider">또는 이메일로 계속하기</div>

        <form onSubmit={handleSubmit} autoComplete="off">
          <div className="field">
            <label>이메일</label>
            <div className="input-with-button">
              <input
                type="email"
                placeholder="you@metagg.gg"
                value={email}
                onChange={(event) => handleEmailChange(event.target.value)}
                required
                autoComplete="off"
              />
              {mode === "signup" && (
                <button type="button" className="check-button" onClick={checkEmailDuplicate}>
                  중복확인
                </button>
              )}
            </div>

            {mode === "signup" && emailChecked && (
              <p className={emailAvailable ? "success-message" : "error-message"}>
                {emailAvailable ? "사용 가능한 이메일입니다." : "이미 사용 중인 이메일입니다."}
              </p>
            )}
          </div>

          {mode === "signup" && (
            <div className="field">
              <label>닉네임</label>
              <div className="input-with-button">
                <input
                  type="text"
                  placeholder="Summoner"
                  value={nickname}
                  onChange={(event) => handleNicknameChange(event.target.value)}
                  autoComplete="off"
                />
                <button type="button" className="check-button" onClick={checkNicknameDuplicate}>
                  중복확인
                </button>
              </div>

              {nicknameChecked && (
                <p className={nicknameAvailable ? "success-message" : "error-message"}>
                  {nicknameAvailable ? "사용 가능한 닉네임입니다." : "이미 사용 중인 닉네임입니다."}
                </p>
              )}
            </div>
          )}

          <div className="field">
            <label>비밀번호</label>
            <input
              type="password"
              placeholder="영문+숫자 포함 8자 이상"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
              autoComplete={mode === "signup" ? "new-password" : "off"}
            />
            {mode === "signup" && passwordRuleMessage && (
              <p className="error-message">{passwordRuleMessage}</p>
            )}
            {mode === "signup" && password && !passwordRuleMessage && (
              <p className="success-message">사용 가능한 비밀번호 형식입니다.</p>
            )}
          </div>

          {mode === "signup" && (
            <div className="field">
              <label>비밀번호 확인</label>
              <input
                type="password"
                placeholder="비밀번호를 다시 입력하세요"
                value={passwordConfirm}
                onChange={(event) => setPasswordConfirm(event.target.value)}
                required
                autoComplete="new-password"
              />
              {passwordConfirmMessage && <p className="error-message">{passwordConfirmMessage}</p>}
              {passwordConfirm && !passwordConfirmMessage && (
                <p className="success-message">비밀번호가 일치합니다.</p>
              )}
            </div>
          )}

          {message && <div className={isError ? "modal-msg error" : "modal-msg ok"}>{message}</div>}

          <div className="modal-actions">
            <button className="btn btn-gold" type="submit" disabled={loading}>
              {loading ? "처리 중..." : mode === "login" ? "로그인" : "가입하기"}
            </button>
          </div>
        </form>

        <div className="modal-foot">
          {mode === "login" ? "계정이 없으신가요?" : "이미 계정이 있으신가요?"}{" "}
          <button type="button" className="button-link" onClick={switchMode}>
            {mode === "login" ? "회원가입" : "로그인"}
          </button>
        </div>
      </div>
    </div>
  );
}
