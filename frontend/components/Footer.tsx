export default function Footer() {
  return (
    <footer>
      <div className="container">
        <div className="foot">
          <div>
            <div className="brand">META GG</div>
            <div className="disclaimer">
              META GG isn&apos;t endorsed by Riot Games and doesn&apos;t reflect the views or
              opinions of Riot Games or anyone officially involved in producing or managing
              League of Legends.
            </div>
          </div>
          <div className="links">
            <button onClick={() => alert("About 페이지 이동 예정")}>About</button>
            <button onClick={() => alert("문의 페이지 이동 예정")}>문의</button>
            <a href="https://github.com" target="_blank" rel="noopener noreferrer">
              GitHub
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}
