import { motion } from "framer-motion";
import {
  ChartPie,
  MessagesSquare,
  Scale,
  CreditCard,
  ScrollText,
  ChevronDown,
  Plus,
  Check,
  Sun,
  Moon,
  Home,
  LogIn,
  LogOut,
  Sparkles,
} from "lucide-react";
import { useState } from "react";
import { cn } from "@/lib/utils";
import { Avatar, BrandLogo } from "./primitives";
import { currencies, personas, type CurrencyCode, type PersonaId } from "@/lib/splitsmart-data";

export type TabId = "landing" | "overview" | "ingest" | "consensus" | "settle" | "audit";

export const tabs: { id: TabId; label: string; short: string; icon: typeof ChartPie }[] = [
  { id: "landing", label: "Home", short: "Home", icon: Home },
  { id: "overview", label: "Overview & Groups", short: "Overview", icon: ChartPie },
  { id: "ingest", label: "AI Ingress", short: "AI", icon: MessagesSquare },
  { id: "consensus", label: "Draft Consensus", short: "Drafts", icon: Scale },
  { id: "settle", label: "Settlement & UPI", short: "Settle", icon: CreditCard },
  { id: "audit", label: "Audit Feed", short: "Audit", icon: ScrollText },
];

export function TopBar({
  tab,
  onTab,
  persona,
  onPersona,
  currency,
  onCurrency,
  onNewGroup,
  isDark,
  onToggleTheme,
  onOpenAuth,
  user,
  onSignOut,
}: {
  tab: TabId;
  onTab: (t: TabId) => void;
  persona: PersonaId;
  onPersona: (p: PersonaId) => void;
  currency: CurrencyCode;
  onCurrency: (c: CurrencyCode) => void;
  onNewGroup: () => void;
  isDark: boolean;
  onToggleTheme: () => void;
  onOpenAuth: () => void;
  user: { name: string; email: string } | null;
  onSignOut: () => void;
}) {
  const [personaOpen, setPersonaOpen] = useState(false);
  const [currencyOpen, setCurrencyOpen] = useState(false);
  const active = personas.find((p) => p.id === persona) || personas[0];

  const visibleTabs = user ? tabs.filter((t) => t.id !== "landing") : [];

  return (
    <header className="sticky top-0 z-40 px-3 pt-3 pb-2 sm:px-6 no-scrollbar overflow-hidden">
      <div className="mx-auto flex max-w-7xl items-center justify-between gap-2 no-scrollbar overflow-hidden">
        {/* Brand Logo */}
        <button
          type="button"
          onClick={() => onTab(user ? "overview" : "landing")}
          className="flex items-center gap-2 transition-transform hover:scale-105"
        >
          <BrandLogo size="md" />
        </button>

        {/* Compact Navigation Bar (Only rendered when user is authenticated) */}
        {user && visibleTabs.length > 0 && (
          <nav
            aria-label="Primary"
            className="flex items-center gap-1.5 rounded-full p-1.5 border border-emerald-500/40 bg-[#061017]/95 shadow-[0_0_30px_rgba(16,185,129,0.2)] backdrop-blur-2xl max-w-full no-scrollbar overflow-hidden"
          >
            {visibleTabs.map((t) => {
              const Icon = t.icon;
              const isActive = tab === t.id;
              return (
                <button
                  key={t.id}
                  type="button"
                  onClick={() => onTab(t.id)}
                  aria-current={isActive ? "page" : undefined}
                  className={cn(
                    "relative flex shrink-0 items-center gap-2 rounded-full px-4 py-2 text-xs font-bold transition-all duration-200 focus-visible:outline-none",
                    isActive
                      ? "text-slate-950 font-black shadow-[0_0_20px_rgba(52,211,153,0.75)] scale-105"
                      : "text-slate-300 hover:text-emerald-300 hover:bg-emerald-500/10 hover:border-emerald-500/30 hover:shadow-[0_0_15px_rgba(16,185,129,0.2)] hover:scale-102 border border-transparent",
                  )}
                >
                  {isActive && (
                    <motion.span
                      layoutId="tab-indicator"
                      transition={{ type: "spring", stiffness: 450, damping: 32 }}
                      className="absolute inset-0 -z-10 rounded-full bg-emerald-400 border border-emerald-300 shadow-[0_0_25px_rgba(52,211,153,0.8)]"
                    />
                  )}
                  {isActive ? (
                    <span className="h-2 w-2 rounded-full bg-slate-950 animate-pulse" />
                  ) : (
                    <Icon size={14} strokeWidth={2} />
                  )}
                  <span className="hidden md:inline font-extrabold">{t.label}</span>
                  <span className="md:hidden font-extrabold">{t.short}</span>
                </button>
              );
            })}
          </nav>
        )}

        {/* Right Controls */}
        <div className="flex items-center gap-2">

          {user ? (
            <>
              {/* Persona Switcher */}
              <div className="relative hidden xl:block">
                <button
                  type="button"
                  onClick={() => {
                    setPersonaOpen((v) => !v);
                    setCurrencyOpen(false);
                  }}
                  className="glass flex items-center gap-2 rounded-full px-2.5 py-1.5 text-xs font-semibold cursor-pointer transition-all duration-150 hover:border-purple-400 hover:scale-105"
                >
                  <Avatar initials={active.initials} size="sm" />
                  <span>{active.name.split(" ")[0]}</span>
                  <ChevronDown size={13} className="text-muted-foreground" />
                </button>
                {personaOpen && (
                  <ul className="glass-strong absolute right-0 z-50 mt-2 w-56 rounded-2xl p-1.5 border border-purple-500/30">
                    {personas.map((p) => (
                      <li key={p.id}>
                        <button
                          type="button"
                          onClick={() => {
                            onPersona(p.id);
                            setPersonaOpen(false);
                          }}
                          className="flex w-full items-center gap-2 rounded-xl px-2.5 py-2 text-left text-xs cursor-pointer hover:bg-purple-600/20"
                        >
                          <Avatar initials={p.initials} size="sm" />
                          <span className="min-w-0 flex-1 truncate font-semibold">{p.name}</span>
                          {p.id === persona && <Check size={14} className="text-purple-400" />}
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>

              {/* User Profile Badge & Sign Out */}
              <div className="flex items-center gap-1.5">
                <div className="glass flex items-center gap-2 rounded-full px-3 py-1.5 text-xs font-bold text-white border border-purple-500/30">
                  <div className="flex h-5 w-5 items-center justify-center rounded-full bg-purple-600 text-white text-[10px] font-extrabold">
                    {user.name.charAt(0).toUpperCase()}
                  </div>
                  <span className="hidden sm:inline max-w-[100px] truncate">{user.name}</span>
                </div>

                <button
                  type="button"
                  onClick={onSignOut}
                  title="Sign Out"
                  className="flex items-center gap-1.5 rounded-full border border-red-500/30 bg-red-500/10 px-3 py-1.5 text-xs font-extrabold text-red-400 cursor-pointer transition-all duration-150 hover:scale-105 hover:bg-red-500 hover:text-white"
                >
                  <LogOut size={13} />
                  <span className="hidden sm:inline">Sign Out</span>
                </button>
              </div>
            </>
          ) : (
            /* Log In CTA */
            <button
              type="button"
              onClick={onOpenAuth}
              className="flex items-center gap-1.5 rounded-full border border-purple-400/40 bg-purple-500/15 px-4 py-2 text-xs font-black text-purple-300 dark:text-cyan-300 cursor-pointer transition-all duration-150 hover:scale-108 hover:bg-gradient-to-r hover:from-purple-600 hover:via-indigo-600 hover:to-cyan-500 hover:text-white shadow-[0_0_20px_rgba(168,85,247,0.4)] active:scale-95"
            >
              <LogIn size={14} />
              <span>Sign In</span>
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
