import { motion, AnimatePresence } from "framer-motion";
import { Check, Copy, PartyPopper, UserRound, Users, X } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";
import { BrandButton, GhostButton, Pill, SectionLabel } from "./primitives";
import { cn } from "@/lib/utils";

function randomCode() {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  return Array.from({ length: 8 }, () => chars[Math.floor(Math.random() * chars.length)]).join("");
}

export function OnboardingModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [step, setStep] = useState(0);
  const [form, setForm] = useState({ full_name: "", email: "", vpa: "" });
  const [groupName, setGroupName] = useState("");
  const [code, setCode] = useState(randomCode());
  const [copied, setCopied] = useState(false);

  const close = () => {
    onClose();
    setTimeout(() => {
      setStep(0);
      setCopied(false);
      setCode(randomCode());
    }, 250);
  };

  const steps = ["Your details", "Group", "Ready"];

  return (
    <AnimatePresence>
      {open && (
        <>
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={close}
            className="fixed inset-0 z-50 bg-background/75 backdrop-blur-sm"
          />
          <motion.div
            role="dialog"
            aria-label="Create a new group"
            initial={{ opacity: 0, y: 24, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 14, scale: 0.97 }}
            transition={{ type: "spring", stiffness: 320, damping: 30 }}
            className="glass-strong fixed top-1/2 left-1/2 z-50 w-[calc(100%-1.5rem)] max-w-lg -translate-x-1/2 -translate-y-1/2 rounded-3xl p-6"
          >
            <div className="flex items-start justify-between">
              <div>
                <SectionLabel>Onboarding · step {step + 1} of 3</SectionLabel>
                <h2 className="font-display mt-1 text-xl font-bold tracking-tight">
                  {steps[step]}
                </h2>
              </div>
              <button
                type="button"
                onClick={close}
                aria-label="Close onboarding"
                className="rounded-full border border-border p-2 text-muted-foreground transition-colors hover:text-foreground"
              >
                <X size={16} strokeWidth={1.75} />
              </button>
            </div>

            <div className="mt-4 flex gap-1.5" aria-hidden>
              {steps.map((s, i) => (
                <span
                  key={s}
                  className={cn(
                    "h-1.5 flex-1 rounded-full transition-colors",
                    i <= step ? "gradient-brand" : "bg-secondary",
                  )}
                />
              ))}
            </div>

            {step === 0 && (
              <div className="mt-6 space-y-3">
                {(
                  [
                    ["full_name", "Full name", "Sarah Menon", UserRound],
                    ["email", "Email", "sarah@splitsmart.app", UserRound],
                    ["vpa", "UPI VPA", "sarah@upi", Users],
                  ] as const
                ).map(([key, label, placeholder]) => (
                  <div key={key}>
                    <label htmlFor={key} className="label-caps">
                      {label}
                    </label>
                    <input
                      id={key}
                      value={form[key]}
                      onChange={(e) => setForm({ ...form, [key]: e.target.value })}
                      placeholder={placeholder}
                      className="mt-1.5 w-full rounded-xl border border-input bg-background/50 px-3.5 py-2.5 text-sm outline-none transition-colors placeholder:text-muted-foreground focus:border-primary/60 focus:ring-2 focus:ring-ring/40"
                    />
                  </div>
                ))}
                <BrandButton className="mt-2 w-full" onClick={() => setStep(1)}>
                  Continue
                </BrandButton>
              </div>
            )}

            {step === 1 && (
              <div className="mt-6 space-y-4">
                <div>
                  <label htmlFor="group_name" className="label-caps">
                    Group name
                  </label>
                  <input
                    id="group_name"
                    value={groupName}
                    onChange={(e) => setGroupName(e.target.value)}
                    placeholder="Goa Trip '26"
                    className="mt-1.5 w-full rounded-xl border border-input bg-background/50 px-3.5 py-2.5 text-sm outline-none transition-colors placeholder:text-muted-foreground focus:border-primary/60 focus:ring-2 focus:ring-ring/40"
                  />
                </div>
                <div className="glass rounded-2xl p-4">
                  <SectionLabel>Invite code</SectionLabel>
                  <div className="mt-2 flex items-center justify-between gap-3">
                    <p className="font-mono text-2xl font-bold tracking-[0.2em]">{code}</p>
                    <GhostButton onClick={() => setCode(randomCode())}>Regenerate</GhostButton>
                  </div>
                  <p className="mt-2 text-[11px] text-muted-foreground">
                    8-character code · members join instantly, no account juggling.
                  </p>
                </div>
                <div className="flex gap-2">
                  <GhostButton className="flex-1" onClick={() => setStep(0)}>
                    Back
                  </GhostButton>
                  <BrandButton className="flex-1" onClick={() => setStep(2)}>
                    Create group
                  </BrandButton>
                </div>
              </div>
            )}

            {step === 2 && (
              <div className="mt-6 text-center">
                <span className="gradient-brand glow-brand mx-auto flex h-14 w-14 items-center justify-center rounded-2xl">
                  <PartyPopper size={24} strokeWidth={1.75} className="text-primary-foreground" />
                </span>
                <h3 className="font-display mt-4 text-lg font-bold">
                  {groupName || "Your group"} is live
                </h3>
                <p className="mt-1 text-sm text-muted-foreground">
                  Share the link — balances reconcile the moment members join.
                </p>
                <div className="glass mt-5 flex items-center gap-2 rounded-full p-1.5 pl-4">
                  <span className="flex-1 truncate text-left font-mono text-xs text-muted-foreground">
                    splitsmart.app/join/{code}
                  </span>
                  <BrandButton
                    className="px-3 py-2 text-xs"
                    onClick={async () => {
                      try {
                        await navigator.clipboard.writeText(`https://splitsmart.app/join/${code}`);
                        setCopied(true);
                        toast.success("Invite link copied");
                      } catch {
                        toast.error("Clipboard unavailable");
                      }
                    }}
                  >
                    {copied ? <Check size={14} strokeWidth={2} /> : <Copy size={14} strokeWidth={1.75} />}
                    Copy
                  </BrandButton>
                </div>
                <Pill tone="positive" className="mt-4">
                  Ledger initialized · seq #0
                </Pill>
                <BrandButton className="mt-5 w-full" onClick={close}>
                  Go to dashboard
                </BrandButton>
              </div>
            )}
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}
