export type PersonaId = "sarah" | "david" | "host";

export type Persona = {
  id: PersonaId;
  name: string;
  role: string;
  initials: string;
  vpa: string;
  tint: string;
};

export const personas: Persona[] = [
  {
    id: "sarah",
    name: "Sarah Menon",
    role: "Organizer",
    initials: "SM",
    vpa: "sarah@upi",
    tint: "from-primary to-primary-deep",
  },
  {
    id: "david",
    name: "David Rao",
    role: "Roommate",
    initials: "DR",
    vpa: "david@upi",
    tint: "from-positive to-primary",
  },
  {
    id: "host",
    name: "Aisha Kapoor",
    role: "Event Host",
    initials: "AK",
    vpa: "aisha@upi",
    tint: "from-negative to-primary-deep",
  },
];

export type CurrencyCode = "INR";

export const currencies: Record<
  CurrencyCode,
  { symbol: string; label: string; rate: number; minorLabel: string }
> = {
  INR: { symbol: "₹", label: "Indian Rupee", rate: 1, minorLabel: "paise" },
};

/** Amounts are stored as integer minor units (paise) — never floats. */
export function formatMinor(paise: number, code: CurrencyCode = "INR"): string {
  const value = paise / 100;
  const sign = value < 0 ? "-" : "";
  return `${sign}₹${Math.abs(value).toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

export function minorRemainder(paise: number, code: CurrencyCode): string {
  const { rate, minorLabel } = currencies[code];
  const converted = Math.round(Math.abs(paise) * rate);
  return `${converted % 100} ${minorLabel}`;
}

export type Member = {
  id: string;
  name: string;
  initials: string;
  vpa: string;
  /** signed integer paise: positive = is owed, negative = owes */
  balance: number;
  group: string;
  breakdown: { label: string; amount: number; note: string }[];
};

export const members: Member[] = [
  {
    id: "m1",
    name: "Sarah Menon",
    initials: "SM",
    vpa: "sarah@upi",
    balance: 412_500,
    group: "Goa Trip '26",
    breakdown: [
      { label: "Beach shack dinner", amount: 400_000, note: "Paid in full, split 3 ways" },
      { label: "Scooter rentals", amount: 90_000, note: "Paid, split with David" },
      { label: "Airport cab", amount: -77_500, note: "Owed to Maya" },
    ],
  },
  {
    id: "m2",
    name: "David Rao",
    initials: "DR",
    vpa: "david@upi",
    balance: -133_333,
    group: "Goa Trip '26",
    breakdown: [
      { label: "Beach shack dinner", amount: -133_333, note: "Equal share of ₹4,000" },
      { label: "Scooter rentals", amount: -45_000, note: "Equal share" },
      { label: "Villa deposit", amount: 45_000, note: "Prepaid" },
    ],
  },
  {
    id: "m3",
    name: "Maya Iyer",
    initials: "MI",
    vpa: "maya@upi",
    balance: -179_167,
    group: "Goa Trip '26",
    breakdown: [
      { label: "Beach shack dinner", amount: -133_333, note: "Equal share of ₹4,000" },
      { label: "Airport cab", amount: 77_500, note: "Paid, split 3 ways" },
      { label: "Kayak tickets", amount: -123_334, note: "Exact split" },
    ],
  },
  {
    id: "m4",
    name: "Rahul Verma",
    initials: "RV",
    vpa: "rahul@upi",
    balance: -100_000,
    group: "Flat 402 Bills",
    breakdown: [
      { label: "Electricity Aug", amount: -60_000, note: "40% weighted split" },
      { label: "Internet", amount: -40_000, note: "Equal share" },
    ],
  },
  {
    id: "m5",
    name: "Aisha Kapoor",
    initials: "AK",
    vpa: "aisha@upi",
    balance: 0,
    group: "Flat 402 Bills",
    breakdown: [{ label: "Settled up", amount: 0, note: "All ledgers reconciled" }],
  },
];

export type Group = {
  id: string;
  name: string;
  emoji: string;
  memberIds: string[];
  net: number;
  pendingDrafts: number;
  inviteCode: string;
};

export const groups: Group[] = [
  {
    id: "g1",
    name: "Goa Trip '26",
    emoji: "🏝️",
    memberIds: ["m1", "m2", "m3"],
    net: 412_500,
    pendingDrafts: 2,
    inviteCode: "GOA7XK21",
  },
  {
    id: "g2",
    name: "Flat 402 Bills",
    emoji: "🏠",
    memberIds: ["m4", "m5", "m1"],
    net: -100_000,
    pendingDrafts: 1,
    inviteCode: "FLT402QZ",
  },
  {
    id: "g3",
    name: "Team Offsite",
    emoji: "🎯",
    memberIds: ["m1", "m3", "m5"],
    net: 68_400,
    pendingDrafts: 0,
    inviteCode: "OFFSTE94",
  },
];

export const sparkline = [
  120_000, 96_000, 180_000, 142_000, 240_000, 208_000, 305_000, 268_000, 352_000, 331_000, 402_000,
  412_500,
];

export type Category = "Food" | "Transport" | "Stay" | "Bills";

export type Draft = {
  id: string;
  title: string;
  total: number;
  payer: string;
  payerInitials: string;
  category: Category;
  split: "Equal" | "Exact" | "Percentage";
  approvals: number;
  required: number;
  version: string;
  confidence: number;
  history: { version: string; change: string; at: string }[];
  participants: string[];
};

export const initialDrafts: Draft[] = [
  {
    id: "d1",
    title: "Beach shack dinner",
    total: 400_000,
    payer: "Sarah Menon",
    payerInitials: "SM",
    category: "Food",
    split: "Equal",
    approvals: 2,
    required: 3,
    version: "v1.1",
    confidence: 98,
    history: [
      { version: "v1.0", change: "DraftCreated · equal split across 3 members", at: "18:42:07" },
      { version: "v1.1", change: "Split adjusted: Maya exact ₹1,200 → remainder equal", at: "18:47:55" },
    ],
    participants: ["SM", "DR", "MI"],
  },
  {
    id: "d2",
    title: "Scooter rentals (2 days)",
    total: 90_000,
    payer: "Sarah Menon",
    payerInitials: "SM",
    category: "Transport",
    split: "Equal",
    approvals: 1,
    required: 2,
    version: "v1.0",
    confidence: 94,
    history: [{ version: "v1.0", change: "DraftCreated · equal split across 2 members", at: "09:12:31" }],
  participants: ["SM", "DR"],
  },
  {
    id: "d3",
    title: "Electricity — August",
    total: 150_000,
    payer: "Rahul Verma",
    payerInitials: "RV",
    category: "Bills",
    split: "Percentage",
    approvals: 2,
    required: 3,
    version: "v1.2",
    confidence: 91,
    history: [
      { version: "v1.0", change: "DraftCreated · equal split", at: "11:02:10" },
      { version: "v1.1", change: "Changed to weighted 40/35/25", at: "11:09:44" },
      { version: "v1.2", change: "OCC conflict resolved — retry on stale version", at: "11:12:03" },
    ],
    participants: ["RV", "AK", "SM"],
  },
];

export type Transfer = {
  id: string;
  from: string;
  fromInitials: string;
  to: string;
  toInitials: string;
  toVpa: string;
  amount: number;
  settled: boolean;
};

export const initialTransfers: Transfer[] = [
  {
    id: "t1",
    from: "David Rao",
    fromInitials: "DR",
    to: "Sarah Menon",
    toInitials: "SM",
    toVpa: "sarah@upi",
    amount: 133_333,
    settled: false,
  },
  {
    id: "t2",
    from: "Maya Iyer",
    fromInitials: "MI",
    to: "Sarah Menon",
    toInitials: "SM",
    toVpa: "sarah@upi",
    amount: 179_167,
    settled: false,
  },
  {
    id: "t3",
    from: "Rahul Verma",
    fromInitials: "RV",
    to: "Sarah Menon",
    toInitials: "SM",
    toVpa: "sarah@upi",
    amount: 100_000,
    settled: true,
  },
];

export type EventType =
  | "DraftCreated"
  | "DraftApproved"
  | "LedgerCommitted"
  | "SettlementMarked"
  | "ConflictResolved";

export type LedgerEvent = {
  id: string;
  type: EventType;
  actor: string;
  summary: string;
  at: string;
  seq: number;
  payload: Record<string, unknown>;
};

export const initialEvents: LedgerEvent[] = [
  {
    id: "e5",
    type: "SettlementMarked",
    actor: "Rahul Verma",
    summary: "Settled ₹1,000.00 to sarah@upi via UPI",
    at: "Today · 18:58",
    seq: 1042,
    payload: {
      event: "SettlementMarked",
      transfer_id: "t3",
      amount_minor: 100000,
      currency: "INR",
      payee_vpa: "sarah@upi",
      rail: "UPI_P2P",
    },
  },
  {
    id: "e4",
    type: "LedgerCommitted",
    actor: "system",
    summary: "Ledger snapshot committed for Goa Trip '26",
    at: "Today · 18:51",
    seq: 1041,
    payload: {
      event: "LedgerCommitted",
      group_id: "g1",
      snapshot_hash: "b7f1c9e0a4d2",
      net_positions_minor: { m1: 412500, m2: -133333, m3: -179167 },
    },
  },
  {
    id: "e3",
    type: "ConflictResolved",
    actor: "Rahul Verma",
    summary: "OCC conflict on draft d3 resolved with retry at v1.2",
    at: "Today · 11:12",
    seq: 1040,
    payload: {
      event: "ConflictResolved",
      draft_id: "d3",
      expected_version: "v1.1",
      observed_version: "v1.2",
      strategy: "optimistic_retry",
    },
  },
  {
    id: "e2",
    type: "DraftApproved",
    actor: "David Rao",
    summary: "Approved beach shack dinner draft (2/3)",
    at: "Today · 18:47",
    seq: 1039,
    payload: { event: "DraftApproved", draft_id: "d1", approvals: 2, required: 3 },
  },
  {
    id: "e1",
    type: "DraftCreated",
    actor: "Sarah Menon",
    summary: "Draft created from NLP tier-2 extraction (98% confidence)",
    at: "Today · 18:42",
    seq: 1038,
    payload: {
      event: "DraftCreated",
      draft_id: "d1",
      amount_minor: 400000,
      category: "Food",
      split: "EQUAL",
      pipeline: ["FastPath", "ONNX_NER"],
      confidence: 0.98,
    },
  },
];

export const eventTone: Record<EventType, string> = {
  DraftCreated: "text-purple-400 font-bold",
  DraftApproved: "text-purple-400 font-bold",
  LedgerCommitted: "text-foreground font-semibold",
  SettlementMarked: "text-purple-400 font-bold",
  ConflictResolved: "text-foreground font-medium",
};

export const categoryTone: Record<Category, string> = {
  Food: "bg-purple-500/15 text-purple-300 border-purple-400/30 font-bold",
  Transport: "bg-purple-500/15 text-purple-300 border-purple-400/30 font-bold",
  Stay: "bg-purple-500/15 text-purple-300 border-purple-400/30 font-bold",
  Bills: "bg-white/10 text-foreground border-white/20 font-medium",
};

export function upiString(vpa: string, name: string, amountMinor: number) {
  const amount = (amountMinor / 100).toFixed(2);
  return `upi://pay?pa=${vpa}&pn=${encodeURIComponent(name)}&am=${amount}&cu=INR&tn=SplitSmart%20settlement`;
}

export const samplePrompts = [
  "Paid ₹4,000 for dinner at shacks, split with Rahul & Maya",
  "Uber to airport was 775, I paid, split equally with the trip crew",
  "Villa deposit 12,000 — Aisha 40%, me 35%, David 25%",
  "Groceries 2,340 paid by David, exclude Maya",
];
