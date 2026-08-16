import { motion, AnimatePresence } from "framer-motion";
import { Sparkle, Send, Receipt, Wand2, CircleCheck, Loader2, Zap } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";
import { parseNlpInput } from "@/lib/api";
import {
  formatMinor,
  samplePrompts,
  categoryTone,
  type CurrencyCode,
  type Category,
} from "@/lib/splitsmart-data";
import { BrandButton, GhostButton, GlassCard, Pill, SectionLabel, Avatar } from "./primitives";
import { cn } from "@/lib/utils";

type Extracted = {
  payer: string;
  payerInitials: string;
  amount: number;
  category: Category;
  split: "Equal" | "Exact" | "Percentage";
  participants: string[];
  confidence: number;
  title: string;
};

const pipeline = [
  { id: 1, label: "Tier 1: FastPath regex", detail: "deterministic amount + currency capture" },
  { id: 2, label: "Tier 2: ONNX NER", detail: "payer, participants, merchant entities" },
  { id: 3, label: "Tier 3: Structured LLM", detail: "split logic + category normalization" },
];

function extract(text: string): Extracted {
  const amountMatch = text.replace(/,/g, "").match(/(\d+(?:\.\d{1,2})?)/);
  const amount = amountMatch?.[1] ? Math.round(parseFloat(amountMatch[1]) * 100) : 400_000;
  const lower = text.toLowerCase();
  const category: Category = /dinner|food|grocer|lunch|shack|cafe/.test(lower)
    ? "Food"
    : /uber|cab|scooter|taxi|flight|train/.test(lower)
      ? "Transport"
      : /villa|hotel|stay|airbnb|deposit/.test(lower)
        ? "Stay"
        : "Bills";
  const split = /%|percent/.test(lower)
    ? "Percentage"
    : /exact|only|exclude/.test(lower)
      ? "Exact"
      : "Equal";
  const known = [
    ["Rahul", "RV"],
    ["Maya", "MI"],
    ["David", "DR"],
    ["Aisha", "AK"],
  ] as const;
  const participants = ["SM", ...known.filter(([n]) => lower.includes(n.toLowerCase())).map(([, i]) => i)];
  return {
    payer: /paid by david/.test(lower) ? "David Rao" : "Sarah Menon",
    payerInitials: /paid by david/.test(lower) ? "DR" : "SM",
    amount,
    category,
    split,
    participants: participants.length > 1 ? participants : ["SM", "DR", "MI"],
    confidence: split === "Equal" ? 98 : 94,
    title: text.slice(0, 44) || "Untitled expense",
  };
}

export function IngestScreen({
  currency,
  onSubmitDraft,
}: {
  currency: CurrencyCode;
  onSubmitDraft: (e: { title: string; total: number; category: Category; split: Extracted["split"]; participants: string[]; confidence: number; payer: string; payerInitials: string }) => void;
}) {
  const [text, setText] = useState("");
  const [stage, setStage] = useState(-1);
  const [result, setResult] = useState<Extracted | null>(null);

  const handleSampleClick = (prompt: string) => {
    setText("");
    let i = 0;
    const timer = setInterval(() => {
      if (i <= prompt.length) {
        setText(prompt.slice(0, i));
        i++;
      } else {
        clearInterval(timer);
      }
    }, 12);
  };

  const run = async () => {
    if (!text.trim()) {
      toast.error("Add an expense sentence or paste a receipt first");
      return;
    }
    setResult(null);
    setStage(0);
    
    // Animate Pipeline execution
    setTimeout(() => setStage(1), 300);
    setTimeout(() => setStage(2), 600);

    // Call backend API
    const backendDraft = await parseNlpInput(text, "Sarah");
    setTimeout(() => {
      setStage(3);
      if (backendDraft) {
        const catMap: Record<string, Category> = {
          "Food & Dining": "Food",
          "Transport": "Transport",
          "Accommodation": "Stay",
          "Utilities": "Bills",
        };
        const category: Category = catMap[backendDraft.category] || "Food";
        const split: "Equal" | "Exact" | "Percentage" =
          backendDraft.splitLogic === "PERCENTAGE" ? "Percentage" : backendDraft.splitLogic === "EXACT" ? "Exact" : "Equal";

        setResult({
          payer: backendDraft.payerName || "Sarah Menon",
          payerInitials: "SM",
          amount: backendDraft.totalAmountCents,
          category,
          split,
          participants: backendDraft.participants && backendDraft.participants.length > 0 ? backendDraft.participants : ["SM", "DR", "MI"],
          confidence: Math.round(backendDraft.confidenceScore * 100),
          title: backendDraft.description || text.slice(0, 44),
        });
        toast.success(`Extracted via backend (${backendDraft.extractionSource})`);
      } else {
        setResult(extract(text));
      }
      setStage(-1);
    }, 900);
  };

  return (
    <div className="grid gap-5 lg:grid-cols-5">
      <GlassCard className="p-6 lg:col-span-3 relative overflow-hidden">
        <div className="flex items-center gap-2">
          <span className="flex h-8 w-8 items-center justify-center rounded-xl bg-cyan-400 text-black font-extrabold shadow-[0_0_15px_rgba(56,189,248,0.5)]">
            <Sparkle size={16} strokeWidth={2} />
          </span>
          <div>
            <h2 className="font-display font-bold tracking-tight">Conversational ingress</h2>
            <p className="text-[11px] text-muted-foreground">
              Natural language, receipts and raw chat logs &rarr; structured drafts
            </p>
          </div>
        </div>

        <label htmlFor="nlp-input" className="label-caps mt-6 block">
          Unstructured input
        </label>
        
        {/* Dropzone Container with Laser Scan Beam */}
        <div className="relative mt-2 overflow-hidden rounded-2xl">
          {stage >= 0 && (
            <motion.div
              initial={{ y: 0 }}
              animate={{ y: [0, 130, 0] }}
              transition={{ duration: 1.2, repeat: Infinity, ease: "linear" }}
              className="pointer-events-none absolute left-0 right-0 z-20 h-1 bg-cyan-400 shadow-[0_0_20px_#38BDF8]"
            />
          )}

          <textarea
            id="nlp-input"
            value={text}
            onChange={(e) => setText(e.target.value)}
            rows={5}
            placeholder="Paid ₹4,000 for dinner at shacks, split with Rahul & Maya"
            className="w-full resize-none border border-cyan-500/20 bg-background/60 p-4 text-sm outline-none transition-all placeholder:text-muted-foreground focus:border-cyan-400 focus:ring-2 focus:ring-cyan-400/30"
          />
        </div>

        {/* Quick action chips */}
        <SectionLabel className="mt-4">Interactive sample prompt chips</SectionLabel>
        <div className="mt-2 flex flex-wrap gap-2">
          {samplePrompts.map((p) => (
            <button
              key={p}
              type="button"
              onClick={() => handleSampleClick(p)}
              className="rounded-full border border-border bg-secondary/60 px-3.5 py-1.5 text-[11px] font-medium text-muted-foreground transition-all duration-200 hover:scale-105 hover:border-cyan-400 hover:text-cyan-300 hover:shadow-[0_0_12px_rgba(56,189,248,0.25)]"
            >
              ⚡ {p}
            </button>
          ))}
        </div>

        <div className="mt-5 flex flex-wrap items-center gap-2.5">
          <BrandButton onClick={run}>
            <Send size={15} strokeWidth={2} /> Parse expense sentence
          </BrandButton>
          <GhostButton
            onClick={() => {
              handleSampleClick("Receipt · MARTINS BEACH SHACK · 2 x prawn thali 1,200 · beer 800 · total 4,000");
              toast.success("Receipt OCR simulated — text loaded");
            }}
          >
            <Receipt size={15} strokeWidth={1.75} /> Receipt parser
          </GhostButton>
          <GhostButton
            onClick={() => {
              handleSampleClick("Sarah: dinner was 4000\nDavid: I'll pay you back\nMaya: same, split 3 ways please");
              toast.success("Raw chat log loaded");
            }}
          >
            <Wand2 size={15} strokeWidth={1.75} /> Raw chat log
          </GhostButton>
        </div>
      </GlassCard>

      <div className="space-y-5 lg:col-span-2">
        <GlassCard className="p-5" hover={false}>
          <SectionLabel>Pipeline execution</SectionLabel>
          <ul className="mt-3 space-y-2.5">
            {pipeline.map((p, i) => {
              const running = stage === i;
              const done = stage === -1 ? Boolean(result) : stage > i;
              return (
                <li key={p.id} className="flex items-start gap-3">
                  <span
                    className={cn(
                      "relative mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full border transition-colors",
                      done
                        ? "border-cyan-400/40 bg-cyan-400/15 text-cyan-300 font-bold"
                        : running
                          ? "border-cyan-400/50 bg-cyan-400/20 text-cyan-300"
                          : "border-border bg-secondary/50 text-muted-foreground",
                    )}
                  >
                    {running && (
                      <motion.span
                        aria-hidden
                        animate={{ scale: [1, 1.55], opacity: [0.55, 0] }}
                        transition={{ duration: 1.1, repeat: Infinity }}
                        className="absolute inset-0 rounded-full bg-cyan-400/40"
                      />
                    )}
                    {done ? (
                      <CircleCheck size={15} strokeWidth={2} />
                    ) : running ? (
                      <Loader2 size={14} strokeWidth={2} className="animate-spin text-cyan-300" />
                    ) : (
                      <span className="text-xs font-bold">{p.id}</span>
                    )}
                  </span>
                  <div>
                    <p className="text-xs font-bold">{p.label}</p>
                    <p className="text-[11px] text-muted-foreground">{p.detail}</p>
                  </div>
                </li>
              );
            })}
          </ul>
        </GlassCard>

        {/* Real-Time Token Extracted Preview */}
        <AnimatePresence>
          {result && (
            <motion.div
              initial={{ opacity: 0, y: 15, scale: 0.96 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              transition={{ duration: 0.3 }}
            >
              <GlassCard className="p-5 border-yellow-500/40 shadow-[0_0_30px_rgba(250,204,21,0.2)]">
                <div className="flex items-center justify-between">
                  <SectionLabel>Extracted draft preview</SectionLabel>
                  <Pill tone="brand">
                    <Zap size={12} /> {result.confidence}% entity confidence
                  </Pill>
                </div>

                <p className="font-display mt-2 text-xl font-bold">{result.title}</p>

                <div className="mt-3 flex items-baseline gap-2">
                  <span className="font-display text-3xl font-extrabold text-yellow-400 tabular-nums">
                    {formatMinor(result.amount, currency)}
                  </span>
                  <span className="text-xs text-muted-foreground">total</span>
                </div>

                {/* Entity Tokens Grid */}
                <div className="mt-4 grid grid-cols-2 gap-2 text-xs">
                  <div className="rounded-xl border border-yellow-500/20 bg-secondary/50 p-2.5">
                    <span className="text-muted-foreground text-[10px] uppercase font-semibold">Payer</span>
                    <p className="font-bold text-yellow-400">{result.payer}</p>
                  </div>
                  <div className="rounded-xl border border-yellow-500/20 bg-secondary/50 p-2.5">
                    <span className="text-muted-foreground text-[10px] uppercase font-semibold">Split Logic</span>
                    <p className="font-bold text-foreground">{result.split}</p>
                  </div>
                </div>

                <div className="mt-4 flex items-center justify-between">
                  <div>
                    <SectionLabel>Participants</SectionLabel>
                    <div className="mt-1 flex items-center gap-1">
                      {result.participants.map((p) => (
                        <Avatar key={p} initials={p} size="sm" />
                      ))}
                    </div>
                  </div>
                  <Pill className={categoryTone[result.category]}>{result.category}</Pill>
                </div>

                <BrandButton
                  onClick={() =>
                    onSubmitDraft({
                      title: result.title,
                      total: result.amount,
                      category: result.category,
                      split: result.split,
                      participants: result.participants,
                      confidence: result.confidence,
                      payer: result.payer,
                      payerInitials: result.payerInitials,
                    })
                  }
                  className="mt-5 w-full justify-center py-3 font-extrabold"
                >
                  Submit draft to consensus &rarr;
                </BrandButton>
              </GlassCard>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
