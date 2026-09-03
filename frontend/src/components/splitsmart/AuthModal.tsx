import { AnimatePresence, motion } from "framer-motion";
import { ArrowRight, Lock, Mail, User, Wallet, X, Scale, Sparkles } from "lucide-react";
import { useState, useEffect } from "react";
import { toast } from "sonner";
import { SectionLabel } from "./primitives";

export function AuthModal({
  open,
  onClose,
  onSuccess,
  defaultToSignUp = false,
  inviteGroup = null,
}: {
  open: boolean;
  onClose: () => void;
  onSuccess: (user: { name: string; email: string }) => void;
  defaultToSignUp?: boolean;
  inviteGroup?: { name: string; emoji?: string; code: string } | null;
}) {
  const [isLogin, setIsLogin] = useState(!defaultToSignUp && !inviteGroup);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [vpa, setVpa] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (inviteGroup || defaultToSignUp) {
      setIsLogin(false);
    }
  }, [inviteGroup, defaultToSignUp]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      const displayName = isLogin
        ? name.trim() || email.split("@")[0] || "User"
        : name.trim() || "User";
      onSuccess({ name: displayName, email: email.trim() });
      toast.success(
        isLogin
          ? `Welcome back, ${displayName.split(" ")[0]}!`
          : `Account created! Welcome, ${displayName.split(" ")[0]} 🎉`
      );
      onClose();
    }, 400);
  };

  if (!open) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
          className="fixed inset-0 bg-background/80 backdrop-blur-md"
        />

        <motion.div
          role="dialog"
          aria-label="Authentication modal"
          initial={{ opacity: 0, scale: 0.95, y: 15 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 10 }}
          transition={{ type: "spring", stiffness: 360, damping: 30 }}
          className="glass-strong relative z-10 w-full max-w-md overflow-hidden rounded-3xl border border-primary/30 p-6 shadow-[0_0_50px_rgba(0,0,0,0.5)]"
        >
          {/* Header */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="gradient-brand glow-brand flex h-9 w-9 items-center justify-center rounded-xl">
                <Scale size={18} strokeWidth={2} className="text-primary-foreground" />
              </div>
              <div>
                <h3 className="font-display text-base font-extrabold tracking-tight">SplitSmart</h3>
                <p className="text-[11px] text-muted-foreground">
                  {inviteGroup ? `Invite to ${inviteGroup.name}` : "Sign in to manage your ledgers"}
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="rounded-full border border-border p-2 text-muted-foreground transition-all hover:border-primary/50 hover:text-foreground hover:scale-105 cursor-pointer"
            >
              <X size={15} />
            </button>
          </div>

          {/* Group Invitation Banner */}
          {inviteGroup && (
            <div className="mt-4 rounded-2xl border border-purple-500/30 bg-purple-500/10 p-3.5 flex items-center gap-3">
              <span className="text-2xl">{inviteGroup.emoji || "🏝️"}</span>
              <div className="min-w-0 flex-1 text-left">
                <p className="text-xs font-bold text-foreground">
                  Join <span className="text-purple-400">{inviteGroup.name}</span>
                </p>
                <p className="text-[11px] text-muted-foreground">
                  Sign up or log in to instantly join the group and view balances.
                </p>
              </div>
            </div>
          )}

          {/* Toggle Tabs */}
          <div className="mt-5 flex rounded-full border border-border bg-secondary/50 p-1">
            <button
              type="button"
              onClick={() => setIsLogin(false)}
              className={`flex-1 rounded-full py-2 text-xs font-bold transition-all cursor-pointer ${
                !isLogin
                  ? "gradient-brand text-primary-foreground shadow-sm font-extrabold"
                  : "text-muted-foreground hover:text-foreground"
              }`}
            >
              Create Account
            </button>
            <button
              type="button"
              onClick={() => setIsLogin(true)}
              className={`flex-1 rounded-full py-2 text-xs font-bold transition-all cursor-pointer ${
                isLogin
                  ? "gradient-brand text-primary-foreground shadow-sm font-extrabold"
                  : "text-muted-foreground hover:text-foreground"
              }`}
            >
              Sign In
            </button>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="mt-5 space-y-3.5">
            {!isLogin && (
              <>
                <div>
                  <SectionLabel>Full Name</SectionLabel>
                  <div className="relative mt-1">
                    <User size={14} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
                    <input
                      type="text"
                      required
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      placeholder="Sarah Menon"
                      className="w-full rounded-xl border border-input bg-background/60 py-2 pl-9 pr-3 text-xs font-medium outline-none transition-all focus:border-primary focus:ring-1 focus:ring-primary"
                    />
                  </div>
                </div>

                <div>
                  <SectionLabel>UPI VPA (for settlements)</SectionLabel>
                  <div className="relative mt-1">
                    <Wallet size={14} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
                    <input
                      type="text"
                      required
                      value={vpa}
                      onChange={(e) => setVpa(e.target.value)}
                      placeholder="sarah@upi"
                      className="w-full rounded-xl border border-input bg-background/60 py-2 pl-9 pr-3 text-xs font-medium font-mono outline-none transition-all focus:border-primary focus:ring-1 focus:ring-primary"
                    />
                  </div>
                </div>
              </>
            )}

            <div>
              <SectionLabel>Email Address</SectionLabel>
              <div className="relative mt-1">
                <Mail size={14} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
                  className="w-full rounded-xl border border-input bg-background/60 py-2 pl-9 pr-3 text-xs font-medium outline-none transition-all focus:border-primary focus:ring-1 focus:ring-primary"
                />
              </div>
            </div>

            <div>
              <SectionLabel>Password</SectionLabel>
              <div className="relative mt-1">
                <Lock size={14} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full rounded-xl border border-input bg-background/60 py-2 pl-9 pr-3 text-xs font-medium outline-none transition-all focus:border-primary focus:ring-1 focus:ring-primary"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="gradient-brand glow-brand mt-4 flex w-full items-center justify-center gap-2 rounded-xl py-2.5 text-xs font-extrabold text-primary-foreground transition-all hover:scale-[1.02] active:scale-95 disabled:opacity-50 cursor-pointer shadow-lg"
            >
              {loading ? (
                <span>Verifying...</span>
              ) : isLogin ? (
                <>
                  <span>Sign In</span>
                  <ArrowRight size={14} />
                </>
              ) : (
                <>
                  <span>Create Account & Continue</span>
                  <Sparkles size={14} />
                </>
              )}
            </button>
          </form>

          {/* Footer note */}
          <p className="mt-4 text-center text-[10px] text-muted-foreground">
            {isLogin
              ? "Don't have an account? Switch to 'Create Account' above."
              : "By creating an account, you agree to SplitSmart terms."}
          </p>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}
