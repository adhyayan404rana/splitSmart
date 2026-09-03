import { motion } from "framer-motion";
import {
  ChartPie,
  MessagesSquare,
  Scale,
  CreditCard,
  ScrollText,
  LogOut,
  LogIn,
  MessageCircle,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { Avatar } from "./primitives";

export type TabId = "landing" | "overview" | "ingest" | "consensus" | "settle" | "audit";

export const tabs: { id: TabId; label: string; short: string; icon: typeof ChartPie }[] = [
  { id: "overview", label: "Overview", short: "Overview", icon: ChartPie },
  { id: "ingest", label: "AI Ingress", short: "AI Ingress", icon: MessagesSquare },
  { id: "consensus", label: "Draft", short: "Draft", icon: Scale },
  { id: "settle", label: "Settlement", short: "Settlement", icon: CreditCard },
  { id: "audit", label: "Audit", short: "Audit", icon: ScrollText },
];

export function TopBar({
  tab,
  onTab,
  user,
  onSignOut,
  onSignIn,
  onToggleMessages,
  unreadCount = 0,
  isMessagesOpen = false,
}: {
  tab: TabId;
  onTab: (t: TabId) => void;
  user: { name: string; email: string } | null;
  onNewGroup?: () => void;
  onSignOut: () => void;
  onSignIn: () => void;
  onToggleMessages?: () => void;
  unreadCount?: number;
  isMessagesOpen?: boolean;
}) {
  const isLoggedIn = !!user && tab !== "landing";
  const userInitials = user?.name
    ? user.name
        .trim()
        .split(" ")
        .map((w) => w[0])
        .join("")
        .slice(0, 2)
        .toUpperCase() || "ME"
    : "ME";
  const displayName = user?.name ? user.name.trim().split(" ")[0] : "Account";

  return (
    <header className="sticky top-0 z-40 px-3 pt-3 pb-2 sm:px-6">
      <div className="mx-auto flex max-w-7xl items-center justify-between gap-3">
        {/* Clean Modern Brand Logo */}
        <button
          type="button"
          onClick={() => onTab(isLoggedIn ? "overview" : "landing")}
          className="flex items-center gap-2.5 text-left transition-transform hover:scale-105 cursor-pointer shrink-0"
        >
          <div className="gradient-brand glow-brand flex h-9 w-9 items-center justify-center rounded-xl shadow-[0_0_20px_rgba(168,85,247,0.5)]">
            <Scale size={18} strokeWidth={2} className="text-white" />
          </div>
          <span className="font-display text-lg font-black tracking-tight select-none">
            <span className="text-foreground">Split</span>
            <span className="bg-gradient-to-r from-purple-600 to-indigo-600 dark:from-purple-400 dark:to-indigo-300 bg-clip-text text-transparent">Smart</span>
          </span>
        </button>

        {/* Single-Line Compact Navigation Bar (Shown when user is authenticated) */}
        {isLoggedIn && (
          <nav
            aria-label="Primary"
            className="glass-strong hidden md:flex items-center gap-1 rounded-full p-1 shrink-0 border border-purple-500/25 shadow-lg"
          >
            {tabs.map((t) => {
              const Icon = t.icon;
              const isActive = tab === t.id;
              return (
                <button
                  key={t.id}
                  type="button"
                  onClick={() => onTab(t.id)}
                  aria-current={isActive ? "page" : undefined}
                  className={cn(
                    "relative flex shrink-0 items-center gap-1.5 rounded-full px-4 py-1.5 text-xs font-semibold whitespace-nowrap transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring cursor-pointer",
                    isActive ? "text-primary-foreground font-bold" : "text-muted-foreground hover:text-foreground",
                  )}
                >
                  {isActive && (
                    <motion.span
                      layoutId="tab-indicator"
                      transition={{ type: "spring", stiffness: 420, damping: 34 }}
                      className="gradient-brand absolute inset-0 -z-10 rounded-full shadow-[0_10px_28px_-14px_var(--primary)]"
                    />
                  )}
                  <Icon size={14} strokeWidth={1.75} />
                  <span>{t.label}</span>
                </button>
              );
            })}
          </nav>
        )}

        {/* Right Controls */}
        <div className="flex items-center gap-2 shrink-0">
          {!isLoggedIn ? (
            <button
              type="button"
              onClick={onSignIn}
              className="gradient-brand glow-brand flex items-center gap-1.5 rounded-full px-4 py-2 text-xs font-bold text-primary-foreground transition-all hover:scale-105 active:scale-95 cursor-pointer shadow-[0_0_20px_rgba(168,85,247,0.4)]"
            >
              <LogIn size={14} strokeWidth={2} />
              <span>Sign In</span>
            </button>
          ) : (
            <>
              {/* Minimal In-App Messaging Inbox Button */}
              <button
                type="button"
                onClick={onToggleMessages}
                title="Direct Messages & Inbox"
                aria-label="Open Direct Messages Inbox"
                className={cn(
                  "relative glass flex h-9 w-9 items-center justify-center rounded-full border transition-all duration-200 hover:scale-105 active:scale-95 cursor-pointer shadow-sm",
                  isMessagesOpen
                    ? "border-primary bg-primary/20 text-primary shadow-[0_0_15px_rgba(168,85,247,0.35)]"
                    : "border-purple-500/20 text-muted-foreground hover:text-foreground hover:border-purple-500/40"
                )}
              >
                <MessageCircle size={16} strokeWidth={2} />
                {unreadCount > 0 && (
                  <span className="absolute -top-1 -right-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[9px] font-black text-white shadow-[0_0_10px_rgba(239,68,68,0.7)] animate-pulse">
                    {unreadCount > 9 ? "9+" : unreadCount}
                  </span>
                )}
              </button>

              {/* Authenticated User Badge */}
              <div className="glass flex items-center gap-2 rounded-full px-3 py-1.5 text-xs font-semibold select-none border border-purple-500/20 shadow-sm">
                <Avatar initials={userInitials} size="sm" />
                <span className="hidden sm:inline font-bold text-foreground">
                  {displayName}
                </span>
              </div>

              {/* Dedicated Sign Out Button */}
              <button
                type="button"
                onClick={onSignOut}
                title="Sign Out"
                aria-label="Sign out of SplitSmart"
                className="glass flex items-center gap-1.5 rounded-full border border-red-500/30 bg-red-500/10 px-3 py-2 text-xs font-semibold text-red-400 transition-all hover:border-red-500/50 hover:bg-red-500/20 hover:text-red-300 hover:scale-[1.03] active:scale-95 cursor-pointer shadow-[0_0_12px_rgba(239,68,68,0.15)]"
              >
                <LogOut size={13} strokeWidth={2} />
                <span className="hidden md:inline">Sign Out</span>
              </button>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
