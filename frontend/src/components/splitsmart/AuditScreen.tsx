import { motion, AnimatePresence } from "framer-motion";
import { Search, ChevronDown, Radio } from "lucide-react";
import { useMemo, useState } from "react";
import { eventTone, type EventType, type LedgerEvent } from "@/lib/splitsmart-data";
import { GlassCard, Pill, SectionLabel } from "./primitives";
import { cn } from "@/lib/utils";

const filters: (EventType | "All")[] = [
  "All",
  "DraftCreated",
  "DraftApproved",
  "ConflictResolved",
  "LedgerCommitted",
  "SettlementMarked",
];

export function AuditScreen({ events }: { events: LedgerEvent[] }) {
  const [filter, setFilter] = useState<EventType | "All">("All");
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState<string | null>(null);

  const visible = useMemo(
    () =>
      events.filter(
        (e) =>
          (filter === "All" || e.type === filter) &&
          (query.trim() === "" ||
            `${e.summary} ${e.actor} ${e.type}`.toLowerCase().includes(query.toLowerCase())),
      ),
    [events, filter, query],
  );

  return (
    <div className="space-y-5">
      <GlassCard className="flex flex-col gap-3 p-4 lg:flex-row lg:items-center" hover={false}>
        <div className="relative flex-1">
          <Search
            size={15}
            strokeWidth={1.75}
            className="absolute top-1/2 left-3 -translate-y-1/2 text-muted-foreground"
          />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            aria-label="Search audit events"
            placeholder="Search events, actors, payloads…"
            className="w-full rounded-full border border-input bg-background/50 py-2.5 pr-4 pl-9 text-sm outline-none transition-colors placeholder:text-muted-foreground focus:border-primary/60 focus:ring-2 focus:ring-ring/40"
          />
        </div>
        <div className="flex flex-wrap gap-1.5" role="group" aria-label="Filter by event type">
          {filters.map((f) => (
            <button
              key={f}
              type="button"
              onClick={() => setFilter(f)}
              aria-pressed={filter === f}
              className={cn(
                "rounded-full border px-3 py-1.5 text-[11px] font-semibold transition-colors",
                filter === f
                  ? "gradient-brand border-transparent text-primary-foreground"
                  : "border-border bg-secondary/50 text-muted-foreground hover:text-foreground",
              )}
            >
              {f}
            </button>
          ))}
        </div>
      </GlassCard>

      <div className="flex items-center justify-between">
        <h2 className="font-display text-lg font-bold tracking-tight">Immutable event stream</h2>
        <Pill tone="brand">
          <Radio size={13} strokeWidth={1.75} /> append-only · {visible.length} shown
        </Pill>
      </div>

      <ol className="relative space-y-3 pl-6">
        <span
          aria-hidden
          className="absolute top-2 bottom-2 left-[7px] w-px bg-gradient-to-b from-primary/60 via-border to-transparent"
        />
        {visible.map((e) => {
          const expanded = open === e.id;
          return (
            <li key={e.id} className="relative">
              <span
                aria-hidden
                className={cn(
                  "absolute top-6 -left-[22px] h-3 w-3 rounded-full border-2 border-background",
                  e.type === "SettlementMarked" || e.type === "DraftApproved"
                    ? "bg-positive shadow-[0_0_12px_var(--positive)]"
                    : e.type === "ConflictResolved"
                      ? "bg-negative shadow-[0_0_12px_var(--negative)]"
                      : "bg-primary shadow-[0_0_12px_var(--primary)]",
                )}
              />
              <GlassCard className="p-4">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <p className={cn("text-sm font-bold", eventTone[e.type])}>{e.type}</p>
                    <p className="mt-0.5 text-sm text-foreground/90">{e.summary}</p>
                    <p className="mt-1 text-[11px] text-muted-foreground">
                      seq #{e.seq} · {e.actor} · {e.at}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => setOpen(expanded ? null : e.id)}
                    aria-expanded={expanded}
                    className="inline-flex items-center gap-1 rounded-full border border-border px-2.5 py-1 text-[11px] font-semibold text-muted-foreground transition-colors hover:text-foreground"
                  >
                    Raw JSON
                    <ChevronDown
                      size={13}
                      strokeWidth={1.75}
                      className={cn("transition-transform", expanded && "rotate-180")}
                    />
                  </button>
                </div>
                <AnimatePresence initial={false}>
                  {expanded && (
                    <motion.pre
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: "auto", opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={{ duration: 0.22 }}
                      className="mt-3 overflow-x-auto rounded-xl border border-border bg-background/60 p-3 font-mono text-[11px] leading-relaxed text-muted-foreground"
                    >
                      {JSON.stringify(e.payload, null, 2)}
                    </motion.pre>
                  )}
                </AnimatePresence>
              </GlassCard>
            </li>
          );
        })}
        {visible.length === 0 && (
          <li>
            <GlassCard className="p-6 text-center" hover={false}>
              <SectionLabel>No events match this filter</SectionLabel>
            </GlassCard>
          </li>
        )}
      </ol>
    </div>
  );
}
