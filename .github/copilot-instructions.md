# ChimpChat UI Guidelines

Apply these rules to every UI change in this workspace:

- Use the ChimpChat brand color `#ffe01b` as the primary accent and plain white page/surface backgrounds. Define colors as shared CSS custom properties in `chimpchatweb/src/index.css`; do not add one-off color literals elsewhere. Use the shared neutral palette for text, borders, and states.
- Use the `Alga` font family through shared typography tokens, with suitable system fallbacks. Keep headings, body text, labels, and controls on a consistent type scale and weight system.
- Reuse the shared spacing and radius tokens. Never use box shadows or shadow tokens. Keep layouts, component states, and interaction patterns visually consistent across screens.
- Preserve accessible contrast, keyboard focus visibility, semantic markup, and responsive behavior.
- When adding a shared UI pattern, update the existing theme tokens or styles rather than introducing a competing design system.

## Backend Structure

- Organize backend features by domain and use the layers `controller`, `business`, `entity`, `dto`, and `repository`; keep HTTP handling in controllers and business rules in business services.
- Name HTTP handlers `<Feature>Controller`, business services `<Feature>Business`, persistence models `<Feature>Entity`, request/response records with `Dto` suffix, and Spring Data interfaces `<Feature>Repository`.
- Never return entities from API controllers. Map entities to DTOs and enforce current-user/workspace ownership in the business layer for every read or mutation.
