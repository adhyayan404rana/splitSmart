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
import { MessagingPanel } from "@/components/splitsmart/MessagingPanel";
import {
  initialDrafts,
  initialEvents,
  initialTransfers,
  getStoredGroups,
  joinGroupInStore,
  formatMinor,
  type CurrencyCode,
  type Draft,
  type LedgerEvent,
  type PersonaId,
  type Transfer,
  type Group,
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
  const [user, setUser] = useState<{ name: string; email: string } | null>(() => {
    if (typeof window !== "undefined") {
      const saved = localStorage.getItem("splitsmart_user");
      if (saved) {
        try {
          return JSON.parse(saved);
        } catch {
          return null;
        }
      }
    }
    return null;
  });

  const [tab, setTab] = useState<TabId>(() => {
    if (typeof window !== "undefined") {
      const saved = localStorage.getItem("splitsmart_user");
      if (saved) return "overview";
    }
    return "landing";
  });

  const [persona, setPersona] = useState<PersonaId>("sarah");
  const [currency] = useState<CurrencyCode>("INR");
  const [refreshKey, setRefreshKey] = useState(0);
  const [inviteGroup, setInviteGroup] = useState<{
    name: string;
    emoji?: string;
    code: string;
  } | null>(null);

  const [isDark, setIsDark] = useState<boolean>(() => {
    if (typeof window !== "undefined") {
      const saved = localStorage.getItem("splitsmart_theme");
      if (saved) return saved === "dark";
      return window.matchMedia("(prefers-color-scheme: dark)").matches;
    }
    return true;
  });

  const [onboarding, setOnboarding] = useState(false);
  const [authOpen, setAuthOpen] = useState(false);
  const [messagesOpen, setMessagesOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(1);
  const [drafts, setDrafts] = useState<Draft[]>(initialDrafts);
  const [transfers, setTransfers] = useState<Transfer[]>(initialTransfers);
  const [events, setEvents] = useState<LedgerEvent[]>(initialEvents);

  // Sync theme with html root class
  useEffect(() => {
    if (typeof document !== "undefined") {
      const root = document.documentElement;
      if (isDark) {
        root.classList.add("dark");
        root.classList.remove("light");
        localStorage.setItem("splitsmart_theme", "dark");
      } else {
        root.classList.remove("dark");
        root.classList.add("light");
        localStorage.setItem("splitsmart_theme", "light");
      }
    }
  }, [isDark]);

  // Handle Shareable Invite Link (?join=CODE or ?invite=CODE)
  useEffect(() => {
    if (typeof window !== "undefined") {
      const params = new URLSearchParams(window.location.search);
      const joinCode = params.get("join") || params.get("invite");
      if (joinCode) {
        const storedGroups = getStoredGroups();
        const found = storedGroups.find(
          (g) => g.inviteCode.toUpperCase() === joinCode.toUpperCase()
        );
        const targetGroup = found
          ? { name: found.name, emoji: found.emoji, code: found.inviteCode }
          : { name: `Group (${joinCode})`, emoji: "🏝️", code: joinCode };

        setInviteGroup(targetGroup);

        const savedUserStr = localStorage.getItem("splitsmart_user");
        if (!savedUserStr) {
          // If user is not logged in, prompt sign up modal first!
          setTab("landing");
          setAuthOpen(true);
          toast.info(`You've been invited to join ${targetGroup.name}! Please sign up to proceed.`);
        } else {
          // If already logged in, join the group in the store!
          try {
            const savedUser = JSON.parse(savedUserStr);
            joinGroupInStore(joinCode, savedUser);
            setRefreshKey((k) => k + 1);
            toast.success(`Joined ${targetGroup.name} via shareable link! 🎉`);
            setTab("overview");
          } catch {
            setAuthOpen(true);
          }
        }

        window.history.replaceState({}, "", window.location.pathname);
      }
    }
  }, []);

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
    <div className="min-h-screen pb-20">
      <Toaster theme={isDark ? "dark" : "light"} position="top-center" />
      <TopBar
        tab={tab}
        onTab={setTab}
        user={user}
        onNewGroup={() => setOnboarding(true)}
        onSignOut={() => {
          if (typeof window !== "undefined") {
            localStorage.removeItem("splitsmart_user");
          }
          setUser(null);
          setTab("landing");
          toast.success("Signed out successfully");
        }}
        onSignIn={() => setAuthOpen(true)}
        onToggleMessages={() => setMessagesOpen((p) => !p)}
        unreadCount={unreadCount}
        isMessagesOpen={messagesOpen}
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
                onLogin={() => setAuthOpen(true)}
              />
            )}
            {tab === "overview" && (
              <OverviewScreen
                key={`${user?.email || "guest"}_${refreshKey}`}
                currency={currency}
                user={user}
                onNewGroup={() => setOnboarding(true)}
                onNavigateToIngest={(_groupName) => setTab("ingest")}
                onNavigateToSettlement={() => setTab("settle")}
              />
            )}
            {tab === "ingest" && (
              <IngestScreen
                currency={currency}
                currentUser={user}
                onSubmitDraft={(d) => {
                  const draft: Draft = {
                    id: `d${Date.now()}`,
                    title: d.title,
                    total: d.total,
                    date: d.date || "19 Aug 2026",
                    payer: d.payer,
                    payerInitials: d.payerInitials,
                    category: d.category,
                    split: d.split,
                    approvals: 1,
                    required: d.participants.length,
                    userApproved: true,
                    isDisputed: false,
                    confidence: d.confidence,
                    participants: d.participants,
                    history: [
                      {
                        change: `Draft created · ${d.split.toLowerCase()} split from NLP extraction`,
                        at: `${d.date || "Today"} · ${new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`,
                      },
                    ],
                  };
                  setDrafts((prev) => [draft, ...prev]);
                  appendEvent({
                    type: "DraftCreated",
                    actor: d.payer,
                    summary: `Draft created for "${draft.title}" (${formatMinor(draft.total, currency)}) on ${draft.date}`,
                    payload: {
                      event: "DraftCreated",
                      draft_id: draft.id,
                      amount_minor: d.total,
                      date: draft.date,
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
                      d.id === id
                        ? {
                            ...d,
                            approvals: Math.min(d.approvals + 1, d.required),
                            userApproved: true,
                            isDisputed: false,
                            history: [
                              ...d.history,
                              {
                                change: "Approved by you",
                                at: `Now · ${new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`,
                              },
                            ],
                          }
                        : d
                    )
                  );
                  const d = drafts.find((x) => x.id === id)!;
                  appendEvent({
                    type: "DraftApproved",
                    actor: "You",
                    summary: `Approved ${d.title} (${Math.min(d.approvals + 1, d.required)}/${d.required})`,
                    payload: { event: "DraftApproved", draft_id: id, approvals: d.approvals + 1 },
                  });
                  toast.success("Approval recorded on the ledger ✓");
                }}
                onRevokeApproval={(id) => {
                  setDrafts((prev) =>
                    prev.map((d) =>
                      d.id === id
                        ? {
                            ...d,
                            approvals: Math.max(d.approvals - 1, 0),
                            userApproved: false,
                            history: [
                              ...d.history,
                              {
                                change: "Approval revoked by you",
                                at: `Now · ${new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`,
                              },
                            ],
                          }
                        : d
                    )
                  );
                  const d = drafts.find((x) => x.id === id);
                  if (d) {
                    appendEvent({
                      type: "ConflictResolved",
                      actor: "You",
                      summary: `Revoked approval on "${d.title}" — pending quorum`,
                      payload: { event: "ApprovalRevoked", draft_id: id },
                    });
                  }
                  toast.info("Approval revoked — moved back to pending");
                }}
                onModify={(id, updatedFields) => {
                  setDrafts((prev) =>
                    prev.map((d) =>
                      d.id === id
                        ? {
                            ...d,
                            ...updatedFields,
                            approvals: 1,
                            userApproved: true,
                            isDisputed: false,
                            history: [
                              ...d.history,
                              {
                                change: `Split modified to ${updatedFields.split || d.split} (${formatMinor(updatedFields.total || d.total, currency)})`,
                                at: `Now · ${new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`,
                              },
                            ],
                          }
                        : d
                    )
                  );
                  appendEvent({
                    type: "ConflictResolved",
                    actor: "You",
                    summary: `Split updated for draft ${id}`,
                    payload: { event: "ConflictResolved", draft_id: id, ...updatedFields },
                  });
                }}
                onDispute={(id, reason) => {
                  setDrafts((prev) =>
                    prev.map((d) =>
                      d.id === id
                        ? {
                            ...d,
                            isDisputed: true,
                            disputeReason: reason || "Disputed by group member",
                            history: [
                              ...d.history,
                              {
                                change: `Dispute raised: ${reason || "Flagged for review"}`,
                                at: `Now · ${new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`,
                              },
                            ],
                          }
                        : d
                    )
                  );
                  appendEvent({
                    type: "ConflictResolved",
                    actor: "You",
                    summary: `Dispute raised on draft ${id} (${reason || "Flagged"}) — consensus paused`,
                    payload: { event: "DisputeRaised", draft_id: id, reason, status: "BLOCKED" },
                  });
                }}
                onResolveDispute={(id) => {
                  setDrafts((prev) =>
                    prev.map((d) =>
                      d.id === id
                        ? {
                            ...d,
                            isDisputed: false,
                            disputeReason: undefined,
                            history: [
                              ...d.history,
                              {
                                change: "Dispute resolved and unfreezed",
                                at: `Now · ${new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`,
                              },
                            ],
                          }
                        : d
                    )
                  );
                  toast.success("Dispute resolved — draft unfreezed");
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
                    prev.map((x) => (x.id === id ? { ...x, settled: true } : x))
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

      <OnboardingModal
        open={onboarding}
        user={user}
        onClose={() => setOnboarding(false)}
        onGroupCreated={() => {
          setRefreshKey((k) => k + 1);
        }}
      />

      <AuthModal
        open={authOpen}
        inviteGroup={inviteGroup}
        defaultToSignUp={!!inviteGroup}
        onClose={() => {
          setAuthOpen(false);
          setInviteGroup(null);
        }}
        onSuccess={(u) => {
          if (typeof window !== "undefined") {
            localStorage.setItem("splitsmart_user", JSON.stringify(u));
          }
          setUser(u);

          // If joined via invite link, add to group store!
          if (inviteGroup) {
            joinGroupInStore(inviteGroup.code, u);
            setRefreshKey((k) => k + 1);
            toast.success(`Joined ${inviteGroup.name} successfully! 🎉`);
            setInviteGroup(null);
          } else {
            toast.success(`Signed in as ${u.name}`);
          }

          setTab("overview");
        }}
      />

      {/* Floating In-App Direct Messaging Drawer */}
      <MessagingPanel
        isOpen={messagesOpen}
        onClose={() => setMessagesOpen(false)}
        currentUser={user}
        onUnreadChange={setUnreadCount}
      />

      {/* Floating Bottom-Right Theme Slider Switch Board */}
      <div className="fixed bottom-6 right-6 z-50 select-none">
        <button
          type="button"
          role="switch"
          aria-checked={!isDark}
          aria-label="Toggle theme slider"
          title={isDark ? "Slide to Light Mode" : "Slide to Dark Mode"}
          onClick={() => {
            setIsDark((prev) => !prev);
            toast.success(!isDark ? "Switched to Dark Mode 🌙" : "Switched to Light Mode ☀️");
          }}
          className="glass relative flex h-10 w-20 items-center justify-between rounded-full p-1 border border-purple-500/40 shadow-[0_8px_30px_rgba(0,0,0,0.25)] backdrop-blur-2xl transition-all hover:scale-105 hover:border-purple-400 active:scale-95 cursor-pointer bg-card/90"
        >
          {/* Slider Board Track Icons */}
          <div className="flex w-full items-center justify-between px-2 text-muted-foreground pointer-events-none">
            <Sun size={14} className={!isDark ? "text-amber-500" : "opacity-35 text-slate-400"} />
            <Moon size={14} className={isDark ? "text-purple-400" : "opacity-35 text-slate-400"} />
          </div>

          {/* Smooth Sliding Thumb Knob with Spring Physics */}
          <motion.div
            layout
            transition={{
              type: "spring",
              stiffness: 550,
              damping: 32,
            }}
            style={{
              position: "absolute",
              top: "3px",
              left: isDark ? "calc(100% - 35px)" : "3px",
            }}
            className={`flex h-8 w-8 items-center justify-center rounded-full shadow-lg ${
              isDark
                ? "bg-gradient-to-r from-purple-600 to-indigo-600 text-white shadow-[0_0_16px_rgba(168,85,247,0.7)]"
                : "bg-gradient-to-r from-amber-400 to-orange-400 text-slate-900 shadow-[0_0_16px_rgba(251,191,36,0.7)]"
            }`}
          >
            {isDark ? (
              <Moon size={15} strokeWidth={2.2} />
            ) : (
              <Sun size={15} strokeWidth={2.2} />
            )}
          </motion.div>
        </button>
      </div>
    </div>
  );
}
