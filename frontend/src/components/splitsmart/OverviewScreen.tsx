import { motion, AnimatePresence } from "framer-motion";
import { ArrowUpRight, ArrowDownRight, TrendingUp, X, Users, Layers } from "lucide-react";
import { useState } from "react";
import {
  formatMinor,
  minorRemainder,
  members,
  groups,
  sparkline,
  type CurrencyCode,
  type Member,
} from "@/lib/splitsmart-data";
import { Amount, AvatarStack, Avatar, GlassCard, Pill, SectionLabel, Sparkline } from "./primitives";

export function OverviewScreen({ currency }: { currency: CurrencyCode }) {
  const [selected, setSelected] = useState<Member | null>(null);

  const getBack = members.filter((m) => m.balance > 0).reduce((a, m) => a + m.balance, 0);
  const owe = members.filter((m) => m.balance < 0).reduce((a, m) => a + m.balance, 0);
  const net = groups.reduce((a, g) => a + g.net, 0);

  return (
    <div className="space-y-6">
      <div className="grid gap-4 lg:grid-cols-3">
        <GlassCard className="relative overflow-hidden p-6 lg:col-span-2">
          <div
            aria-hidden
            className="gradient-brand pointer-events-none absolute -top-24 -right-16 h-64 w-64 rounded-full opacity-25 blur-3xl"
          />
          <SectionLabel>Net portfolio balance</SectionLabel>
          <div className="mt-3 flex flex-wrap items-end justify-between gap-4">
            <div>
              <p className="font-display text-4xl font-extrabold tracking-tight tabular-nums sm:text-5xl">
                {formatMinor(net, currency)}
              </p>
              <div className="mt-2 flex flex-wrap items-center gap-2">
                <Pill tone="positive">
                  <TrendingUp size={13} strokeWidth={1.75} /> +12.4% this cycle
                </Pill>
                <Pill>{minorRemainder(net, currency)} residual</Pill>
                <Pill tone="brand">
                  <Layers size={13} strokeWidth={1.75} /> 1,042 events
                </Pill>
              </div>
            </div>
            <div className="w-full max-w-[260px]">
              <Sparkline points={sparkline} />
              <p className="mt-1 text-right text-[11px] text-muted-foreground">
                last 12 ledger commits
              </p>
            </div>
          </div>
        </GlassCard>

        <div className="grid gap-4">
          <GlassCard className="glow-positive p-5">
            <SectionLabel>Total you get back</SectionLabel>
            <p className="font-display mt-2 text-3xl font-extrabold text-foreground tabular-nums">
              {formatMinor(getBack, currency)}
            </p>
            <p className="mt-1 flex items-center gap-1.5 text-xs text-muted-foreground">
              <ArrowDownRight size={14} strokeWidth={2} className="text-cyan-400" />
              across 2 groups · 3 members
            </p>
          </GlassCard>
          <GlassCard className="glow-negative p-5">
            <SectionLabel>Total you owe</SectionLabel>
            <p className="font-display mt-2 text-2xl font-bold text-foreground tabular-nums">
              {formatMinor(owe, currency)}
            </p>
            <p className="mt-1 flex items-center gap-1.5 text-xs text-muted-foreground">
              <ArrowUpRight size={14} strokeWidth={1.75} className="text-muted-foreground" />
              4 open obligations
            </p>
          </GlassCard>
        </div>
      </div>

      <section aria-labelledby="groups-heading" className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 id="groups-heading" className="font-display text-lg font-bold tracking-tight">
            Groups
          </h2>
          <SectionLabel>{groups.length} active ledgers</SectionLabel>
        </div>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {groups.map((g) => (
            <GlassCard key={g.id} className="p-5">
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-3">
                  <span className="flex h-10 w-10 items-center justify-center rounded-xl border border-border bg-secondary/60 text-lg">
                    {g.emoji}
                  </span>
                  <div>
                    <p className="font-semibold">{g.name}</p>
                    <p className="text-[11px] text-muted-foreground">Invite · {g.inviteCode}</p>
                  </div>
                </div>
                {g.pendingDrafts > 0 ? (
                  <Pill tone="brand">{g.pendingDrafts} pending</Pill>
                ) : (
                  <Pill tone="positive">settled</Pill>
                )}
              </div>
              <div className="mt-5 flex items-end justify-between">
                <div>
                  <SectionLabel>Your net</SectionLabel>
                  <Amount
                    value={g.net}
                    formatted={formatMinor(g.net, currency)}
                    className="mt-1 block text-xl"
                  />
                </div>
                <AvatarStack items={g.memberIds.map((id) => members.find((m) => m.id === id)!.initials)} />
              </div>
            </GlassCard>
          ))}
        </div>
      </section>

      <section aria-labelledby="members-heading" className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 id="members-heading" className="font-display text-lg font-bold tracking-tight">
            Member balances
          </h2>
          <SectionLabel>Tap a card for the itemized breakdown</SectionLabel>
        </div>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {members.map((m) => (
            <button
              key={m.id}
              type="button"
              onClick={() => setSelected(m)}
              aria-label={`Open breakdown for ${m.name}`}
              className="glass card-hover rounded-2xl p-5 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <div className="flex items-center gap-3">
                <Avatar initials={m.initials} size="lg" />
                <div className="min-w-0">
                  <p className="truncate font-semibold">{m.name}</p>
                  <p className="truncate text-[11px] text-muted-foreground">
                    {m.vpa} · {m.group}
                  </p>
                </div>
              </div>
              <div className="mt-4 flex items-center justify-between">
                <div>
                  <SectionLabel>
                    {m.balance > 0 ? "Gets back" : m.balance < 0 ? "Owes" : "Settled"}
                  </SectionLabel>
                  <Amount
                    value={m.balance}
                    formatted={formatMinor(m.balance, currency)}
                    className="mt-1 block text-lg"
                  />
                </div>
                <Pill
                  tone={m.balance > 0 ? "positive" : m.balance < 0 ? "negative" : "muted"}
                >
                  {minorRemainder(m.balance, currency)}
                </Pill>
              </div>
            </button>
          ))}
        </div>
      </section>

      <AnimatePresence>
        {selected && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setSelected(null)}
              className="fixed inset-0 z-50 bg-background/70 backdrop-blur-sm"
            />
            <motion.aside
              role="dialog"
              aria-label={`${selected.name} breakdown`}
              initial={{ x: "100%" }}
              animate={{ x: 0 }}
              exit={{ x: "100%" }}
              transition={{ type: "spring", stiffness: 320, damping: 34 }}
              className="glass-strong fixed inset-y-0 right-0 z-50 w-full max-w-md overflow-y-auto p-6 sm:rounded-l-3xl"
            >
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <Avatar initials={selected.initials} size="lg" />
                  <div>
                    <p className="font-display text-lg font-bold">{selected.name}</p>
                    <p className="text-xs text-muted-foreground">
                      {selected.vpa} · {selected.group}
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setSelected(null)}
                  aria-label="Close breakdown"
                  className="rounded-full border border-border p-2 text-muted-foreground transition-colors hover:text-foreground"
                >
                  <X size={16} strokeWidth={1.75} />
                </button>
              </div>

              <div className="glass mt-6 rounded-2xl p-4">
                <SectionLabel>Net position</SectionLabel>
                <Amount
                  value={selected.balance}
                  formatted={formatMinor(selected.balance, currency)}
                  className="mt-1 block text-3xl"
                />
                <p className="mt-1 text-xs text-muted-foreground">
                  integer minor units · {minorRemainder(selected.balance, currency)} remainder
                </p>
              </div>

              <SectionLabel className="mt-6">Itemized breakdown</SectionLabel>
              <ul className="mt-3 space-y-2">
                {selected.breakdown.map((b) => (
                  <li
                    key={b.label}
                    className="glass flex items-start justify-between gap-3 rounded-xl p-3.5"
                  >
                    <div>
                      <p className="text-sm font-semibold">{b.label}</p>
                      <p className="text-[11px] text-muted-foreground">{b.note}</p>
                    </div>
                    <Amount
                      value={b.amount}
                      formatted={formatMinor(b.amount, currency)}
                      className="text-sm"
                    />
                  </li>
                ))}
              </ul>

              <p className="mt-6 flex items-center gap-1.5 text-[11px] text-muted-foreground">
                <Users size={13} strokeWidth={1.75} /> Balances derive from the immutable event log —
                no mutable totals.
              </p>
            </motion.aside>
          </>
        )}
      </AnimatePresence>
    </div>
  );
}
