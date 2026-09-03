import { motion, AnimatePresence } from "framer-motion";
import { QRCodeSVG } from "qrcode.react";
import confetti from "canvas-confetti";
import { ArrowRight, Copy, Check, Zap, X, Sparkle, ShieldCheck } from "lucide-react";
import { useState, useRef } from "react";
import { toast } from "sonner";
import { formatMinor, upiString, type CurrencyCode, type Transfer } from "@/lib/splitsmart-data";
import { Avatar, BrandButton, GhostButton, GlassCard, Pill, SectionLabel } from "./primitives";

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

  const handleCardMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!cardRef.current) return;
    const rect = cardRef.current.getBoundingClientRect();
    const x = (e.clientX - rect.left) / rect.width - 0.5;
    const y = (e.clientY - rect.top) / rect.height - 0.5;
    setTilt({ x: x * 14, y: -y * 14 });
  };

  const handleCardMouseLeave = () => setTilt({ x: 0, y: 0 });

  const copy = async (label: string, value: string) => {
    try {
      await navigator.clipboard.writeText(value);
      setCopied(label);
      toast.success(`${label} copied`);
      setTimeout(() => setCopied(null), 1600);
    } catch {
      toast.error("Clipboard unavailable in this browser");
    }
  };

  const celebrate = () => {
    void confetti({
      particleCount: 120,
      spread: 78,
      origin: { y: 0.7 },
      colors: ["#A855F7", "#6366F1", "#10B981", "#ffffff"],
    });
  };

  return (
    <div className="space-y-5">
      <GlassCard className="p-6" hover={false}>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="font-display text-lg font-bold tracking-tight">
              Pairwise debt minimization
            </h2>
            <p className="text-xs text-muted-foreground">
              6 raw obligations collapsed into {open.length} optimal transfers.
            </p>
          </div>
          <Pill tone="positive">
            <Zap size={13} strokeWidth={1.75} /> Minimum transaction optimization · 3 steps saved
          </Pill>
        </div>

        <ul className="mt-5 grid gap-3 lg:grid-cols-2">
          {transfers.map((t) => (
            <li key={t.id}>
              <div className="glass card-hover flex flex-wrap items-center justify-between gap-4 rounded-2xl p-4 border border-purple-500/20">
                <div className="flex items-center gap-3">
                  <Avatar initials={t.fromInitials} />
                  <ArrowRight size={16} strokeWidth={2} className="text-purple-400" />
                  <Avatar initials={t.toInitials} />
                  <div className="ml-1">
                    <p className="text-sm font-semibold">
                      {t.from.split(" ")[0]} → {t.to.split(" ")[0]}
                    </p>
                    <p className="text-[11px] text-muted-foreground font-mono">{t.toVpa}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <p className="font-display font-bold tabular-nums">
                    {formatMinor(t.amount, currency)}
                  </p>
                  {t.settled ? (
                    <Pill tone="positive">
                      <Check size={13} strokeWidth={2} /> settled
                    </Pill>
                  ) : (
                    <BrandButton className="px-3.5 py-2 text-xs" onClick={() => setActive(t)}>
                      Pay now
                    </BrandButton>
                  )}
                </div>
              </div>
            </li>
          ))}
        </ul>
      </GlassCard>

      <div className="grid gap-4 sm:grid-cols-3">
        {[
          {
            label: "Open settlement value",
            value: formatMinor(open.reduce((a, t) => a + t.amount, 0), currency),
          },
          {
            label: "Settled this cycle",
            value: formatMinor(
              transfers.filter((t) => t.settled).reduce((a, t) => a + t.amount, 0),
              currency
            ),
          },
          { label: "Rail", value: "UPI P2P · instant" },
        ].map((s) => (
          <GlassCard key={s.label} className="p-5">
            <SectionLabel>{s.label}</SectionLabel>
            <p className="font-display mt-1.5 text-xl font-bold tabular-nums">{s.value}</p>
          </GlassCard>
        ))}
      </div>

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
              initial={{ opacity: 0, scale: 0.94, y: 18 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.96, y: 10 }}
              transition={{ type: "spring", stiffness: 340, damping: 30 }}
              className="glass-strong fixed top-1/2 left-1/2 z-50 w-[calc(100%-1.5rem)] max-w-md -translate-x-1/2 -translate-y-1/2 rounded-3xl p-6 border border-purple-500/30 shadow-[0_0_50px_rgba(168,85,247,0.3)]"
            >
              <div
                ref={cardRef}
                onMouseMove={handleCardMouseMove}
                onMouseLeave={handleCardMouseLeave}
                style={{
                  transform: `perspective(1000px) rotateX(${tilt.y}deg) rotateY(${tilt.x}deg)`,
                  transition: "transform 0.1s ease-out",
                }}
              >
                <div className="flex items-start justify-between">
                  <div>
                    <SectionLabel>Dynamic UPI terminal</SectionLabel>
                    <h3 className="font-display mt-1 text-xl font-bold">
                      Pay {active.to.split(" ")[0]}
                    </h3>
                  </div>
                  <button
                    type="button"
                    onClick={() => setActive(null)}
                    aria-label="Close payment terminal"
                    className="rounded-full border border-border p-2 text-muted-foreground transition-colors hover:text-foreground cursor-pointer"
                  >
                    <X size={16} strokeWidth={1.75} />
                  </button>
                </div>

                <div className="mt-5 flex flex-col items-center">
                  <div className="rounded-2xl border border-border bg-foreground p-3 shadow-xl">
                    <QRCodeSVG
                      value={upiString(active.toVpa, active.to, active.amount)}
                      size={188}
                      bgColor="transparent"
                      fgColor="#090D16"
                      level="M"
                    />
                  </div>
                  <p className="font-display mt-4 text-3xl font-extrabold tabular-nums">
                    {formatMinor(active.amount, currency)}
                  </p>
                  <p className="mt-1 text-xs text-muted-foreground font-mono">to {active.toVpa}</p>
                </div>

                <div className="mt-5 grid grid-cols-2 gap-2">
                  <GhostButton onClick={() => copy("Payee VPA", active.toVpa)}>
                    {copied === "Payee VPA" ? (
                      <Check size={15} strokeWidth={2} className="text-positive" />
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
                      <Check size={15} strokeWidth={2} className="text-positive" />
                    ) : (
                      <Copy size={15} strokeWidth={1.75} />
                    )}
                    Copy UPI string
                  </GhostButton>
                </div>

                <a
                  href={upiString(active.toVpa, active.to, active.amount)}
                  className="glass mt-2 flex items-center justify-center gap-2 rounded-full px-4 py-2.5 text-sm font-semibold transition-colors hover:border-primary/50 cursor-pointer"
                >
                  <Sparkle size={15} strokeWidth={1.75} className="text-primary" /> Pay via app
                </a>

                <BrandButton
                  className="mt-2 w-full"
                  onClick={() => {
                    onSettle(active.id);
                    celebrate();
                    setActive(null);
                  }}
                >
                  <ShieldCheck size={16} strokeWidth={1.75} /> Mark as settled
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
