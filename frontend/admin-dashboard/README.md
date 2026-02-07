# admin-dashboard

Frontend application for payment lifecycle, webhook delivery status, and ledger summaries.

## Responsibilities

- **Visualize payment lifecycles** — View payment intent states and transitions.
- **Show webhook delivery status** — List deliveries, retries, and dead-letter.
- **Display ledger summaries** — Account balances and entry summaries.

## Layout

- `src/pages/` — Page-level components (PaymentLifecycle, WebhookStatus, LedgerSummary).
- `src/components/` — Reusable UI (Layout, DataTable).
- `src/services/` — API client logic.
- `src/hooks/` — Custom React hooks (usePayments, useWebhooks).
- `src/styles/` — Styling.
- `public/` — Static assets and index.html.

## Run locally

```bash
npm install
npm run dev
```

API base URL is configured via env (e.g. VITE_API_URL).
