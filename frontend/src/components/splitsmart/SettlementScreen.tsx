import { motion, AnimatePresence } from "framer-motion";
import { QRCodeSVG } from "qrcode.react";
import { ArrowRight, Copy, Check, Zap, X, Sparkle, ShieldCheck, Network } from "lucide-react";
import { useState, useRef } from "react";
import { toast } from "sonner";
import { postBackendSettlement } from "@/lib/api";
import { formatMinor, upiString, type CurrencyCode, type Transfer } from "@/lib/splitsmart-data";
import { Avatar, BrandButton, GhostButton, GlassCard, Pill, SectionLabel, triggerGoldenConfetti } from "./primitives";

/* Interactive 3D Debt Graph Visualizer Component */
function DebtMinimizationGraph({ transfers, currency }: { transfers: Transfer[]; currency: CurrencyCode }) {
  const nodes = [
    { id: "SM", name: "Sarah", x: 80, y: 70 },
    { id: "DR", name: "David", x: 320, y: 70 },
    { id: "MI", name: "Maya", x: 320, y: 210 },
    { id: "RV", name: "Rahul", x: 80, y: 210 },
  ];

  return (
    <div className="relative overflow-hidden rounded-2xl border border-cyan-500/25 bg-background/80 p-5 backdrop-blur-md">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-cyan-400 text-black font-extrabold shadow-[0_0_12px_rgba(56,189,248,0.5)]">
            <Network size={15} />
          </span>
          <SectionLabel>Bitmask DP Debt Minimization Graph</SectionLabel>
        </div>
        <Pill tone="brand">
          <Zap size={12} /> Minimized to {transfers.filter((t) => !t.settled).length} optimal links
        </Pill>
      </div>

      {/* SVG Interactive Vector Graph with Animated Pulse Lasers */}
      <div className="relative mt-4 h-64 w-full">
        <svg viewBox="0 0 400 280" className="h-full w-full">
          <defs>
            <linearGradient id="cyan-vector" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#38BDF8" stopOpacity="0.85" />
              <stop offset="100%" stopColor="#0EA5E9" stopOpacity="0.3" />
            </linearGradient>
            <filter id="glow-cyan-filter">
              <feGaussianBlur stdDeviation="3" result="coloredBlur" />
              <feMerge>
                <feMergeNode in="coloredBlur" />
                <feMergeNode in="SourceGraphic" />
              </feMerge>
            </filter>
          </defs>

          {/* Draw connecting vector paths */}
          {transfers.map((t, idx) => {
            const fromNode = nodes.find((n) => n.id === t.fromInitials) || nodes[0];
            const toNode = nodes.find((n) => n.id === t.toInitials) || nodes[1];

            const pathId = `path-${idx}`;
            const pathD = `M ${fromNode.x} ${fromNode.y} Q ${(fromNode.x + toNode.x) / 2} ${(fromNode.y + toNode.y) / 2 - 20} ${toNode.x} ${toNode.y}`;

            return (
              <g key={t.id}>
                {/* Background Line */}
                <path
                  id={pathId}
                  d={pathD}
                  fill="none"
                  stroke="url(#cyan-vector)"
                  strokeWidth={t.settled ? "1.5" : "3"}
                  strokeDasharray={t.settled ? "4,4" : undefined}
                  filter="url(#glow-cyan-filter)"
                />

                {/* Animated Cyan Laser Pulse along vector */}
                {!t.settled && (
                  <circle r="4" fill="#38BDF8">
                    <animateMotion dur="2.2s" repeatCount="indefinite" path={pathD} />
                  </circle>
                )}

                {/* Amount Label on Path */}
                <text
                  x={(fromNode.x + toNode.x) / 2}
                  y={(fromNode.y + toNode.y) / 2 - 28}
                  fill="#38BDF8"
                  fontSize="10"
                  fontWeight="bold"
                  textAnchor="middle"
                  className="font-mono"
                >
                  {formatMinor(t.amount, currency)}
                </text>
              </g>
            );
          })}

          {/* Member Orb Nodes */}
          {nodes.map((n) => (
            <g key={n.id} className="cursor-pointer transition-transform duration-300 hover:scale-110">
              <circle
                cx={n.x}
                cy={n.y}
                r="22"
                fill="#0A0E1A"
                stroke="#38BDF8"
                strokeWidth="2"
                filter="url(#glow-cyan-filter)"
              />
              <text
                x={n.x}
                y={n.y + 4}
                fill="#FFFFFF"
                fontSize="11"
                fontWeight="bold"
                textAnchor="middle"
              >
                {n.id}
              </text>
            </g>
          ))}
        </svg>
      </div>
    </div>
  );
}

export function SettlementScreen({
  currency,
  transfers,
  onSettle,
}: {
  currency: CurrencyCode;
  transfers: Transfer[];
  onSettle: (id: string) => void;
}) {
  const [active, setActive] = useState<Transfer | null>(null);
  const [copied, setCopied] = useState<string | null>(null);
  const [tilt, setTilt] = useState({ x: 0, y: 0 });
  const cardRef = useRef<HTMLDivElement>(null);
  const open = transfers.filter((t) => !t.settled);

  const copy = (label: string, text: string) => {
    navigator.clipboard.writeText(text);
    setCopied(label);
    toast.success(`Copied ${label} to clipboard`);
    setTimeout(() => setCopied(null), 1800);
  };

  const handleCardMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!cardRef.current) return;
    const rect = cardRef.current.getBoundingClientRect();
    const cx = rect.left + rect.width / 2;
    const cy = rect.top + rect.height / 2;
    const dx = (e.clientX - cx) / (rect.width / 2);
    const dy = (e.clientY - cy) / (rect.height / 2);
    setTilt({ x: dx * 10, y: -dy * 10 });
  };

  const handleCardMouseLeave = () => setTilt({ x: 0, y: 0 });

  return (
    <div className="space-y-6">
      {/* Interactive Graph Visualizer */}
      <DebtMinimizationGraph transfers={transfers} currency={currency} />

      <GlassCard className="p-6">
        <div className="flex items-center justify-between">
          <div>
            <SectionLabel>Direct 1-Tap UPI Transfers</SectionLabel>
            <h2 className="font-display mt-1 text-2xl font-extrabold text-foreground">
              Optimized Settle-Up Queue
            </h2>
          </div>
          <Pill tone="brand">paise-exact precision</Pill>
        </div>

        <ul className="mt-5 grid gap-3 lg:grid-cols-2">
          {transfers.map((t) => (
            <li key={t.id}>
              <div className="glass card-hover flex flex-wrap items-center justify-between gap-4 rounded-2xl p-4 transition-all duration-300 hover:scale-102 hover:border-emerald-400">
                <div className="flex items-center gap-3">
                  <Avatar initials={t.fromInitials} />
                  <ArrowRight size={16} strokeWidth={2} className="text-emerald-400" />
                  <Avatar initials={t.toInitials} />
                  <div className="ml-1">
                    <p className="text-sm font-bold text-foreground">
                      {t.from.split(" ")[0]} &rarr; {t.to.split(" ")[0]}
                    </p>
                    <p className="text-[11px] text-muted-foreground">{t.toVpa}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <p className="font-display font-extrabold text-foreground text-base sm:text-lg tabular-nums">
                    {formatMinor(t.amount, currency)}
                  </p>
                  {t.settled ? (
                    <Pill tone="positive">
                      <Check size={13} strokeWidth={2} /> settled
                    </Pill>
                  ) : (
                    <button
                      type="button"
                      onClick={() => setActive(t)}
                      className="inline-flex items-center gap-1.5 rounded-full bg-gradient-to-r from-purple-600 via-indigo-600 to-cyan-500 px-4 py-2 text-xs font-extrabold text-white shadow-[0_0_18px_rgba(168,85,247,0.4)] transition-all hover:scale-105 hover:shadow-[0_0_22px_rgba(56,189,248,0.5)] active:scale-95 cursor-pointer"
                    >
                      <Sparkle size={13} strokeWidth={2} /> Pay now
                    </button>
                  )}
                </div>
              </div>
            </li>
          ))}
        </ul>
      </GlassCard>

      <div className="grid gap-4 sm:grid-cols-3">
        {[
          { label: "Open settlement value", value: formatMinor(open.reduce((a, t) => a + t.amount, 0), currency) },
          { label: "Settled this cycle", value: formatMinor(transfers.filter((t) => t.settled).reduce((a, t) => a + t.amount, 0), currency) },
          { label: "Rail", value: "UPI P2P · instant" },
        ].map((s) => (
          <GlassCard key={s.label} className="p-5">
            <SectionLabel>{s.label}</SectionLabel>
            <p className="font-display mt-1.5 text-2xl font-extrabold tabular-nums text-foreground">{s.value}</p>
          </GlassCard>
        ))}
      </div>

      {/* 3D TILT HOLOGRAPHIC UPI QR TERMINAL MODAL */}
      <AnimatePresence>
        {active && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 z-50 bg-background/80 backdrop-blur-md"
              onClick={() => setActive(null)}
            />
            <motion.div
              role="dialog"
              aria-label="UPI payment terminal"
              initial={{ opacity: 0, scale: 0.92, y: 18 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.94, y: 10 }}
              transition={{ type: "spring", stiffness: 340, damping: 30 }}
              className="fixed top-1/2 left-1/2 z-50 w-[calc(100%-1.5rem)] max-w-md -translate-x-1/2 -translate-y-1/2"
            >
              <div
                ref={cardRef}
                onMouseMove={handleCardMouseMove}
                onMouseLeave={handleCardMouseLeave}
                style={{
                  transform: `perspective(1000px) rotateX(${tilt.y}deg) rotateY(${tilt.x}deg)`,
                  transition: "transform 0.1s ease-out",
                }}
                className="glass-strong relative overflow-hidden rounded-3xl border border-emerald-500/40 p-6 shadow-[0_0_50px_rgba(16,185,129,0.35)]"
              >
                {/* Specular emerald glare overlay */}
                <div
                  aria-hidden
                  className="pointer-events-none absolute -top-32 -left-32 h-64 w-64 rounded-full bg-emerald-500/20 blur-3xl"
                />

                <div className="flex items-start justify-between">
                  <div>
                    <SectionLabel>3D Holographic UPI Terminal</SectionLabel>
                    <h3 className="font-display mt-1 text-xl font-extrabold text-foreground">
                      Pay {active.to.split(" ")[0]}
                    </h3>
                  </div>
                  <button
                    type="button"
                    onClick={() => setActive(null)}
                    aria-label="Close payment terminal"
                    className="rounded-full border border-border p-2 text-muted-foreground transition-all hover:border-emerald-400 hover:text-emerald-300"
                  >
                    <X size={16} strokeWidth={2} />
                  </button>
                </div>

                <div className="mt-5 flex flex-col items-center">
                  <div className="relative rounded-2xl border-2 border-emerald-400 bg-white p-3.5 shadow-[0_0_25px_rgba(16,185,129,0.4)]">
                    <QRCodeSVG
                      value={upiString(active.toVpa, active.to, active.amount)}
                      size={188}
                      bgColor="transparent"
                      fgColor="#090D16"
                      level="M"
                    />
                  </div>
                  <p className="font-display mt-4 text-3xl font-extrabold text-emerald-400 tabular-nums">
                    {formatMinor(active.amount, currency)}
                  </p>
                  <p className="mt-1 text-xs text-muted-foreground font-semibold">to {active.toVpa}</p>
                </div>

                <div className="mt-5 grid grid-cols-2 gap-2">
                  <GhostButton onClick={() => copy("Payee VPA", active.toVpa)}>
                    {copied === "Payee VPA" ? (
                      <Check size={15} strokeWidth={2} className="text-emerald-400" />
                    ) : (
                      <Copy size={15} strokeWidth={1.75} />
                    )}
                    Copy VPA
                  </GhostButton>
                  <GhostButton
                    onClick={() =>
                      copy("UPI string", upiString(active.toVpa, active.to, active.amount))
                    }
                  >
                    {copied === "UPI string" ? (
                      <Check size={15} strokeWidth={2} className="text-emerald-400" />
                    ) : (
                      <Copy size={15} strokeWidth={1.75} />
                    )}
                    Copy UPI string
                  </GhostButton>
                </div>

                <a
                  href={upiString(active.toVpa, active.to, active.amount)}
                  className="glass mt-2 flex items-center justify-center gap-2 rounded-full px-4 py-2.5 text-sm font-bold transition-all hover:border-emerald-400 hover:text-emerald-300"
                >
                  <Sparkle size={15} strokeWidth={2} className="text-emerald-400" /> Pay via UPI App
                </a>

                <BrandButton
                  className="mt-3 w-full justify-center py-3 text-base font-extrabold"
                  onClick={async () => {
                    await postBackendSettlement({
                      groupId: "g1",
                      debtorId: active.from,
                      creditorId: active.to,
                      amountCents: active.amount,
                      note: `UPI Settlement to ${active.toVpa}`,
                    });
                    onSettle(active.id);
                    triggerGoldenConfetti();
                    toast.success("Settlement committed to the ledger 🎉");
                    setActive(null);
                  }}
                >
                  <ShieldCheck size={18} strokeWidth={2} /> Mark as settled via UPI
                </BrandButton>
                <p className="mt-3 text-center text-[11px] text-muted-foreground">
                  Marking settled appends an immutable SettlementMarked event to the ledger.
                </p>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
}
