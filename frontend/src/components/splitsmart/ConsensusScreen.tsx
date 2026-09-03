import { motion, AnimatePresence } from "framer-motion";
import {
  Check,
  SlidersHorizontal,
  TriangleAlert,
  Calendar,
  ChevronDown,
  X,
  Undo2,
  Clock,
  Sparkles,
  ShieldAlert,
  ShieldCheck,
  RotateCcw,
  Edit3,
} from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";
import {
  formatMinor,
  categoryTone,
  type CurrencyCode,
  type Draft,
} from "@/lib/splitsmart-data";
import {
  Avatar,
  AvatarStack,
  BrandButton,
  GhostButton,
  GlassCard,
  Pill,
  SectionLabel,
} from "./primitives";
import { cn } from "@/lib/utils";

export function ConsensusScreen({
  currency,
  drafts,
  onApprove,
  onRevokeApproval,
  onModify,
  onDispute,
  onResolveDispute,
}: {
  currency: CurrencyCode;
  drafts: Draft[];
  onApprove: (id: string) => void;
  onRevokeApproval?: (id: string) => void;
  onModify: (id: string, updatedDraft: Partial<Draft>) => void;
  onDispute: (id: string, reason?: string) => void;
  onResolveDispute?: (id: string) => void;
}) {
  const [openHistoryId, setOpenHistoryId] = useState<string | null>(null);
  const [editingDraft, setEditingDraft] = useState<Draft | null>(null);
  const [disputeModalDraft, setDisputeModalDraft] = useState<Draft | null>(null);
  const [disputeReasonText, setDisputeReasonText] = useState("");

  // State for the interactive Modify Split editor
  const [editTitle, setEditTitle] = useState("");
  const [editTotal, setEditTotal] = useState("");
  const [editDate, setEditDate] = useState("");
  const [editSplit, setEditSplit] = useState<Draft["split"]>("Equal");
  const [customShares, setCustomShares] = useState<Record<string, string>>({});

  const startEditing = (d: Draft) => {
    setEditingDraft(d);
    setEditTitle(d.title);
    setEditTotal((d.total / 100).toString());
    setEditDate(d.date || "Today");
    setEditSplit(d.split);

    const defaultShare = (d.total / (d.participants.length * 100)).toFixed(2);
    const initialShares: Record<string, string> = {};
    d.participants.forEach((p) => {
      initialShares[p] = defaultShare;
    });
    setCustomShares(initialShares);
  };

  const handleSaveModifiedSplit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingDraft) return;

    const totalNum = parseFloat(editTotal);
    if (isNaN(totalNum) || totalNum <= 0) {
      toast.error("Please enter a valid total amount");
      return;
    }

    const updatedTotalMinor = Math.round(totalNum * 100);

    onModify(editingDraft.id, {
      title: editTitle.trim() || editingDraft.title,
      total: updatedTotalMinor,
      date: editDate.trim() || editingDraft.date,
      split: editSplit,
    });

    setEditingDraft(null);
    toast.success("Split modified & updated on the ledger!");
  };

  const handleDisputeSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!disputeModalDraft) return;

    onDispute(disputeModalDraft.id, disputeReasonText.trim() || "Dispute raised by member");
    setDisputeModalDraft(null);
    setDisputeReasonText("");
    toast.error("Dispute recorded — transaction paused pending review");
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="font-display text-xl font-bold tracking-tight text-foreground">
            Draft Consensus & Approvals
          </h2>
          <p className="text-xs text-muted-foreground">
            Review and approve group expenses. You can change splits, raise disputes, or undo approvals at any time.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Pill tone="brand">
            {drafts.filter((d) => d.approvals < d.required && !d.isDisputed).length} awaiting quorum
          </Pill>
          {drafts.some((d) => d.isDisputed) && (
            <Pill tone="negative">
              {drafts.filter((d) => d.isDisputed).length} disputed
            </Pill>
          )}
        </div>
      </div>

      {/* Drafts Grid */}
      <div className="grid gap-4 xl:grid-cols-2">
        {drafts.map((d) => {
          const pct = Math.round((d.approvals / d.required) * 100);
          const expanded = openHistoryId === d.id;
          const isApproved = Boolean(d.userApproved);
          const isDisputed = Boolean(d.isDisputed);

          return (
            <GlassCard
              key={d.id}
              className={cn(
                "p-5 transition-all",
                isDisputed && "border-red-500/40 bg-red-500/5 shadow-[0_0_20px_rgba(239,68,68,0.15)]"
              )}
            >
              {/* Top Row: Title, Date, Payer & Amount */}
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-start gap-3">
                  <Avatar initials={d.payerInitials} size="md" />
                  <div>
                    <p className="font-bold text-foreground leading-tight text-base">{d.title}</p>
                    <p className="mt-1 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                      <span>Paid by <strong className="text-foreground">{d.payer}</strong></span>
                      <span>·</span>
                      <span>{d.split} split</span>
                      <span>·</span>
                      {/* Prominent Transaction Date */}
                      <span className="inline-flex items-center gap-1 font-semibold text-purple-400">
                        <Calendar size={12} /> {d.date || "18 Aug 2026"}
                      </span>
                    </p>
                  </div>
                </div>

                <div className="text-right shrink-0">
                  <p className="font-display text-xl font-black text-foreground tabular-nums">
                    {formatMinor(d.total, currency)}
                  </p>
                  <p className="text-[11px] text-muted-foreground font-mono">
                    {formatMinor(Math.round(d.total / d.participants.length), currency)} / person
                  </p>
                </div>
              </div>

              {/* Badges & Participants */}
              <div className="mt-4 flex flex-wrap items-center gap-2">
                <span
                  className={cn(
                    "rounded-full border px-2.5 py-0.5 text-xs font-semibold",
                    categoryTone[d.category]
                  )}
                >
                  {d.category}
                </span>

                {isDisputed ? (
                  <span className="rounded-full bg-red-500/20 px-2.5 py-0.5 text-xs font-bold text-red-400 border border-red-500/30 flex items-center gap-1">
                    <ShieldAlert size={12} /> Disputed
                  </span>
                ) : (
                  <Pill tone={d.approvals >= d.required ? "positive" : "muted"}>
                    {d.approvals}/{d.required} approved
                  </Pill>
                )}

                <Pill>{d.confidence}% confidence</Pill>

                <div className="ml-auto">
                  <AvatarStack items={d.participants} />
                </div>
              </div>

              {/* Approval Progress Bar */}
              <div className="mt-3.5 h-2 w-full overflow-hidden rounded-full bg-secondary/80">
                <motion.div
                  className={cn("h-full rounded-full", isDisputed ? "bg-red-500" : "gradient-brand")}
                  initial={false}
                  animate={{ width: `${Math.min(pct, 100)}%` }}
                  transition={{ type: "spring", stiffness: 220, damping: 28 }}
                />
              </div>

              {/* Disputed Alert Banner if active */}
              {isDisputed && (
                <div className="mt-3 rounded-xl border border-red-500/30 bg-red-500/10 p-2.5 text-xs text-red-300 flex items-center justify-between">
                  <span>⚠️ Reason: {d.disputeReason || "Disputed by group member"}</span>
                  {onResolveDispute && (
                    <button
                      type="button"
                      onClick={() => onResolveDispute(d.id)}
                      className="font-bold underline hover:text-white cursor-pointer ml-2"
                    >
                      Resolve & Unfreeze
                    </button>
                  )}
                </div>
              )}

              {/* Action Buttons: Approve / Undo Approve / Modify Split / Dispute */}
              <div className="mt-4 flex flex-wrap items-center gap-2 pt-2 border-t border-border/40">
                {/* Approve or Undo Approval Button */}
                {isApproved ? (
                  <button
                    type="button"
                    onClick={() => {
                      if (onRevokeApproval) {
                        onRevokeApproval(d.id);
                      } else {
                        onApprove(d.id);
                      }
                    }}
                    className="inline-flex items-center gap-1.5 rounded-full border border-emerald-500/40 bg-emerald-500/20 px-3.5 py-1.5 text-xs font-bold text-emerald-300 transition-all hover:bg-emerald-500/30 hover:scale-105 active:scale-95 cursor-pointer shadow-sm"
                    title="Click to undo your approval"
                  >
                    <Check size={13} strokeWidth={2.5} />
                    <span>Approved ✓ (Undo)</span>
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={() => onApprove(d.id)}
                    className="gradient-brand glow-brand inline-flex items-center gap-1.5 rounded-full px-3.5 py-1.5 text-xs font-bold text-white transition-all hover:scale-105 active:scale-95 cursor-pointer shadow-sm"
                  >
                    <Check size={13} strokeWidth={2.5} />
                    <span>Approve</span>
                  </button>
                )}

                {/* Modify Split Button — Always accessible! */}
                <button
                  type="button"
                  onClick={() => startEditing(d)}
                  className="inline-flex items-center gap-1.5 rounded-full border border-purple-500/30 bg-purple-500/10 px-3 py-1.5 text-xs font-bold text-purple-300 transition-all hover:bg-purple-500/20 hover:scale-105 active:scale-95 cursor-pointer"
                >
                  <SlidersHorizontal size={13} />
                  <span>Modify Split</span>
                </button>

                {/* Dispute / Resolve Dispute Button */}
                {isDisputed ? (
                  <button
                    type="button"
                    onClick={() => onResolveDispute?.(d.id)}
                    className="inline-flex items-center gap-1.5 rounded-full border border-border bg-secondary/60 px-3 py-1.5 text-xs font-semibold text-muted-foreground transition-all hover:text-foreground active:scale-95 cursor-pointer"
                  >
                    <RotateCcw size={12} />
                    <span>Cancel Dispute</span>
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={() => setDisputeModalDraft(d)}
                    className="inline-flex items-center gap-1.5 rounded-full border border-red-500/30 bg-red-500/10 px-3 py-1.5 text-xs font-bold text-red-400 transition-all hover:bg-red-500/20 hover:scale-105 active:scale-95 cursor-pointer"
                  >
                    <TriangleAlert size={12} />
                    <span>Dispute</span>
                  </button>
                )}

                {/* Activity Trail Accordion Toggle */}
                <button
                  type="button"
                  onClick={() => setOpenHistoryId(expanded ? null : d.id)}
                  aria-expanded={expanded}
                  className="ml-auto inline-flex items-center gap-1 text-[11px] font-semibold text-muted-foreground transition-colors hover:text-foreground cursor-pointer"
                >
                  <Clock size={12} />
                  <span>Activity</span>
                  <ChevronDown
                    size={13}
                    strokeWidth={2}
                    className={cn("transition-transform", expanded && "rotate-180")}
                  />
                </button>
              </div>

              {/* Activity Log Accordion */}
              <AnimatePresence initial={false}>
                {expanded && (
                  <motion.ul
                    initial={{ height: 0, opacity: 0 }}
                    animate={{ height: "auto", opacity: 1 }}
                    exit={{ height: 0, opacity: 0 }}
                    transition={{ duration: 0.22 }}
                    className="mt-3 space-y-2 overflow-hidden border-t border-border/50 pt-3"
                  >
                    {d.history.map((h, i) => (
                      <li key={i} className="flex items-start gap-2.5 text-xs">
                        <span className="h-1.5 w-1.5 rounded-full bg-purple-400 mt-1.5 shrink-0" />
                        <div>
                          <span className="font-semibold text-foreground">{h.change}</span>
                          <span className="text-[11px] text-muted-foreground ml-1.5">· {h.at}</span>
                        </div>
                      </li>
                    ))}
                  </motion.ul>
                )}
              </AnimatePresence>
            </GlassCard>
          );
        })}
      </div>

      {/* Interactive Modify Split Modal / Drawer */}
      <AnimatePresence>
        {editingDraft && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 bg-background/80 backdrop-blur-md"
              onClick={() => setEditingDraft(null)}
            />
            <motion.div
              role="dialog"
              aria-label="Modify Split"
              initial={{ opacity: 0, scale: 0.95, y: 15 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 10 }}
              className="glass-strong relative z-10 w-full max-w-lg overflow-hidden rounded-3xl border border-purple-500/30 p-6 shadow-[0_0_50px_rgba(0,0,0,0.5)]"
            >
              <div className="flex items-start justify-between">
                <div>
                  <SectionLabel>Modify Expense Split</SectionLabel>
                  <h3 className="font-display text-lg font-extrabold text-foreground mt-0.5">
                    {editingDraft.title}
                  </h3>
                </div>
                <button
                  type="button"
                  onClick={() => setEditingDraft(null)}
                  className="rounded-full border border-border p-2 text-muted-foreground hover:text-foreground cursor-pointer"
                >
                  <X size={15} />
                </button>
              </div>

              <form onSubmit={handleSaveModifiedSplit} className="mt-5 space-y-4">
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-[10px] font-bold uppercase text-muted-foreground">
                      Total Amount (₹)
                    </label>
                    <input
                      type="number"
                      required
                      value={editTotal}
                      onChange={(e) => setEditTotal(e.target.value)}
                      className="mt-1 w-full rounded-xl border border-input bg-background/80 px-3 py-2 text-sm font-bold outline-none focus:border-primary"
                    />
                  </div>

                  <div>
                    <label className="text-[10px] font-bold uppercase text-muted-foreground">
                      Transaction Date
                    </label>
                    <input
                      type="text"
                      required
                      value={editDate}
                      onChange={(e) => setEditDate(e.target.value)}
                      placeholder="e.g. 18 Aug 2026"
                      className="mt-1 w-full rounded-xl border border-input bg-background/80 px-3 py-2 text-sm outline-none focus:border-primary"
                    />
                  </div>
                </div>

                <div>
                  <label className="text-[10px] font-bold uppercase text-muted-foreground block mb-1.5">
                    Split Method
                  </label>
                  <div className="flex rounded-full border border-border bg-secondary/50 p-1">
                    {(["Equal", "Exact", "Percentage"] as const).map((method) => (
                      <button
                        key={method}
                        type="button"
                        onClick={() => setEditSplit(method)}
                        className={cn(
                          "flex-1 rounded-full py-1.5 text-xs font-bold transition-all cursor-pointer",
                          editSplit === method
                            ? "gradient-brand text-white shadow-sm font-extrabold"
                            : "text-muted-foreground hover:text-foreground"
                        )}
                      >
                        {method} Split
                      </button>
                    ))}
                  </div>
                </div>

                {/* Participant breakdown */}
                <div>
                  <label className="text-[10px] font-bold uppercase text-muted-foreground block mb-1">
                    Participant Breakdown ({editingDraft.participants.length} members)
                  </label>
                  <ul className="space-y-2 max-h-40 overflow-y-auto pr-1">
                    {editingDraft.participants.map((p, i) => {
                      const shareAmount = parseFloat(editTotal)
                        ? (parseFloat(editTotal) / editingDraft.participants.length).toFixed(2)
                        : "0.00";
                      return (
                        <li
                          key={p}
                          className="glass flex items-center justify-between rounded-xl p-2.5 px-3 border border-border/60 text-xs"
                        >
                          <div className="flex items-center gap-2 font-semibold">
                            <Avatar initials={p} size="sm" />
                            <span>Member {p}</span>
                          </div>
                          <span className="font-mono font-bold text-purple-400">
                            ₹{shareAmount} ({Math.round(100 / editingDraft.participants.length)}%)
                          </span>
                        </li>
                      );
                    })}
                  </ul>
                </div>

                <div className="flex justify-end gap-2 pt-2 border-t border-border/40">
                  <GhostButton type="button" onClick={() => setEditingDraft(null)} className="text-xs">
                    Cancel
                  </GhostButton>
                  <BrandButton type="submit" className="text-xs font-bold px-4 py-2">
                    Save Changes & Update Split
                  </BrandButton>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Dispute Modal */}
      <AnimatePresence>
        {disputeModalDraft && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 bg-background/80 backdrop-blur-md"
              onClick={() => setDisputeModalDraft(null)}
            />
            <motion.div
              role="dialog"
              aria-label="Raise Dispute"
              initial={{ opacity: 0, scale: 0.95, y: 15 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 10 }}
              className="glass-strong relative z-10 w-full max-w-md overflow-hidden rounded-3xl border border-red-500/40 p-6 shadow-[0_0_50px_rgba(239,68,68,0.25)]"
            >
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-2.5">
                  <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-red-500/20 text-red-400 border border-red-500/30">
                    <TriangleAlert size={18} />
                  </div>
                  <div>
                    <SectionLabel>Flag Transaction</SectionLabel>
                    <h3 className="font-display text-base font-bold text-foreground">
                      Raise a Dispute
                    </h3>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setDisputeModalDraft(null)}
                  className="rounded-full border border-border p-2 text-muted-foreground hover:text-foreground cursor-pointer"
                >
                  <X size={15} />
                </button>
              </div>

              <form onSubmit={handleDisputeSubmit} className="mt-4 space-y-3">
                <p className="text-xs text-muted-foreground">
                  Disputing <strong>{disputeModalDraft.title}</strong> ({formatMinor(disputeModalDraft.total, currency)}) will pause reconciliation until resolved.
                </p>

                <div>
                  <label className="text-[10px] font-bold uppercase text-muted-foreground">
                    Reason for Dispute *
                  </label>
                  <textarea
                    required
                    rows={3}
                    value={disputeReasonText}
                    onChange={(e) => setDisputeReasonText(e.target.value)}
                    placeholder="e.g. I was not present for this meal, or amount was incorrectly recorded..."
                    className="mt-1 w-full resize-none rounded-xl border border-input bg-background/80 p-3 text-xs outline-none focus:border-red-500"
                    autoFocus
                  />
                </div>

                <div className="flex justify-end gap-2 pt-2">
                  <GhostButton type="button" onClick={() => setDisputeModalDraft(null)} className="text-xs">
                    Cancel
                  </GhostButton>
                  <button
                    type="submit"
                    className="rounded-full bg-red-600 px-4 py-2 text-xs font-bold text-white transition-all hover:bg-red-500 cursor-pointer shadow-md"
                  >
                    Confirm Dispute
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
