import { cn } from "@/lib/utils";
import { motion } from "framer-motion";
import { useState, useRef, type ReactNode } from "react";
import confetti from "canvas-confetti";

export function triggerGoldenConfetti() {
  confetti({
    particleCount: 90,
    spread: 75,
    origin: { y: 0.6 },
    colors: ["#A855F7", "#C084FC", "#9333EA", "#FFFFFF"],
  });
}

export function GlassCard({
  children,
  className,
  hover = true,
}: {
  children: ReactNode;
  className?: string;
  hover?: boolean;
}) {
  return (
    <div
      className={cn(
        "glass rounded-2xl border border-border/60 dark:border-purple-500/30 bg-card/75 dark:bg-[#0B0718]/45 backdrop-blur-xl transition-all duration-300",
        hover && "cursor-pointer hover:scale-[1.02] hover:-translate-y-1 hover:border-purple-400 hover:shadow-[0_15px_35px_rgba(139,92,246,0.25)]",
        className,
      )}
    >
      {children}
    </div>
  );
}

export function SectionLabel({ children, className }: { children: ReactNode; className?: string }) {
  return <p className={cn("label-caps text-purple-600 dark:text-cyan-400 font-extrabold tracking-wider", className)}>{children}</p>;
}

export function Avatar({
  initials,
  size = "md",
  className,
}: {
  initials: string;
  size?: "sm" | "md" | "lg";
  className?: string;
}) {
  const sizes = {
    sm: "h-7 w-7 text-[10px]",
    md: "h-9 w-9 text-xs",
    lg: "h-12 w-12 text-sm",
  };
  return (
    <span
      aria-hidden
      className={cn(
        "inline-flex shrink-0 items-center justify-center rounded-full border border-purple-400/40 bg-secondary font-bold tracking-wide text-purple-300 transition-transform duration-200 hover:scale-110",
        sizes[size],
        className,
      )}
    >
      {initials}
    </span>
  );
}

export function AvatarStack({ items, size = "sm" }: { items: string[]; size?: "sm" | "md" }) {
  return (
    <div className="flex items-center">
      {items.map((i, idx) => (
        <Avatar
          key={i + idx}
          initials={i}
          size={size}
          className={cn("ring-2 ring-card", idx > 0 && "-ml-2")}
        />
      ))}
    </div>
  );
}

export function Pill({
  children,
  className,
  tone = "muted",
}: {
  children: ReactNode;
  className?: string;
  tone?: "muted" | "brand" | "positive" | "negative";
}) {
  const tones = {
    muted: "border-border bg-secondary/60 text-muted-foreground",
    brand: "border-purple-400/40 bg-purple-500/15 text-purple-300 font-bold",
    positive: "border-purple-400/40 bg-purple-500/15 text-purple-300 font-bold",
    negative: "border-white/20 bg-white/5 text-foreground font-medium",
  };
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] transition-transform duration-200 hover:scale-105",
        tones[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}

export function MagneticButton({
  children,
  onClick,
  className,
  type = "button",
  disabled,
}: {
  children: ReactNode;
  onClick?: () => void;
  className?: string;
  type?: "button" | "submit";
  disabled?: boolean;
}) {
  const [pos, setPos] = useState({ x: 0, y: 0 });

  const handleMouseMove = (e: React.MouseEvent<HTMLButtonElement>) => {
    const { clientX, clientY, currentTarget } = e;
    const { left, top, width, height } = currentTarget.getBoundingClientRect();
    const x = (clientX - (left + width / 2)) * 0.2;
    const y = (clientY - (top + height / 2)) * 0.2;
    setPos({ x, y });
  };

  const handleMouseLeave = () => setPos({ x: 0, y: 0 });

  return (
    <motion.button
      type={type}
      onClick={onClick}
      disabled={disabled}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      animate={{ x: pos.x, y: pos.y }}
      transition={{ type: "spring", stiffness: 320, damping: 22 }}
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-full bg-gradient-to-r from-emerald-400 via-teal-400 to-cyan-400 px-6 py-3 text-sm font-black text-slate-950 shadow-[0_0_25px_rgba(16,185,129,0.45)] transition-all hover:shadow-[0_0_30px_rgba(20,184,166,0.6)] hover:scale-105 active:scale-95 disabled:opacity-50",
        className,
      )}
    >
      {children}
    </motion.button>
  );
}

export function BrandButton({
  children,
  onClick,
  className,
  type = "button",
  disabled,
  "aria-label": ariaLabel,
}: {
  children: ReactNode;
  onClick?: () => void;
  className?: string;
  type?: "button" | "submit";
  disabled?: boolean;
  "aria-label"?: string;
}) {
  return (
    <MagneticButton type={type} onClick={onClick} disabled={disabled} className={className}>
      {children}
    </MagneticButton>
  );
}

export function GhostButton({
  children,
  onClick,
  className,
  tone = "muted",
  "aria-label": ariaLabel,
}: {
  children: ReactNode;
  onClick?: () => void;
  className?: string;
  tone?: "muted" | "positive" | "negative";
  "aria-label"?: string;
}) {
  const tones = {
    muted: "border-border bg-secondary/50 text-foreground hover:border-emerald-400 hover:text-emerald-300",
    positive: "border-emerald-500/40 bg-emerald-500/15 text-emerald-300 font-bold hover:border-emerald-400",
    negative: "border-border bg-secondary/70 text-foreground font-medium hover:border-emerald-400/50",
  };
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={ariaLabel}
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-full border px-3.5 py-2 text-sm transition-all duration-200 hover:scale-105 focus-visible:outline-none active:scale-[0.98]",
        tones[tone],
        className,
      )}
    >
      {children}
    </button>
  );
}

export function Amount({
  value,
  formatted,
  className,
}: {
  value: number;
  formatted: string;
  className?: string;
}) {
  const tone =
    value > 0 ? "text-emerald-300 font-black" : value < 0 ? "text-white font-bold" : "text-muted-foreground";
  return (
    <span className={cn("font-display tabular-nums transition-transform duration-200 hover:scale-105 inline-block", tone, className)}>
      {value > 0 ? "+" : ""}
      {formatted}
    </span>
  );
}

export function Sparkline({ points, className }: { points: number[]; className?: string }) {
  const min = Math.min(...points);
  const max = Math.max(...points);
  const span = max - min || 1;
  const d = points
    .map((p, i) => {
      const x = (i / (points.length - 1)) * 100;
      const y = 34 - ((p - min) / span) * 30;
      return `${i === 0 ? "M" : "L"}${x.toFixed(2)},${y.toFixed(2)}`;
    })
    .join(" ");
  return (
    <svg
      viewBox="0 0 100 36"
      preserveAspectRatio="none"
      className={cn("h-10 w-full transition-transform duration-300 hover:scale-105", className)}
      role="img"
      aria-label="Balance trend over the last 12 reconciliations"
    >
      <defs>
        <linearGradient id="spark-fill" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#A855F7" stopOpacity="0.45" />
          <stop offset="100%" stopColor="#A855F7" stopOpacity="0" />
        </linearGradient>
      </defs>
      <path d={`${d} L100,36 L0,36 Z`} fill="url(#spark-fill)" />
      <path
        d={d}
        fill="none"
        stroke="#A855F7"
        strokeWidth="2"
        strokeLinecap="round"
        strokeWidth="2"
        strokeLinecap="round"
        vectorEffect="non-scaling-stroke"
      />
    </svg>
  );
}

export function BrandLogo({ className, size = "md" }: { className?: string; size?: "sm" | "md" | "lg" | "xl" }) {
  const sizeClasses = {
    sm: "text-base",
    md: "text-xl",
    lg: "text-2xl sm:text-3xl",
    xl: "text-4xl sm:text-5xl",
  };

  return (
    <span className={cn("inline-flex items-center font-display font-black tracking-tight select-none", sizeClasses[size], className)}>
      <span className="text-foreground">Split</span>
      <span className="text-emerald-400 font-black drop-shadow-[0_0_14px_rgba(52,211,153,0.9)] mx-[0.5px]">
        ₹
      </span>
      <span className="text-foreground">mart</span>
    </span>
  );
}
