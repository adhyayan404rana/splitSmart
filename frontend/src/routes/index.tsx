import { createFileRoute } from "@tanstack/react-router";
import { AnimatePresence, motion } from "framer-motion";
import { useState, useEffect } from "react";
import { Toaster, toast } from "sonner";
import { Sun, Moon } from "lucide-react";
import { TopBar, type TabId } from "@/components/splitsmart/TopBar";
import { LandingPage } from "@/components/splitsmart/LandingPage";
import { OverviewScreen } from "@/components/splitsmart/OverviewScreen";
import { IngestScreen } from "@/components/splitsmart/IngestScreen";
import { ConsensusScreen } from "@/components/splitsmart/ConsensusScreen";
import { SettlementScreen } from "@/components/splitsmart/SettlementScreen";
import { AuditScreen } from "@/components/splitsmart/AuditScreen";
import { OnboardingModal } from "@/components/splitsmart/OnboardingModal";
import { AuthModal } from "@/components/splitsmart/AuthModal";
import {
  initialDrafts,
  initialEvents,
  initialTransfers,
  formatMinor,
  type CurrencyCode,
  type Draft,
  type LedgerEvent,
  type PersonaId,
  type Transfer,
} from "@/lib/splitsmart-data";

const title = "SplitSmart — AI group expense reconciliation";
const description =
  "SplitSmart reconciles group expenses with AI expense parsing, draft consensus, event-sourced audit trails and instant UPI settlement.";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title },
      { name: "description", content: description },
      { property: "og:title", content: title },
      { property: "og:description", content: description },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Index,
});

function Index() {
  const [tab, setTab] = useState<TabId>("landing");
  const [user, setUser] = useState<{ name: string; email: string } | null>(null);
  const [persona, setPersona] = useState<PersonaId>("sarah");
  const [currency, setCurrency] = useState<CurrencyCode>("INR");
  const [isDark, setIsDark] = useState(true);
  const [onboarding, setOnboarding] = useState(false);
  const [authOpen, setAuthOpen] = useState(false);
  const [drafts, setDrafts] = useState<Draft[]>(initialDrafts);
  const [transfers, setTransfers] = useState<Transfer[]>(initialTransfers);
  const [events, setEvents] = useState<LedgerEvent[]>(initialEvents);

  useEffect(() => {
    if (isDark) {
      document.documentElement.classList.add("dark");
      document.documentElement.classList.remove("light");
    } else {
      document.documentElement.classList.add("light");
      document.documentElement.classList.remove("dark");
    }
  }, [isDark]);

  useEffect(() => {
    if (user && tab === "landing") {
      setTab("overview");
    }
  }, [user, tab]);

  const handleTabChange = (targetTab: TabId) => {
    if (!user && targetTab !== "landing") {
      setAuthOpen(true);
      toast.info("Please sign in to access SplitSmart app tabs.");
      return;
    }
    if (user && targetTab === "landing") {
      setTab("overview");
      return;
    }
    setTab(targetTab);
  };

  const appendEvent = (e: Omit<LedgerEvent, "id" | "seq" | "at">) => {
    setEvents((prev) => [
      {
        ...e,
        id: `e${Date.now()}`,
        seq: (prev[0]?.seq ?? 1000) + 1,
        at: `Now · ${new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`,
      },
      ...prev,
    ]);
  };

  const bumpVersion = (v: string) => {
    const [maj, min] = v.replace("v", "").split(".");
    return `v${maj}.${Number(min ?? 0) + 1}`;
  };

  return (
    <div className="min-h-screen pb-20 no-scrollbar overflow-x-hidden">
      <Toaster theme={isDark ? "dark" : "light"} position="top-center" />
      <TopBar
        tab={tab}
        onTab={handleTabChange}
        persona={persona}
        onPersona={(p) => {
          setPersona(p);
          toast.success("Persona switched — permissions re-scoped");
        }}
        currency={currency}
        onCurrency={setCurrency}
        onNewGroup={() => setOnboarding(true)}
        isDark={isDark}
        onToggleTheme={() => setIsDark((prev) => !prev)}
        onOpenAuth={() => setAuthOpen(true)}
        user={user}
        onSignOut={() => {
          setUser(null);
          setTab("landing");
          toast.success("Signed out successfully");
        }}
      />

      <main className="mx-auto max-w-7xl px-3 pt-4 sm:px-6">
        <AnimatePresence mode="wait">
          <motion.div
            key={tab}
            initial={{ opacity: 0, y: 14 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.25, ease: [0.22, 1, 0.36, 1] }}
          >
            {tab === "landing" && (
              <LandingPage
                onGetStarted={() => {
                  if (user) {
                    setTab("overview");
                  } else {
                    setAuthOpen(true);
                  }
                }}
                onLogin={() => {
                  if (user) {
                    setTab("overview");
                  } else {
                    setAuthOpen(true);
                    toast.info("Please sign in to access the dashboard.");
                  }
                }}
              />
            )}
            {tab === "overview" && <OverviewScreen currency={currency} />}
            {tab === "ingest" && (
              <IngestScreen
                currency={currency}
                onSubmitDraft={(d) => {
                  const draft: Draft = {
                    id: `d${Date.now()}`,
                    title: d.title,
                    total: d.total,
                    payer: d.payer,
                    payerInitials: d.payerInitials,
                    category: d.category,
                    split: d.split,
                    approvals: 1,
                    required: d.participants.length,
                    version: "v1.0",
                    confidence: d.confidence,
                    participants: d.participants,
                    history: [
                      {
                        version: "v1.0",
                        change: `DraftCreated · ${d.split.toLowerCase()} split from NLP extraction`,
                        at: new Date().toLocaleTimeString(),
                      },
                    ],
                  };
                  setDrafts((prev) => [draft, ...prev]);
                  appendEvent({
                    type: "DraftCreated",
                    actor: d.payer,
                    summary: `Draft created from NLP extraction (${d.confidence}% confidence)`,
                    payload: {
                      event: "DraftCreated",
                      draft_id: draft.id,
                      amount_minor: d.total,
                      category: d.category,
                      split: d.split.toUpperCase(),
                      confidence: d.confidence / 100,
                    },
                  });
                  setTab("consensus");
                  toast.success("Draft submitted to group consensus");
                }}
              />
            )}
            {tab === "consensus" && (
              <ConsensusScreen
                currency={currency}
                drafts={drafts}
                onApprove={(id) => {
                  setDrafts((prev) =>
                    prev.map((d) =>
                      d.id === id ? { ...d, approvals: Math.min(d.approvals + 1, d.required) } : d,
                    ),
                  );
                  const d = drafts.find((x) => x.id === id)!;
                  appendEvent({
                    type: "DraftApproved",
                    actor: "You",
                    summary: `Approved ${d.title} (${Math.min(d.approvals + 1, d.required)}/${d.required})`,
                    payload: { event: "DraftApproved", draft_id: id, approvals: d.approvals + 1 },
                  });
                  toast.success("Approval recorded on the ledger");
                }}
                onModify={(id) => {
                  setDrafts((prev) =>
                    prev.map((d) =>
                      d.id === id
                        ? {
                            ...d,
                            version: bumpVersion(d.version),
                            approvals: 1,
                            history: [
                              ...d.history,
                              {
                                version: bumpVersion(d.version),
                                change: "Split modified — approvals reset under OCC",
                                at: new Date().toLocaleTimeString(),
                              },
                            ],
                          }
                        : d,
                    ),
                  );
                  appendEvent({
                    type: "ConflictResolved",
                    actor: "You",
                    summary: `Split modified for draft ${id} — new version committed`,
                    payload: { event: "ConflictResolved", draft_id: id, strategy: "optimistic_retry" },
                  });
                  toast.success("New version committed");
                }}
                onDispute={(id) => {
                  appendEvent({
                    type: "ConflictResolved",
                    actor: "You",
                    summary: `Dispute raised on draft ${id} — consensus paused`,
                    payload: { event: "DisputeRaised", draft_id: id, status: "BLOCKED" },
                  });
                  toast.error("Dispute raised — draft frozen pending review");
                }}
              />
            )}
            {tab === "settle" && (
              <SettlementScreen
                currency={currency}
                transfers={transfers}
                onSettle={(id) => {
                  const t = transfers.find((x) => x.id === id)!;
                  setTransfers((prev) =>
                    prev.map((x) => (x.id === id ? { ...x, settled: true } : x)),
                  );
                  appendEvent({
                    type: "SettlementMarked",
                    actor: t.from,
                    summary: `Settled ${formatMinor(t.amount, currency)} to ${t.toVpa} via UPI`,
                    payload: {
                      event: "SettlementMarked",
                      transfer_id: t.id,
                      amount_minor: t.amount,
                      payee_vpa: t.toVpa,
                      rail: "UPI_P2P",
                    },
                  });
                  toast.success("Settlement committed to the ledger 🎉");
                }}
              />
            )}
            {tab === "audit" && <AuditScreen events={events} />}
          </motion.div>
        </AnimatePresence>
      </main>

      <OnboardingModal open={onboarding} onClose={() => setOnboarding(false)} />
      <AuthModal
        open={authOpen}
        onClose={() => setAuthOpen(false)}
        onSuccess={(u) => {
          setUser(u);
          setTab("overview");
          toast.success(`Signed in as ${u.name}`);
        }}
      />

      {/* Floating Bottom-Right Theme Toggle Pill Button (No text caption) */}
      <button
        type="button"
        onClick={() => setIsDark((prev) => !prev)}
        aria-label="Toggle Theme"
        title={isDark ? "Switch to Light Mode" : "Switch to Dark Mode"}
        className="fixed bottom-6 right-6 z-50 glass flex h-9 w-16 items-center rounded-full p-1 border-purple-500/40 shadow-[0_0_20px_rgba(0,0,0,0.5)] transition-all hover:border-purple-400 hover:scale-105"
      >
        <div
          className={`flex h-7 w-7 items-center justify-center rounded-full transition-all duration-300 ${
            isDark ? "translate-x-7 bg-purple-600 text-white shadow-[0_0_15px_rgba(168,85,247,0.6)]" : "translate-x-0 bg-secondary text-purple-400"
          }`}
        >
          {isDark ? <Moon size={14} /> : <Sun size={14} />}
        </div>
      </button>
    </div>
  );
}
