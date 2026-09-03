import { cn } from "@/lib/utils";
import type { ReactNode } from "react";

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
    <div className={cn("glass rounded-2xl", hover && "card-hover", className)}>{children}</div>
  );
}

export function SectionLabel({ children, className }: { children: ReactNode; className?: string }) {
  return <p className={cn("label-caps", className)}>{children}</p>;
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
        "inline-flex shrink-0 items-center justify-center rounded-full border border-border bg-secondary font-semibold tracking-wide text-secondary-foreground",
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
    brand: "border-primary/30 bg-primary/15 text-primary",
    positive: "border-positive/30 bg-positive/12 text-positive",
    negative: "border-negative/30 bg-negative/12 text-negative",
  };
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] font-semibold",
        tones[tone],
        className,
      )}
    >
      {children}
    </span>
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
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      aria-label={ariaLabel}
      className={cn(
        "gradient-brand inline-flex items-center justify-center gap-2 rounded-full px-4 py-2.5 text-sm font-semibold text-primary-foreground transition-all duration-200 hover:brightness-110 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50",
        "shadow-[0_14px_34px_-16px_var(--primary)]",
        className,
      )}
    >
      {children}
    </button>
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
    muted: "border-border bg-secondary/50 text-foreground hover:border-primary/50",
    positive: "border-positive/30 bg-positive/10 text-positive hover:border-positive/60",
    negative: "border-negative/30 bg-negative/10 text-negative hover:border-negative/60",
  };
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={ariaLabel}
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-full border px-3.5 py-2 text-sm font-semibold transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring active:scale-[0.98]",
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
    value > 0 ? "text-positive" : value < 0 ? "text-negative" : "text-muted-foreground";
  return (
    <span className={cn("font-display font-bold tabular-nums", tone, className)}>
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
      className={cn("h-10 w-full", className)}
      role="img"
      aria-label="Balance trend over the last 12 reconciliations"
    >
      <defs>
        <linearGradient id="spark-fill" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="var(--positive)" stopOpacity="0.45" />
          <stop offset="100%" stopColor="var(--positive)" stopOpacity="0" />
        </linearGradient>
      </defs>
      <path d={`${d} L100,36 L0,36 Z`} fill="url(#spark-fill)" />
      <path
        d={d}
        fill="none"
        stroke="var(--positive)"
        strokeWidth="1.75"
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
      <span className="text-primary font-black mx-[0.5px]">
        Smart
      </span>
    </span>
  );
}

