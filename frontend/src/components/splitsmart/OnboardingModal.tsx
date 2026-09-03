import { motion, AnimatePresence } from "framer-motion";
import { Check, Copy, PartyPopper, Plus, Sparkles, X, Smile } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";
import { BrandButton, GhostButton, Pill, SectionLabel } from "./primitives";
import { cn } from "@/lib/utils";

function randomCode() {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  return Array.from({ length: 8 }, () => chars[Math.floor(Math.random() * chars.length)]).join("");
}

const defaultEmojis = ["🏝️", "🏠", "🍕", "✈️", "🎯", "🎉", "⛺", "🚀"];

const extendedEmojis = [
  "🍔", "🍻", "☕", "🍣", "🌮", "🍩", "🥑",
  "🚗", "🚆", "🚢", "🏖️", "🏔️", "🏕️", "🏨",
  "🎮", "🎸", "🍿", "⚽", "🏋️", "🚴", "🧘",
  "🎓", "💼", "🛒", "💡", "🐾", "🎂", "🥂",
  "💎", "🎁", "🔥", "✨", "❤️", "⚡", "🌟",
];

export function OnboardingModal({
  open,
  onClose,
  user,
  onGroupCreated,
}: {
  open: boolean;
  onClose: () => void;
  user?: { name: string; email: string } | null;
  onGroupCreated?: (group: any) => void;
}) {
  const [step, setStep] = useState(0);
  const [groupName, setGroupName] = useState("");
  const [selectedEmoji, setSelectedEmoji] = useState("🏝️");
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [customInput, setCustomInput] = useState("");
  const [code, setCode] = useState(randomCode());
  const [copied, setCopied] = useState(false);

  const close = () => {
    onClose();
    setTimeout(() => {
      setStep(0);
      setGroupName("");
      setSelectedEmoji("🏝️");
      setShowEmojiPicker(false);
      setCustomInput("");
      setCopied(false);
      setCode(randomCode());
    }, 250);
  };

  const steps = ["Create Group", "Ready"];

  const handleCreate = () => {
    if (!groupName.trim()) {
      toast.error("Please enter a group name");
      return;
    }
    const currentUser = user || { name: "User", email: "user@splitsmart.app" };
    import("@/lib/splitsmart-data").then(({ addGroupToStore }) => {
      const created = addGroupToStore(groupName.trim(), selectedEmoji, code, currentUser);
      onGroupCreated?.(created);
      setStep(1);
      toast.success(`Group "${groupName}" created! 🎉`);
    });
  };

  const handleCustomEmojiSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (customInput.trim()) {
      setSelectedEmoji(customInput.trim());
      setShowEmojiPicker(false);
      setCustomInput("");
    }
  };

  return (
    <AnimatePresence>
      {open && (
        <>
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={close}
            className="fixed inset-0 z-50 bg-background/80 backdrop-blur-md"
          />
          <motion.div
            role="dialog"
            aria-label="Create a new group"
            initial={{ opacity: 0, y: 24, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 14, scale: 0.97 }}
            transition={{ type: "spring", stiffness: 320, damping: 30 }}
            className="glass-strong fixed top-1/2 left-1/2 z-50 w-[calc(100%-1.5rem)] max-w-md -translate-x-1/2 -translate-y-1/2 rounded-3xl p-6 border border-purple-500/30 shadow-[0_0_50px_rgba(0,0,0,0.5)]"
          >
            <div className="flex items-start justify-between">
              <div>
                <SectionLabel>New Group · Step {step + 1} of 2</SectionLabel>
                <h2 className="font-display mt-1 text-xl font-extrabold tracking-tight">
                  {steps[step]}
                </h2>
              </div>
              <button
                type="button"
                onClick={close}
                aria-label="Close modal"
                className="rounded-full border border-border p-2 text-muted-foreground transition-all hover:border-purple-400 hover:text-foreground cursor-pointer"
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
              <div className="mt-6 space-y-4">
                {/* Group Name Input */}
                <div>
                  <label htmlFor="group_name" className="label-caps">
                    Group name
                  </label>
                  <input
                    id="group_name"
                    value={groupName}
                    onChange={(e) => setGroupName(e.target.value)}
                    placeholder="e.g. Goa Trip '26, Flat 402 Bills"
                    autoFocus
                    className="mt-1.5 w-full rounded-xl border border-input bg-background/60 px-3.5 py-2.5 text-sm outline-none transition-colors placeholder:text-muted-foreground focus:border-primary focus:ring-1 focus:ring-primary"
                  />
                </div>

                {/* Emoji Selection with Plus Button */}
                <div>
                  <div className="flex items-center justify-between">
                    <label className="label-caps">Group Icon</label>
                    <span className="text-[11px] text-muted-foreground font-mono">
                      Selected: {selectedEmoji}
                    </span>
                  </div>
                  <div className="mt-1.5 flex flex-wrap items-center gap-2">
                    {defaultEmojis.map((emoji) => (
                      <button
                        key={emoji}
                        type="button"
                        onClick={() => {
                          setSelectedEmoji(emoji);
                          setShowEmojiPicker(false);
                        }}
                        className={cn(
                          "flex h-9 w-9 items-center justify-center rounded-xl border text-base transition-all cursor-pointer",
                          selectedEmoji === emoji
                            ? "border-purple-500 bg-purple-500/20 scale-110 shadow-md ring-2 ring-purple-500/40"
                            : "border-border bg-secondary/60 hover:border-purple-400/50 hover:bg-secondary",
                        )}
                      >
                        {emoji}
                      </button>
                    ))}

                    {/* Custom Selected Emoji if not in default list */}
                    {!defaultEmojis.includes(selectedEmoji) && (
                      <button
                        type="button"
                        className="flex h-9 w-9 items-center justify-center rounded-xl border border-purple-500 bg-purple-500/20 text-base scale-110 shadow-md ring-2 ring-purple-500/40"
                      >
                        {selectedEmoji}
                      </button>
                    )}

                    {/* Plus Button to choose any other emoji */}
                    <button
                      type="button"
                      title="Choose more emojis"
                      aria-label="Choose any other emoji"
                      onClick={() => setShowEmojiPicker((v) => !v)}
                      className={cn(
                        "flex h-9 w-9 items-center justify-center rounded-xl border border-dashed text-sm font-bold transition-all cursor-pointer",
                        showEmojiPicker
                          ? "border-purple-400 bg-purple-500/20 text-purple-300"
                          : "border-purple-500/40 bg-secondary/40 text-purple-400 hover:border-purple-400 hover:bg-purple-500/10 hover:scale-105",
                      )}
                    >
                      <Plus size={16} strokeWidth={2.5} />
                    </button>
                  </div>

                  {/* Expandable Emoji Picker Grid */}
                  <AnimatePresence>
                    {showEmojiPicker && (
                      <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: "auto" }}
                        exit={{ opacity: 0, height: 0 }}
                        className="mt-3 overflow-hidden rounded-2xl border border-purple-500/30 bg-secondary/70 p-3 shadow-lg"
                      >
                        <div className="flex items-center justify-between mb-2 pb-1.5 border-b border-border/40">
                          <span className="text-[11px] font-bold text-muted-foreground">
                            Pick an icon or paste any emoji:
                          </span>
                          <button
                            type="button"
                            onClick={() => setShowEmojiPicker(false)}
                            className="text-muted-foreground hover:text-foreground text-xs"
                          >
                            <X size={13} />
                          </button>
                        </div>

                        {/* Extended Grid */}
                        <div className="grid grid-cols-7 gap-1.5 max-h-36 overflow-y-auto pr-1">
                          {extendedEmojis.map((emoji, idx) => (
                            <button
                              key={idx}
                              type="button"
                              onClick={() => {
                                setSelectedEmoji(emoji);
                                setShowEmojiPicker(false);
                              }}
                              className={cn(
                                "flex h-8 w-8 items-center justify-center rounded-lg text-sm transition-all hover:bg-purple-500/20 hover:scale-110 cursor-pointer",
                                selectedEmoji === emoji && "bg-purple-500/30 ring-1 ring-purple-400"
                              )}
                            >
                              {emoji}
                            </button>
                          ))}
                        </div>

                        {/* Custom Emoji Input Form */}
                        <form
                          onSubmit={handleCustomEmojiSubmit}
                          className="mt-2.5 pt-2 border-t border-border/40 flex items-center gap-1.5"
                        >
                          <input
                            type="text"
                            placeholder="Type or paste any emoji (e.g. 🛸)"
                            value={customInput}
                            onChange={(e) => setCustomInput(e.target.value)}
                            maxLength={4}
                            className="flex-1 rounded-lg border border-input bg-background/80 px-2.5 py-1 text-xs outline-none focus:border-primary"
                          />
                          <button
                            type="submit"
                            className="gradient-brand rounded-lg px-2.5 py-1 text-xs font-bold text-white shadow-sm cursor-pointer"
                          >
                            Use
                          </button>
                        </form>
                      </motion.div>
                    )}
                  </AnimatePresence>
                </div>

                {/* Generated Invite Code */}
                <div className="glass rounded-2xl p-4 border border-purple-500/20">
                  <SectionLabel>Auto-Generated Invite Code</SectionLabel>
                  <div className="mt-2 flex items-center justify-between gap-3">
                    <p className="font-mono text-2xl font-black tracking-[0.2em] text-purple-400">
                      {code}
                    </p>
                    <GhostButton onClick={() => setCode(randomCode())} className="text-xs">
                      Regenerate
                    </GhostButton>
                  </div>
                  <p className="mt-1 text-[11px] text-muted-foreground">
                    Members can use this code or your shareable link to join instantly.
                  </p>
                </div>

                <BrandButton className="mt-2 w-full py-2.5 text-xs font-bold" onClick={handleCreate}>
                  <Sparkles size={14} /> Create Group
                </BrandButton>
              </div>
            )}

            {step === 1 && (
              <div className="mt-6 text-center">
                <span className="gradient-brand glow-brand mx-auto flex h-14 w-14 items-center justify-center rounded-2xl shadow-lg">
                  <PartyPopper size={24} strokeWidth={1.75} className="text-primary-foreground" />
                </span>
                <h3 className="font-display mt-4 text-lg font-bold">
                  {selectedEmoji} {groupName || "Your group"} is live!
                </h3>
                <p className="mt-1 text-xs text-muted-foreground">
                  Share the link with your group members. Balances reconcile as members join.
                </p>

                {/* Shareable Link Box */}
                <div className="glass mt-5 flex items-center gap-2 rounded-full p-1.5 pl-4 border border-purple-500/30 shadow-sm">
                  <span className="flex-1 truncate text-left font-mono text-xs text-foreground select-all">
                    {typeof window !== "undefined" ? window.location.origin : "http://localhost:3000"}/?join={code}
                  </span>
                  <BrandButton
                    className="px-3.5 py-1.5 text-xs font-bold shrink-0"
                    onClick={async () => {
                      const url = `${typeof window !== "undefined" ? window.location.origin : "http://localhost:3000"}/?join=${code}`;
                      try {
                        await navigator.clipboard.writeText(url);
                        setCopied(true);
                        toast.success("Shareable invite link copied to clipboard! 📋");
                        setTimeout(() => setCopied(false), 2000);
                      } catch {
                        toast.error("Clipboard unavailable");
                      }
                    }}
                  >
                    {copied ? <Check size={14} strokeWidth={2} /> : <Copy size={14} strokeWidth={1.75} />}
                    {copied ? "Copied" : "Copy Link"}
                  </BrandButton>
                </div>

                <BrandButton className="mt-5 w-full font-bold" onClick={close}>
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
