import { motion, AnimatePresence } from "framer-motion";
import { X, Lock, Mail, User, Wallet, ArrowRight, CheckCircle2 } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";
import { BrandButton, GhostButton, SectionLabel, BrandLogo } from "./primitives";

export function AuthModal({
  open,
  onClose,
  onSuccess,
}: {
  open: boolean;
  onClose: () => void;
  onSuccess: (user: { name: string; email: string }) => void;
}) {
  const [isLogin, setIsLogin] = useState(true);
  const [email, setEmail] = useState("anvi@example.com");
  const [password, setPassword] = useState("Anvi@2026");
  const [name, setName] = useState("Anvi Verma");
  const [vpa, setVpa] = useState("anvi@upi");
  const [loading, setLoading] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      const displayName = isLogin ? (email.toLowerCase().includes("anvi") ? "Anvi Verma" : "Sarah Menon") : name;
      onSuccess({ name: displayName, email });
      toast.success(isLogin ? `Welcome back, ${displayName.split(" ")[0]}!` : "Account created successfully 🎉");
      onClose();
    }, 600);
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
          className="glass-strong relative z-10 w-full max-w-md overflow-hidden rounded-3xl border border-purple-500/30 p-6 shadow-[0_0_50px_rgba(168,85,247,0.25)]"
        >
          {/* Header */}
          <div className="flex items-center justify-between">
            <BrandLogo size="md" />
            <button
              type="button"
              onClick={onClose}
              className="rounded-full border border-border p-2 text-muted-foreground cursor-pointer transition-all hover:border-purple-400 hover:text-purple-300 hover:scale-110"
            >
              <X size={16} />
            </button>
          </div>

          {/* Toggle Tabs */}
          <div className="mt-6 flex rounded-full border border-purple-500/40 bg-card/90 p-1.5 shadow-[0_0_20px_rgba(168,85,247,0.15)]">
            <button
              type="button"
              onClick={() => setIsLogin(true)}
              className={`flex-1 rounded-full py-2.5 text-xs font-extrabold cursor-pointer transition-all duration-150 ${
                isLogin
                  ? "bg-gradient-to-r from-purple-600 via-indigo-600 to-cyan-500 text-white font-black border border-purple-300 shadow-[0_0_20px_rgba(168,85,247,0.65)] scale-102"
                  : "text-muted-foreground hover:text-purple-600 dark:hover:text-cyan-300 hover:bg-purple-500/10"
              }`}
            >
              Log In
            </button>
            <button
              type="button"
              onClick={() => setIsLogin(false)}
              className={`flex-1 rounded-full py-2.5 text-xs font-extrabold cursor-pointer transition-all duration-150 ${
                !isLogin
                  ? "bg-gradient-to-r from-purple-600 via-indigo-600 to-cyan-500 text-white font-black border border-purple-300 shadow-[0_0_20px_rgba(168,85,247,0.65)] scale-102"
                  : "text-muted-foreground hover:text-purple-600 dark:hover:text-cyan-300 hover:bg-purple-500/10"
              }`}
            >
              Sign Up
            </button>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="mt-6 space-y-4">
            {!isLogin && (
              <>
                <div>
                  <SectionLabel>Full Name</SectionLabel>
                  <div className="relative mt-1">
                    <User size={15} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
                    <input
                      type="text"
                      required
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      placeholder="Sarah Menon"
                      className="w-full rounded-2xl border border-input bg-background/60 py-2.5 pl-10 pr-4 text-sm font-medium outline-none transition-all focus:border-purple-400 focus:ring-2 focus:ring-purple-400/30"
                    />
                  </div>
                </div>

                <div>
                  <SectionLabel>UPI VPA (Optional)</SectionLabel>
                  <div className="relative mt-1">
                    <Wallet size={15} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
                    <input
                      type="text"
                      value={vpa}
                      onChange={(e) => setVpa(e.target.value)}
                      placeholder="sarah@upi"
                      className="w-full rounded-2xl border border-input bg-background/60 py-2.5 pl-10 pr-4 text-sm font-medium outline-none transition-all focus:border-purple-400 focus:ring-2 focus:ring-purple-400/30"
                    />
                  </div>
                </div>
              </>
            )}

            <div>
              <SectionLabel>Email Address</SectionLabel>
              <div className="relative mt-1">
                <Mail size={15} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="sarah@example.com"
                  className="w-full rounded-2xl border border-input bg-background/60 py-2.5 pl-10 pr-4 text-sm font-medium outline-none transition-all focus:border-purple-400 focus:ring-2 focus:ring-purple-400/30"
                />
              </div>
            </div>

            <div>
              <SectionLabel>Password</SectionLabel>
              <div className="relative mt-1">
                <Lock size={15} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full rounded-2xl border border-input bg-background/60 py-2.5 pl-10 pr-4 text-sm font-medium outline-none transition-all focus:border-purple-400 focus:ring-2 focus:ring-purple-400/30"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="mt-6 flex w-full items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-purple-600 via-indigo-600 to-cyan-500 py-3 text-sm font-black text-white shadow-[0_0_25px_rgba(168,85,247,0.4)] cursor-pointer transition-all duration-150 hover:scale-102 hover:shadow-[0_0_30px_rgba(56,189,248,0.6)] active:scale-98"
            >
              {loading ? (
                "Processing..."
              ) : (
                <>
                  {isLogin ? "Sign In to SplitSmart" : "Create Free Account"} <ArrowRight size={16} />
                </>
              )}
            </button>
          </form>

          <p className="mt-4 text-center text-xs text-muted-foreground">
            {isLogin ? "Don't have an account?" : "Already have an account?"}{" "}
            <button
              type="button"
              onClick={() => setIsLogin(!isLogin)}
              className="font-bold text-purple-500 dark:text-cyan-400 hover:underline cursor-pointer"
            >
              {isLogin ? "Sign Up" : "Log In"}
            </button>
          </p>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}
