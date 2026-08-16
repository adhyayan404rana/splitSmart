import { motion, AnimatePresence } from "framer-motion";
import { Check, SlidersHorizontal, TriangleAlert, GitCommitVertical, ChevronDown } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";
import { formatMinor, categoryTone, type CurrencyCode, type Draft } from "@/lib/splitsmart-data";
import { Avatar, AvatarStack, GhostButton, GlassCard, Pill, SectionLabel } from "./primitives";
import { cn } from "@/lib/utils";

export function ConsensusScreen({
  currency,
  drafts,
  onApprove,
  onModify,
  onDispute,
}: {
  currency: CurrencyCode;
  drafts: Draft[];
  onApprove: (id: string) => void;
  onModify: (id: string) => void;
  onDispute: (id: string) => void;
}) {
  const [open, setOpen] = useState<string | null>(null);
  const [editing, setEditing] = useState<Draft | null>(null);

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="font-display text-lg font-bold tracking-tight">Pending drafts</h2>
          <p className="text-xs text-muted-foreground">
            Optimistic concurrency control — every mutation bumps the draft version.
          </p>
        </div>
        <Pill tone="brand">{drafts.filter((d) => d.approvals < d.required).length} awaiting quorum</Pill>
      </div>

      <div className="grid gap-4 xl:grid-cols-2">
        {drafts.map((d) => {
          const pct = Math.round((d.approvals / d.required) * 100);
          const expanded = open === d.id;
          return (
            <GlassCard key={d.id} className="p-5">
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-start gap-3">
                  <Avatar initials={d.payerInitials} size="md" />
                  <div>
                    <p className="font-semibold leading-tight">{d.title}</p>
                    <p className="text-[11px] text-muted-foreground">
                      paid by {d.payer} · {d.split} split
                    </p>
                  </div>
                </div>
                <p className="font-display text-xl font-extrabold text-foreground tabular-nums">
                  {formatMinor(d.total, currency)}
                </p>
              </div>

              <div className="mt-4 flex flex-wrap items-center gap-2">
                <span
                  className={cn(
                    "rounded-full border px-2.5 py-1 text-[11px] font-semibold",
                    categoryTone[d.category],
                  )}
                >
                  {d.category}
                </span>
                <Pill tone={d.approvals >= d.required ? "positive" : "muted"}>
                  {d.approvals}/{d.required} approved
                </Pill>
                <Pill tone="brand">OCC {d.version}</Pill>
                <Pill>{d.confidence}% confidence</Pill>
                <AvatarStack items={d.participants} />
              </div>

              <div className="mt-4 h-1.5 w-full overflow-hidden rounded-full bg-secondary">
                <motion.div
                  className="gradient-brand h-full rounded-full"
                  initial={false}
                  animate={{ width: `${Math.min(pct, 100)}%` }}
                  transition={{ type: "spring", stiffness: 220, damping: 28 }}
                />
              </div>

              <div className="mt-4 flex flex-wrap gap-2">
                <GhostButton
                  tone="positive"
                  onClick={() => onApprove(d.id)}
                  aria-label={`Approve ${d.title}`}
                >
                  <Check size={15} strokeWidth={1.75} /> Approve
                </GhostButton>
                <GhostButton onClick={() => setEditing(d)}>
                  <SlidersHorizontal size={15} strokeWidth={1.75} /> Modify split
                </GhostButton>
                <GhostButton tone="negative" onClick={() => onDispute(d.id)}>
                  <TriangleAlert size={15} strokeWidth={1.75} /> Dispute
                </GhostButton>
                <button
                  type="button"
                  onClick={() => setOpen(expanded ? null : d.id)}
                  aria-expanded={expanded}
                  className="ml-auto inline-flex items-center gap-1 text-[11px] font-semibold text-muted-foreground transition-colors hover:text-foreground"
                >
                  Version history
                  <ChevronDown
                    size={14}
                    strokeWidth={1.75}
                    className={cn("transition-transform", expanded && "rotate-180")}
                  />
                </button>
              </div>

              <AnimatePresence initial={false}>
                {expanded && (
                  <motion.ul
                    initial={{ height: 0, opacity: 0 }}
                    animate={{ height: "auto", opacity: 1 }}
                    exit={{ height: 0, opacity: 0 }}
                    transition={{ duration: 0.24 }}
                    className="mt-3 space-y-2 overflow-hidden border-t border-border pt-3"
                  >
                    {d.history.map((h) => (
                      <li key={h.version} className="flex items-start gap-2.5 text-xs">
                        <GitCommitVertical
                          size={15}
                          strokeWidth={1.75}
                          className="mt-0.5 shrink-0 text-primary"
                        />
                        <span>
                          <span className="font-semibold">{h.version}</span>{" "}
                          <span className="text-muted-foreground">· {h.at}</span>
                          <span className="block text-muted-foreground">{h.change}</span>
                        </span>
                      </li>
                    ))}
                  </motion.ul>
                )}
              </AnimatePresence>
            </GlassCard>
          );
        })}
      </div>

      <AnimatePresence>
        {editing && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 z-50 bg-background/70 backdrop-blur-sm"
              onClick={() => setEditing(null)}
            />
            <motion.aside
              role="dialog"
              aria-label="Modify split"
              initial={{ x: "100%" }}
              animate={{ x: 0 }}
              exit={{ x: "100%" }}
              transition={{ type: "spring", stiffness: 320, damping: 34 }}
              className="glass-strong fixed inset-y-0 right-0 z-50 w-full max-w-md overflow-y-auto p-6"
            >
              <SectionLabel>Conflict resolution</SectionLabel>
              <h3 className="font-display mt-1 text-xl font-bold">{editing.title}</h3>
              <p className="mt-1 text-xs text-muted-foreground">
                Editing from {editing.version} — submitting bumps the version and appends a
                ConflictResolved event.
              </p>

              <ul className="mt-5 space-y-2">
                {editing.participants.map((p, i) => (
                  <li key={p} className="glass flex items-center justify-between rounded-xl p-3">
                    <span className="flex items-center gap-2 text-sm font-semibold">
                      <Avatar initials={p} size="sm" /> Share {i + 1}
                    </span>
                    <span className="font-display text-sm font-bold tabular-nums">
                      {formatMinor(Math.round(editing.total / editing.participants.length), currency)}
                    </span>
                  </li>
                ))}
              </ul>

              <div className="mt-6 flex gap-2">
                <GhostButton
                  tone="positive"
                  className="flex-1"
                  onClick={() => {
                    onModify(editing.id);
                    setEditing(null);
                  }}
                >
                  Commit new version
                </GhostButton>
                <GhostButton
                  className="flex-1"
                  onClick={() => {
                    setEditing(null);
                    toast("Edit discarded — no event appended");
                  }}
                >
                  Cancel
                </GhostButton>
              </div>
            </motion.aside>
          </>
        )}
      </AnimatePresence>
    </div>
  );
}
