export function BrandHeader({
  caption = "INSTAGRAM CONVERSATION AUTOMATION",
  children,
}) {
  return (
    <header className="site-header">
      <a className="brand" href="/" aria-label="ChimpChat home">
        <span className="brand-mark" aria-hidden="true">
          <img src="/logo-transparent.png" alt="" />
        </span>
        <span>chimpchat</span>
      </a>{" "}
    </header>
  );
}
