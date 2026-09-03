import { motion, AnimatePresence } from "framer-motion";
import {
  ArrowUpRight,
  ArrowDownRight,
  TrendingUp,
  X,
  Users,
  Layers,
  Share2,
  Plus,
  Sparkles,
  FolderPlus,
  KeyRound,
  ExternalLink,
  ChevronRight,
  Wallet,
  ShieldCheck,
  UserPlus,
} from "lucide-react";
import { useState, useEffect, useMemo } from "react";
import {
  formatMinor,
  minorRemainder,
  getStoredGroups,
  getStoredMembers,
  isUserInGroup,
  joinGroupInStore,
  fetchRemoteGroups,
  sparkline,
  type CurrencyCode,
  type Member,
  type Group,
} from "@/lib/splitsmart-data";
import {
  Amount,
  AvatarStack,
  Avatar,
  GlassCard,
  Pill,
  SectionLabel,
  BrandButton,
  GhostButton,
  Sparkline,
} from "./primitives";
import { ShareGroupModal } from "./ShareGroupModal";
import { GroupDetailModal } from "./GroupDetailModal";
import { JoinGroupModal } from "./JoinGroupModal";
import { toast } from "sonner";

export function OverviewScreen({
  currency,
  user,
  onNewGroup,
  onNavigateToIngest,
  onNavigateToSettlement,
}: {
  currency: CurrencyCode;
  user: { name: string; email: string } | null;
  onNewGroup?: () => void;
  onNavigateToIngest?: (groupName: string) => void;
  onNavigateToSettlement?: () => void;
}) {
  const [selectedGroup, setSelectedGroup] = useState<Group | null>(null);
  const [shareGroup, setShareGroup] = useState<Group | null>(null);
  const [joinModalOpen, setJoinModalOpen] = useState(false);
  const [allGroups, setAllGroups] = useState<Group[]>(() => getStoredGroups());
  const [allMembers, setAllMembers] = useState<Member[]>(() => getStoredMembers());

  // Reload groups on mount or when storage/data updates (cross-tab & multi-window sync)
  const refreshData = () => {
    setAllGroups(getStoredGroups());
    setAllMembers(getStoredMembers());
    fetchRemoteGroups().then((rem) => {
      if (rem && rem.length > 0) setAllGroups(rem);
    });
  };

  useEffect(() => {
    refreshData();

    const handleDataUpdate = () => {
      refreshData();
    };

    window.addEventListener("storage", handleDataUpdate);
    window.addEventListener("splitsmart_data_updated", handleDataUpdate);

    // Periodic sync poll for multi-window/incognito fast updates
    const interval = setInterval(refreshData, 1500);

    return () => {
      window.removeEventListener("storage", handleDataUpdate);
      window.removeEventListener("splitsmart_data_updated", handleDataUpdate);
      clearInterval(interval);
    };
  }, [user]);

  // Filter groups: show the groups the current user belongs to!
  const userGroups = useMemo(() => {
    if (!user) return allGroups;
    return allGroups.filter((g) => isUserInGroup(g, user));
  }, [allGroups, user]);

  // Relevant active members in user's groups
  const activeMembers = useMemo(() => {
    if (!user) return allMembers;
    if (userGroups.length === 0) return [];

    const memberMap = new Map<string, Member>();
    const getNormKey = (str: string) => {
      let clean = (str || "").toLowerCase().trim();
      if (clean.startsWith("m_")) clean = clean.slice(2);
      if (clean.includes("@")) clean = clean.split("@")[0];
      return clean.replace(/[^a-z0-9]/g, "");
    };

    userGroups.forEach((g) => {
      // Check allMembers
      allMembers.forEach((m) => {
        const isMember = g.memberIds.some((id) => {
          const idLower = id.toLowerCase().trim();
          return idLower === m.id.toLowerCase() || idLower === m.name.toLowerCase();
        });
        if (isMember || (m.group && m.group.toLowerCase() === g.name.toLowerCase())) {
          const key = getNormKey(m.name);
          if (key && !memberMap.has(key)) {
            memberMap.set(key, m);
          }
        }
      });

      // Check memberContributions
      if (g.memberContributions) {
        Object.keys(g.memberContributions).forEach((mKey) => {
          const key = getNormKey(mKey);
          if (key && !memberMap.has(key)) {
            let name = mKey.startsWith("m_") ? mKey.slice(2) : mKey;
            if (name.includes("@")) name = name.split("@")[0];
            name = name.charAt(0).toUpperCase() + name.slice(1);
            memberMap.set(key, {
              id: mKey,
              name,
              initials: name.slice(0, 2).toUpperCase(),
              vpa: `${name.toLowerCase()}@upi`,
              balance: 0,
              group: g.name,
              breakdown: [],
            });
          }
        });
      }
    });

    return Array.from(memberMap.values());
  }, [allMembers, userGroups, user]);

  // Current user's net portfolio balance & debts
  const userMemberId = user?.email ? `m_${user.email.replace(/[@.]/g, "_")}` : null;
  const currentMember = allMembers.find(
    (m) =>
      (userMemberId && m.id.toLowerCase() === userMemberId.toLowerCase()) ||
      (user?.email && m.id.toLowerCase() === user.email.toLowerCase()) ||
      (user && m.name.toLowerCase() === user.name.toLowerCase())
  );

  const getBack = activeMembers.filter((m) => m.balance > 0).reduce((a, m) => a + m.balance, 0);
  const owe = activeMembers.filter((m) => m.balance < 0).reduce((a, m) => a + m.balance, 0);
  const net =
    userGroups.length === 0
      ? 0
      : currentMember
      ? currentMember.balance
      : userGroups.reduce((a, g) => a + g.net, 0);

  const handleQuickJoin = (g: Group) => {
    if (!user) {
      toast.error("Please sign in first");
      return;
    }
    const joined = joinGroupInStore(g.inviteCode, user);
    if (joined) {
      toast.success(`Joined ${g.name}! 🎉`);
      refreshData();
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Net Balance & Analytics Grid */}
      <div className="grid gap-4 lg:grid-cols-3">
        <GlassCard className="relative overflow-hidden p-6 lg:col-span-2">
          <div
            aria-hidden
            className="gradient-brand pointer-events-none absolute -top-24 -right-16 h-64 w-64 rounded-full opacity-25 blur-3xl"
          />
          <SectionLabel>Net portfolio balance</SectionLabel>
          <div className="mt-3 flex flex-wrap items-end justify-between gap-4">
            <div>
              <p className="font-display text-4xl font-extrabold tracking-tight tabular-nums sm:text-5xl">
                {formatMinor(net, currency)}
              </p>
              <div className="mt-2 flex flex-wrap items-center gap-2">
                <Pill tone="positive">
                  <TrendingUp size={13} strokeWidth={1.75} /> +12.4% this cycle
                </Pill>
                <Pill>{minorRemainder(net, currency)} residual</Pill>
                <Pill tone="brand">
                  <Layers size={13} strokeWidth={1.75} /> {userGroups.length} active ledger{userGroups.length === 1 ? "" : "s"}
                </Pill>
              </div>
            </div>
            <div className="w-full max-w-[260px]">
              <Sparkline points={sparkline} />
              <p className="mt-1 text-right text-[11px] text-muted-foreground">
                last 12 ledger commits
              </p>
            </div>
          </div>
        </GlassCard>

        <div className="grid gap-4">
          <GlassCard className="glow-positive p-5">
            <SectionLabel>Total you get back</SectionLabel>
            <p className="font-display mt-2 text-2xl font-bold text-positive tabular-nums">
              {formatMinor(getBack, currency)}
            </p>
            <p className="mt-1 flex items-center gap-1.5 text-xs text-muted-foreground">
              <ArrowDownRight size={14} strokeWidth={1.75} className="text-positive" />
              across {userGroups.length} group{userGroups.length === 1 ? "" : "s"} · {activeMembers.length} member{activeMembers.length === 1 ? "" : "s"}
            </p>
          </GlassCard>
          <GlassCard className="glow-negative p-5">
            <SectionLabel>Total you owe</SectionLabel>
            <p className="font-display mt-2 text-2xl font-bold text-negative tabular-nums">
              {formatMinor(owe, currency)}
            </p>
            <p className="mt-1 flex items-center gap-1.5 text-xs text-muted-foreground">
              <ArrowUpRight size={14} strokeWidth={1.75} className="text-negative" />
              {activeMembers.filter((m) => m.balance < 0).length} open obligations
            </p>
          </GlassCard>
        </div>
      </div>

      {/* Groups Section */}
      <section aria-labelledby="groups-heading" className="space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 id="groups-heading" className="font-display text-xl font-bold tracking-tight">
              My Groups
            </h2>
            <p className="text-xs text-muted-foreground">
              Tap any group card to view member contributions, pledges, and budget goals
            </p>
          </div>

          {/* Action buttons */}
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setJoinModalOpen(true)}
              className="glass flex items-center gap-1.5 rounded-full border border-purple-500/30 bg-purple-500/10 px-3.5 py-1.5 text-xs font-bold text-purple-300 transition-all hover:bg-purple-500/20 hover:scale-105 active:scale-95 cursor-pointer shadow-sm"
            >
              <KeyRound size={13} strokeWidth={2} />
              <span>Join with Code</span>
            </button>

            {onNewGroup && (
              <button
                type="button"
                onClick={onNewGroup}
                className="gradient-brand glow-brand flex items-center gap-1.5 rounded-full px-3.5 py-1.5 text-xs font-bold text-white transition-all hover:scale-105 active:scale-95 cursor-pointer shadow-sm"
              >
                <Plus size={13} strokeWidth={2.5} />
                <span>New Group</span>
              </button>
            )}
          </div>
        </div>

        {/* Empty State when no groups */}
        {userGroups.length === 0 ? (
          <GlassCard className="p-8 text-center border border-dashed border-purple-500/30">
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-purple-500/10 text-purple-400 shadow-md">
              <FolderPlus size={28} />
            </div>
            <h3 className="font-display mt-3 text-lg font-bold text-foreground">No groups joined yet</h3>
            <p className="mt-1 text-xs text-muted-foreground max-w-md mx-auto">
              You are not a member of any group yet. Create your first group or enter an invite code to join a friend's group!
            </p>

            <div className="mt-5 flex flex-wrap items-center justify-center gap-3">
              {onNewGroup && (
                <button
                  type="button"
                  onClick={onNewGroup}
                  className="gradient-brand glow-brand inline-flex items-center gap-1.5 rounded-full px-4 py-2 text-xs font-bold text-white shadow-md hover:scale-105 transition-all cursor-pointer"
                >
                  <Plus size={14} /> Create a Group
                </button>
              )}
              <button
                type="button"
                onClick={() => setJoinModalOpen(true)}
                className="inline-flex items-center gap-1.5 rounded-full border border-purple-500/30 bg-purple-500/10 px-4 py-2 text-xs font-bold text-purple-300 hover:bg-purple-500/20 hover:scale-105 transition-all cursor-pointer"
              >
                <KeyRound size={14} /> Join with Invite Code
              </button>
            </div>
          </GlassCard>
        ) : (
          /* Clickable Group Cards Grid */
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {userGroups.map((g) => {
              const groupMembersMap = new Map<string, Member>();
              const getNormKey = (str: string) => {
                let clean = (str || "").toLowerCase().trim();
                if (clean.startsWith("m_")) clean = clean.slice(2);
                if (clean.includes("@")) clean = clean.split("@")[0];
                return clean.replace(/[^a-z0-9]/g, "");
              };

              allMembers.forEach((m) => {
                const isMember = g.memberIds.some((id) => {
                  const idLower = id.toLowerCase().trim();
                  return idLower === m.id.toLowerCase() || idLower === m.name.toLowerCase();
                });
                if (isMember || (m.group && m.group.toLowerCase() === g.name.toLowerCase())) {
                  const key = getNormKey(m.name);
                  if (key && !groupMembersMap.has(key)) {
                    groupMembersMap.set(key, m);
                  }
                }
              });

              if (g.memberContributions) {
                Object.keys(g.memberContributions).forEach((mKey) => {
                  const key = getNormKey(mKey);
                  if (key && !groupMembersMap.has(key)) {
                    let name = mKey.startsWith("m_") ? mKey.slice(2) : mKey;
                    if (name.includes("@")) name = name.split("@")[0];
                    name = name.charAt(0).toUpperCase() + name.slice(1);
                    groupMembersMap.set(key, {
                      id: mKey,
                      name,
                      initials: name.slice(0, 2).toUpperCase(),
                      vpa: `${name.toLowerCase()}@upi`,
                      balance: 0,
                      group: g.name,
                      breakdown: [],
                    });
                  }
                });
              }
              const groupMembers = Array.from(groupMembersMap.values());

              return (
                <div
                  key={g.id}
                  onClick={() => setSelectedGroup(g)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" || e.key === " ") {
                      setSelectedGroup(g);
                    }
                  }}
                  className="glass card-hover rounded-2xl p-5 flex flex-col justify-between cursor-pointer border border-border/60 hover:border-purple-500/60 hover:shadow-[0_0_25px_rgba(168,85,247,0.2)] transition-all group"
                >
                  <div>
                    {/* Header with Emoji and Status */}
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-center gap-3">
                        <span className="flex h-11 w-11 items-center justify-center rounded-xl border border-border bg-secondary/80 text-2xl shadow-sm group-hover:scale-110 transition-transform">
                          {g.emoji}
                        </span>
                        <div>
                          <p className="font-bold text-foreground group-hover:text-purple-300 transition-colors">
                            {g.name}
                          </p>
                          <p className="text-[11px] text-muted-foreground font-mono">
                            Code · <span className="font-bold text-purple-400">{g.inviteCode}</span>
                          </p>
                        </div>
                      </div>
                      {g.pendingDrafts > 0 ? (
                        <Pill tone="brand">{g.pendingDrafts} pending</Pill>
                      ) : (
                        <Pill tone="positive">settled</Pill>
                      )}
                    </div>

                    {/* Group Net & Members Avatars */}
                    <div className="mt-4 flex items-center justify-between border-t border-border/40 pt-3">
                      <div>
                        <SectionLabel>Group net position</SectionLabel>
                        <Amount
                          value={g.net}
                          formatted={formatMinor(g.net, currency)}
                          className="mt-0.5 block text-lg font-bold"
                        />
                      </div>
                      <AvatarStack
                        items={
                          groupMembers.length > 0
                            ? groupMembers.map((m) => m.initials)
                            : [user?.name ? user.name.slice(0, 2).toUpperCase() : "ME"]
                        }
                      />
                    </div>
                  </div>

                  {/* Footer Bar with Action */}
                  <div className="mt-4 pt-3 border-t border-border/40 flex items-center justify-between gap-2">
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        setShareGroup(g);
                      }}
                      className="flex items-center gap-1.5 rounded-full border border-purple-500/30 bg-purple-500/10 px-2.5 py-1 text-xs font-bold text-purple-400 transition-all hover:bg-purple-500/20 active:scale-95 cursor-pointer shadow-sm"
                    >
                      <Share2 size={12} strokeWidth={2} />
                      <span>Share</span>
                    </button>

                    <span className="text-xs font-semibold text-purple-400 group-hover:translate-x-1 transition-transform inline-flex items-center gap-0.5">
                      View Details & Members <ChevronRight size={14} />
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>

      {/* Interactive Group Detail Modal (Opened on Group Card Click) */}
      <GroupDetailModal
        group={
          selectedGroup
            ? allGroups.find(
                (g) =>
                  g.id === selectedGroup.id ||
                  (g.inviteCode &&
                    selectedGroup.inviteCode &&
                    g.inviteCode.toUpperCase() === selectedGroup.inviteCode.toUpperCase())
              ) || selectedGroup
            : null
        }
        open={!!selectedGroup}
        onClose={() => setSelectedGroup(null)}
        currency={currency}
        currentUser={user}
        onGroupUpdated={refreshData}
        onNavigateToIngest={onNavigateToIngest}
        onNavigateToSettlement={onNavigateToSettlement}
      />

      {/* Share Group Invite Modal */}
      <ShareGroupModal
        group={
          shareGroup
            ? allGroups.find(
                (g) =>
                  g.id === shareGroup.id ||
                  (g.inviteCode &&
                    shareGroup.inviteCode &&
                    g.inviteCode.toUpperCase() === shareGroup.inviteCode.toUpperCase())
              ) || shareGroup
            : null
        }
        open={!!shareGroup}
        onClose={() => setShareGroup(null)}
      />

      {/* Join Group with Code Modal */}
      <JoinGroupModal
        open={joinModalOpen}
        onClose={() => setJoinModalOpen(false)}
        currentUser={user}
        onGroupJoined={() => {
          refreshData();
        }}
      />
    </div>
  );
}
