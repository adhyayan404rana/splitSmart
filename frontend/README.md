# SplitWise AI

Build a world-class, ultra-sleek fintech web application named "SplitSmart" — an AI-powered, event-sourced group expense reconciliation platform. The interface must look and feel as modern and polished as Revolut, Linear, and Stripe, using a calming dark-mode aesthetic with glassmorphism, fluid micro-interactions, and high contrast typography.

---

### 1. VISUAL DESIGN SYSTEM & COLOR PALETTE
- **Background**: Soft dark obsidian gradients (`#090D16` to `#0E1422`).
- **Glassmorphism Panels**: `rgba(18, 24, 38, 0.7)` backdrop blur (`16px`), subtle 1px border (`rgba(255, 255, 255, 0.08)`), rounded 16px to 24px corners, soft ambient drop shadows.
- **Brand Accents**: Vibrant Electric Violet (`#6366F1`) & Indigo (`#4F46E5`) gradients for primary CTAs and active states.
- **Financial Balance Indicators**:
  - Positive / You get back: Emerald Green (`#10B981`) with soft green glow.
  - Negative / You owe: Coral Crimson (`#F43F5E`) with soft red glow.
- **Typography**: Clean geometric sans-serif (Inter / Plus Jakarta Sans). High hierarchy contrast with uppercase muted subheaders (`text-xs tracking-wider text-slate-400 font-semibold`).
- **Icons**: Lucide React icons with consistent stroke widths (1.75px).

---

### 2. NAVIGATION BAR & TOP APP BAR
- **Floating Glass Pill Header**: Centered sticky floating pill navbar with `backdrop-blur-xl`, `bg-slate-900/80`, rounded-full borders, and smooth spring sliding tab indicator.
- **Navigation Tabs**:
  1. 📊 **Overview & Groups**: Summary metrics, group balances, member cards.
  2. 💬 **AI Conversational Ingress**: Natural language chat input, receipt parser simulator, raw text parser.
  3. ⚖️ **Draft Consensus**: Pending expense approvals, split editor, OCC version history.
  4. 💳 **Settlement & UPI**: Pairwise debt minimization graph, dynamic UPI QR codes, instant pay buttons.
  5. 📜 **Audit Feed**: Chronological immutable event-sourcing ledger stream.
- **Top Actions**:
  - Persona Quick Switcher Pill (`Sarah (Organizer)`, `David (Roommate)`, `Event Host`).
  - Currency Dropdown Selector (INR ₹, USD $, EUR €, GBP £).
  - User Profile Avatar & "New Group / Onboarding" Floating CTA with gradient glow.

---

### 3. KEY INTERACTIVE SCREENS & COMPONENTS

#### Screen 1: Dashboard & Group Hub Overview
- **Hero Balance Banner**: Large gradient balance card displaying "Net Portfolio Balance" with a live mini sparkline graph, "Total You Get Back" card, and "Total You Owe" card.
- **Group Balance Grid**: Cards for each member showing avatar stacks, net amount (formatted with selected currency), integer paise/cents badge, and status indicators.
- **Member Breakdown Modal**: Detailed drawer sliding in to show itemized breakdown per group member.

#### Screen 2: Smart Conversational AI Expense Ingestion (NLP Tier 1-3)
- **Chat-Style Expense Input Container**:
  - Unstructured input textarea with prompt suggestions (e.g., *"Paid ₹4,000 for dinner at shacks, split with Rahul & Maya"*).
  - Quick action chips to populate sample chat logs.
  - Live processing state with glowing AI pulse animation showing pipeline execution (`Tier 1: FastPath` → `Tier 2: ONNX NER` → `Tier 3: Structured LLM`).
- **Extracted Expense Draft Card**:
  - Displays Payer, Amount, Category Badge (Food, Transport, Stay, Bills), Split Logic (Equal/Exact/Percentage), and confidence percentage badge (`98% High Confidence`).
  - "Submit to Group Consensus" CTA.

#### Screen 3: Draft Consensus & Conflict Resolution Drawer
- **Pending Draft Cards List**:
  - Cards displaying item name, total, creator avatar, and voting status (e.g., `2/3 Approved`).
  - Interactive "Approve", "Modify Split", and "Dispute" buttons.
  - OCC Version badge (`v1.0`, `v1.1`) with a toggle to expand historical version diffs.

#### Screen 4: Payment Execution & Dynamic UPI Terminal
- **Simplified Debt Graph Terminal**:
  - Visual flow diagram or cards showing optimized transfers (e.g., `David → Sarah: ₹1,333.33`).
  - "Minimum Transaction Optimization" badge highlighting saved transaction steps.
- **Interactive UPI QR Code Modal**:
  - High-resolution dynamic QR code generated for payee VPA (`sarah@upi`).
  - "Copy Payee VPA" & "Copy UPI String" buttons with instant checkmark feedback toasts.
  - "Pay via App" universal URI launcher link (`upi://pay?...`).
  - "Mark as Settled" button that triggers a celebration confetti animation and updates the ledger.

#### Screen 5: Audit Log & Event Sourcing Timeline
- **Immutable Stream Feed**:
  - Vertical timeline with glowing nodes for event types (`DraftCreated`, `DraftApproved`, `LedgerCommitted`, `SettlementMarked`).
  - Filter bar by event type and search bar.
  - Expandable code drawer to view raw JSON event payloads.

---

### 4. ONBOARDING & MODAL DIALOGS
- **Multi-Step Onboarding Modal**:
  - Step 1: User details & VPA input (`full_name`, `email`, `vpa`).
  - Step 2: Create or Join Group with 8-character invite code generation.
  - Step 3: Success state with 1-click shareable link copy.

---

### 5. MICRO-INTERACTIONS & TRANSITIONS
- Smooth Framer Motion page transitions (fade-in & slide-up on tab change).
- Subtle hover elevation and border glow effect on cards (`hover:border-indigo-500/50 hover:shadow-indigo-500/10`).
- Custom sleek scrollbars (`w-1.5 bg-slate-800 rounded-full`).
- Fully responsive for desktop, tablet, and mobile browsers with ARIA compliance.

This project was built with [Lovable](https://lovable.dev).

## Build with Lovable

Continue developing this project in the [Lovable editor](https://lovable.dev/projects/2bbbda25-0679-43cd-a8e8-75d40644b591).

- **Ship faster**: describe what you want to build and Lovable handles the code.
- **Stay in sync**: every change made in Lovable is committed straight to this repository.
- **Full ownership**: this code is yours. Push to `main` on GitHub and your changes sync back into Lovable, ready for your next prompt.

## Development

Prefer working locally? You need Node.js and npm — [install with nvm](https://github.com/nvm-sh/nvm#installing-and-updating).

```sh
git clone <this-repository-url>
cd <repository-name>
npm i
npm run dev
```
