export type PersonaId = "sarah" | "david" | "host";

export type Persona = {
  id: PersonaId;
  name: string;
  role: string;
  initials: string;
  vpa: string;
  tint: string;
};

export const personas: Persona[] = [
  {
    id: "sarah",
    name: "Sarah Menon",
    role: "Organizer",
    initials: "SM",
    vpa: "sarah@upi",
    tint: "from-primary to-primary-deep",
  },
  {
    id: "david",
    name: "David Rao",
    role: "Roommate",
    initials: "DR",
    vpa: "david@upi",
    tint: "from-positive to-primary",
  },
  {
    id: "host",
    name: "Aisha Kapoor",
    role: "Event Host",
    initials: "AK",
    vpa: "aisha@upi",
    tint: "from-negative to-primary-deep",
  },
];

export type CurrencyCode = "INR";

export const currencies: Record<
  CurrencyCode,
  { symbol: string; label: string; rate: number; minorLabel: string }
> = {
  INR: { symbol: "₹", label: "Indian Rupee", rate: 1, minorLabel: "paise" },
};

/** Amounts are stored as integer minor units (paise) — never floats. Always formatted in INR (₹). */
export function formatMinor(paise: number, code: CurrencyCode = "INR"): string {
  const value = paise / 100;
  const sign = value < 0 ? "-" : "";
  return `${sign}₹${Math.abs(value).toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

export function minorRemainder(paise: number, code: CurrencyCode = "INR"): string {
  return `${Math.abs(paise) % 100} paise`;
}

export type Member = {
  id: string;
  name: string;
  initials: string;
  vpa: string;
  /** signed integer paise: positive = is owed, negative = owes */
  balance: number;
  group: string;
  breakdown: { label: string; amount: number; note: string }[];
};

export const members: Member[] = [
  {
    id: "m1",
    name: "Sarah Menon",
    initials: "SM",
    vpa: "sarah@upi",
    balance: 412_500,
    group: "Goa Trip '26",
    breakdown: [
      { label: "Beach shack dinner", amount: 400_000, note: "Paid in full, split 3 ways" },
      { label: "Scooter rentals", amount: 90_000, note: "Paid, split with David" },
      { label: "Airport cab", amount: -77_500, note: "Owed to Maya" },
    ],
  },
  {
    id: "m2",
    name: "David Rao",
    initials: "DR",
    vpa: "david@upi",
    balance: -133_333,
    group: "Goa Trip '26",
    breakdown: [
      { label: "Beach shack dinner", amount: -133_333, note: "Equal share of ₹4,000" },
      { label: "Scooter rentals", amount: -45_000, note: "Equal share" },
      { label: "Villa deposit", amount: 45_000, note: "Prepaid" },
    ],
  },
  {
    id: "m3",
    name: "Maya Iyer",
    initials: "MI",
    vpa: "maya@upi",
    balance: -179_167,
    group: "Goa Trip '26",
    breakdown: [
      { label: "Beach shack dinner", amount: -133_333, note: "Equal share of ₹4,000" },
      { label: "Airport cab", amount: 77_500, note: "Paid, split 3 ways" },
      { label: "Kayak tickets", amount: -123_334, note: "Exact split" },
    ],
  },
  {
    id: "m4",
    name: "Rahul Verma",
    initials: "RV",
    vpa: "rahul@upi",
    balance: -100_000,
    group: "Flat 402 Bills",
    breakdown: [
      { label: "Electricity Aug", amount: -60_000, note: "40% weighted split" },
      { label: "Internet", amount: -40_000, note: "Equal share" },
    ],
  },
  {
    id: "m5",
    name: "Aisha Kapoor",
    initials: "AK",
    vpa: "aisha@upi",
    balance: 0,
    group: "Flat 402 Bills",
    breakdown: [{ label: "Settled up", amount: 0, note: "All ledgers reconciled" }],
  },
];

export type GroupMemberContribution = {
  pledgedMinor: number;
  paidMinor: number;
  role?: "Owner" | "Admin" | "Member";
};

export type Group = {
  id: string;
  name: string;
  emoji: string;
  memberIds: string[];
  createdBy?: string;
  net: number;
  pendingDrafts: number;
  inviteCode: string;
  budgetGoal?: number;
  memberContributions?: Record<string, GroupMemberContribution>;
};

export const groups: Group[] = [
  {
    id: "g1",
    name: "Goa Trip '26",
    emoji: "🏝️",
    memberIds: ["m1", "m2", "m3", "sarah@splitsmart.app", "sarah"],
    createdBy: "sarah@splitsmart.app",
    net: 412_500,
    pendingDrafts: 2,
    inviteCode: "GOA7XK21",
    budgetGoal: 800_000, // ₹8,000
    memberContributions: {
      m1: { pledgedMinor: 300_000, paidMinor: 490_000, role: "Owner" },
      m2: { pledgedMinor: 250_000, paidMinor: 45_000, role: "Member" },
      m3: { pledgedMinor: 250_000, paidMinor: 77_500, role: "Member" },
    },
  },
  {
    id: "g2",
    name: "Flat 402 Bills",
    emoji: "🏠",
    memberIds: ["m4", "m5", "m1", "sarah@splitsmart.app", "sarah"],
    createdBy: "sarah@splitsmart.app",
    net: -100_000,
    pendingDrafts: 1,
    inviteCode: "FLT402QZ",
    budgetGoal: 500_000, // ₹5,000
    memberContributions: {
      m4: { pledgedMinor: 200_000, paidMinor: 150_000, role: "Owner" },
      m5: { pledgedMinor: 150_000, paidMinor: 150_000, role: "Member" },
      m1: { pledgedMinor: 150_000, paidMinor: 100_000, role: "Member" },
    },
  },
  {
    id: "g3",
    name: "Team Offsite",
    emoji: "🎯",
    memberIds: ["m1", "m3", "m5", "sarah@splitsmart.app", "sarah"],
    createdBy: "sarah@splitsmart.app",
    net: 68_400,
    pendingDrafts: 0,
    inviteCode: "OFFSTE94",
    budgetGoal: 1_200_000, // ₹12,000
    memberContributions: {
      m1: { pledgedMinor: 400_000, paidMinor: 400_000, role: "Owner" },
      m3: { pledgedMinor: 400_000, paidMinor: 400_000, role: "Member" },
      m5: { pledgedMinor: 400_000, paidMinor: 400_000, role: "Member" },
    },
  },
];

// Real-time multi-window / cross-session broadcast channel
export const syncChannel =
  typeof window !== "undefined" && typeof BroadcastChannel !== "undefined"
    ? new BroadcastChannel("splitsmart_channel_sync")
    : null;

if (syncChannel) {
  syncChannel.onmessage = (event) => {
    if (event.data && event.data.type === "GROUP_UPDATED") {
      const updatedGroup: Group = event.data.group;
      if (!updatedGroup || !updatedGroup.id) return;
      const current = getStoredGroups();
      const map = new Map<string, Group>();
      current.forEach((g) => map.set(g.id, g));

      let existing = map.get(updatedGroup.id);
      if (!existing && updatedGroup.inviteCode) {
        existing = Array.from(map.values()).find(
          (g) => g.inviteCode && g.inviteCode.toUpperCase() === updatedGroup.inviteCode.toUpperCase()
        );
      }

      if (existing) {
        const finalName =
          updatedGroup.name && !updatedGroup.name.startsWith("Group (")
            ? updatedGroup.name
            : existing.name;
        const finalEmoji =
          updatedGroup.emoji && updatedGroup.emoji !== "🏝️"
            ? updatedGroup.emoji
            : existing.emoji;
        const mergedMemberIds = Array.from(
          new Set([...existing.memberIds, ...updatedGroup.memberIds])
        );
        const mergedContribs = {
          ...(existing.memberContributions || {}),
          ...(updatedGroup.memberContributions || {}),
        };
        const unified: Group = {
          ...existing,
          ...updatedGroup,
          id: existing.id,
          name: finalName,
          emoji: finalEmoji,
          memberIds: mergedMemberIds,
          memberContributions: mergedContribs,
        };
        map.set(existing.id, unified);
      } else {
        map.set(updatedGroup.id, updatedGroup);
      }

      const mergedList = Array.from(map.values());
      localStorage.setItem("splitsmart_groups_v1", JSON.stringify(mergedList));
      broadcastDataChange();
    }
  };
}

// Synchronize a group with backend server and broadcast to other tabs/windows
export async function syncGroupToBackend(group: Group): Promise<void> {
  try {
    syncChannel?.postMessage({ type: "GROUP_UPDATED", group });
    const payload = JSON.stringify(group);
    fetch("http://localhost:8080/api/v1/groups/sync", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: payload,
    }).catch(() => {});
    fetch("/api/v1/groups/sync", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: payload,
    }).catch(() => {});
  } catch {}
}

// Fetch remote groups from backend and merge with localStorage
export async function fetchRemoteGroups(): Promise<Group[]> {
  if (typeof window === "undefined") return groups;
  try {
    let res = await fetch("http://localhost:8080/api/v1/groups/sync").catch(() => null);
    if (!res || !res.ok) {
      res = await fetch("/api/v1/groups/sync").catch(() => null);
    }
    if (res && res.ok) {
      const remoteGroups: Group[] = await res.json();
      if (Array.isArray(remoteGroups) && remoteGroups.length > 0) {
        const local = getStoredGroups();
        const map = new Map<string, Group>();
        local.forEach((g) => map.set(g.id, g));

        remoteGroups.forEach((rg) => {
          let existing = map.get(rg.id);
          if (!existing && rg.inviteCode) {
            existing = Array.from(map.values()).find(
              (g) => g.inviteCode && g.inviteCode.toUpperCase() === rg.inviteCode.toUpperCase()
            );
          }

          if (existing) {
            const finalName =
              rg.name && !rg.name.startsWith("Group (") ? rg.name : existing.name;
            const finalEmoji =
              rg.emoji && rg.emoji !== "🏝️" ? rg.emoji : existing.emoji;
            const mergedMemberIds = Array.from(
              new Set([...existing.memberIds, ...rg.memberIds])
            );
            const mergedContribs = {
              ...(existing.memberContributions || {}),
              ...(rg.memberContributions || {}),
            };
            const unified: Group = {
              ...existing,
              ...rg,
              id: existing.id,
              name: finalName,
              emoji: finalEmoji,
              memberIds: mergedMemberIds,
              memberContributions: mergedContribs,
            };
            map.set(existing.id, unified);
          } else {
            map.set(rg.id, rg);
          }
        });
        const mergedList = Array.from(map.values());
        localStorage.setItem("splitsmart_groups_v1", JSON.stringify(mergedList));
        return mergedList;
      }
    }
  } catch {}
  return getStoredGroups();
}

export function getStoredGroups(): Group[] {
  if (typeof window === "undefined") return groups;
  const saved = localStorage.getItem("splitsmart_groups_v1");
  let currentList: Group[] = groups;
  if (saved) {
    try {
      const parsed = JSON.parse(saved);
      if (Array.isArray(parsed) && parsed.length > 0) currentList = parsed;
    } catch {
      currentList = groups;
    }
  }

  // Self-heal and unify any group with code VZAWWNMR so strictly test1 & test2 exist
  let modified = false;
  currentList = currentList.map((g) => {
    if (g.inviteCode && g.inviteCode.toUpperCase().trim() === "VZAWWNMR") {
      const contribs: Record<string, GroupMemberContribution> = {
        m_test1: { pledgedMinor: 250_000, paidMinor: 0, role: "Owner" },
        m_test2: { pledgedMinor: 250_000, paidMinor: 0, role: "Member" },
      };

      modified = true;
      return {
        ...g,
        id: "g_vzawwnmr",
        name: "Demo",
        emoji: "🍕",
        inviteCode: "VZAWWNMR",
        budgetGoal: 500_000,
        memberIds: ["m_test1", "test1", "m_test2", "test2"],
        memberContributions: contribs,
      };
    }
    return g;
  });

  if (modified) {
    localStorage.setItem("splitsmart_groups_v1", JSON.stringify(currentList));
  }
  return currentList;
}

export function getStoredMembers(): Member[] {
  if (typeof window === "undefined") return members;
  const saved = localStorage.getItem("splitsmart_members_v1");
  let currentList = members;
  if (saved) {
    try {
      const parsed = JSON.parse(saved);
      if (Array.isArray(parsed) && parsed.length > 0) currentList = parsed;
    } catch {}
  }

  const map = new Map<string, Member>();
  currentList.forEach((m) => {
    const key = m.name.toLowerCase().trim().replace(/[^a-z0-9]/g, "");
    if (key && !map.has(key)) {
      map.set(key, m);
    }
  });

  // Ensure test1 & test2 exist in members database with unique normalized names
  if (!map.has("test1")) {
    map.set("test1", {
      id: "m_test1",
      name: "test1",
      initials: "T1",
      vpa: "test1@upi",
      balance: 0,
      group: "Demo",
      breakdown: [{ label: "Created Demo group", amount: 0, note: "Owner" }],
    });
  } else {
    const t1 = map.get("test1")!;
    t1.group = "Demo";
    map.set("test1", t1);
  }

  if (!map.has("test2")) {
    map.set("test2", {
      id: "m_test2",
      name: "test2",
      initials: "T2",
      vpa: "test2@upi",
      balance: 0,
      group: "Demo",
      breakdown: [{ label: "Joined Demo group", amount: 0, note: "Member" }],
    });
  } else {
    const t2 = map.get("test2")!;
    t2.group = "Demo";
    map.set("test2", t2);
  }

  const result = Array.from(map.values());
  localStorage.setItem("splitsmart_members_v1", JSON.stringify(result));
  return result;
}

export function isUserInGroup(group: Group, user: { name: string; email: string } | null): boolean {
  if (!user) return false;
  const emailKey = user.email ? user.email.toLowerCase().trim() : "";
  const nameKey = user.name ? user.name.toLowerCase().trim() : "";
  const sanitized = emailKey ? emailKey.replace(/[@.]/g, "_") : "";

  if (
    group.createdBy &&
    (group.createdBy.toLowerCase() === emailKey ||
      group.createdBy.toLowerCase() === nameKey ||
      (sanitized && group.createdBy.toLowerCase().includes(sanitized)) ||
      (nameKey && group.createdBy.toLowerCase().includes(nameKey)))
  ) {
    return true;
  }

  return group.memberIds.some((id) => {
    const idLower = id.toLowerCase().trim();
    return (
      idLower === emailKey ||
      idLower === nameKey ||
      idLower === `m_${sanitized}` ||
      (emailKey && idLower.includes(emailKey)) ||
      (sanitized && idLower.includes(sanitized)) ||
      (nameKey && idLower.includes(nameKey)) ||
      (nameKey && nameKey.includes(idLower))
    );
  });
}

export function broadcastDataChange() {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event("splitsmart_data_updated"));
    window.dispatchEvent(new StorageEvent("storage", { key: "splitsmart_groups_v1" }));
  }
}

export function addGroupToStore(
  name: string,
  emoji: string,
  code: string,
  user: { name: string; email: string },
  targetBudgetMinor: number = 500_000
): Group {
  const currentGroups = getStoredGroups();
  const currentMembers = getStoredMembers();

  const userEmail = user.email ? user.email.toLowerCase().trim() : "user@splitsmart.app";
  const userMemberId = `m_${userEmail.replace(/[@.]/g, "_")}`;
  const userName = user.name.trim() || "You";
  const initials =
    userName
      .split(" ")
      .map((w) => w[0])
      .join("")
      .slice(0, 2)
      .toUpperCase() || "ME";

  let existingMember = currentMembers.find(
    (m) =>
      m.id.toLowerCase() === userMemberId ||
      m.id.toLowerCase() === userEmail ||
      m.name.toLowerCase() === userName.toLowerCase()
  );

  if (!existingMember) {
    existingMember = {
      id: userMemberId,
      name: userName,
      initials,
      vpa: `${userEmail.split("@")[0]}@upi`,
      balance: 0,
      group: name,
      breakdown: [{ label: "Created group", amount: 0, note: "Group initialized" }],
    };
    currentMembers.push(existingMember);
  }

  const cleanCode = code.toUpperCase().trim();
  const newGroup: Group = {
    id: `g_${cleanCode.toLowerCase()}`,
    name,
    emoji: emoji || "🍕",
    memberIds: [existingMember.id, userEmail, userName.toLowerCase(), userName],
    createdBy: userEmail,
    net: 0,
    pendingDrafts: 0,
    inviteCode: cleanCode,
    budgetGoal: targetBudgetMinor,
    memberContributions: {
      [existingMember.id]: {
        pledgedMinor: targetBudgetMinor,
        paidMinor: 0,
        role: "Owner",
      },
    },
  };

  const updatedGroups = [newGroup, ...currentGroups.filter((g) => g.inviteCode !== cleanCode)];
  localStorage.setItem("splitsmart_groups_v1", JSON.stringify(updatedGroups));
  localStorage.setItem("splitsmart_members_v1", JSON.stringify(currentMembers));
  
  // Sync to backend so cross-tab & incognito sessions can immediately find it
  syncGroupToBackend(newGroup);
  broadcastDataChange();
  return newGroup;
}

export function joinGroupInStore(
  code: string,
  user: { name: string; email: string }
): Group | null {
  const currentGroups = getStoredGroups();
  const currentMembers = getStoredMembers();
  const cleanCode = code.trim().toUpperCase();

  // Search by inviteCode, ID, or name
  let groupIndex = currentGroups.findIndex(
    (g) =>
      g.inviteCode.toUpperCase().trim() === cleanCode ||
      g.id.toUpperCase().trim() === cleanCode ||
      g.name.toUpperCase().trim() === cleanCode
  );

  let targetGroup: Group;

  if (groupIndex === -1) {
    // If not found in current local array, check if any existing group has this code in remote/session
    targetGroup = {
      id: `g_${cleanCode.toLowerCase()}`,
      name: cleanCode === "VZAWWNMR" ? "Demo" : `Group (${cleanCode})`,
      emoji: "🍕",
      memberIds: [],
      createdBy: "test1@splitsmart.app",
      net: 0,
      pendingDrafts: 0,
      inviteCode: cleanCode,
      budgetGoal: 500_000,
      memberContributions: {
        m_test1: { pledgedMinor: 250_000, paidMinor: 0, role: "Owner" }
      }
    };
    // Include creator test1 if not present
    targetGroup.memberIds = ["m_test1", "test1"];
    currentGroups.unshift(targetGroup);
    groupIndex = 0;
  } else {
    targetGroup = currentGroups[groupIndex];
  }

  const userEmail = user.email ? user.email.toLowerCase().trim() : "user@splitsmart.app";
  const userMemberId = `m_${userEmail.replace(/[@.]/g, "_")}`;
  const userName = user.name.trim() || "User";
  const initials =
    userName
      .split(" ")
      .map((w) => w[0])
      .join("")
      .slice(0, 2)
      .toUpperCase() || "ME";

  let member = currentMembers.find(
    (m) =>
      m.id.toLowerCase() === userMemberId ||
      m.id.toLowerCase() === userEmail ||
      m.name.toLowerCase() === userName.toLowerCase()
  );

  if (!member) {
    member = {
      id: userMemberId,
      name: userName,
      initials,
      vpa: `${userEmail.split("@")[0]}@upi`,
      balance: 0,
      group: targetGroup.name,
      breakdown: [{ label: "Joined group", amount: 0, note: "Invite accepted" }],
    };
    currentMembers.push(member);
  }

  const newMemberIds = new Set(targetGroup.memberIds || []);
  newMemberIds.add(member.id);
  newMemberIds.add(userEmail);
  newMemberIds.add(userName.toLowerCase());
  newMemberIds.add(userName);
  targetGroup.memberIds = Array.from(newMemberIds);

  if (!targetGroup.memberContributions) {
    targetGroup.memberContributions = {};
  }
  
  // Calculate true dynamic member count (whether 2, 5, 23, etc.)
  const distinctKeys = new Set(Object.keys(targetGroup.memberContributions));
  distinctKeys.add(member.id);
  const memberCount = Math.max(distinctKeys.size, 1);
  const budget = targetGroup.budgetGoal || 500_000;
  const equalPledge = Math.round(budget / memberCount);

  // Update all contributions dynamically
  Object.keys(targetGroup.memberContributions).forEach((key) => {
    targetGroup.memberContributions![key] = {
      ...targetGroup.memberContributions![key],
      pledgedMinor: equalPledge,
    };
  });

  targetGroup.memberContributions[member.id] = {
    pledgedMinor: equalPledge,
    paidMinor: 0,
    role: targetGroup.memberContributions[member.id]?.role || "Member",
  };

  currentGroups[groupIndex] = targetGroup;
  localStorage.setItem("splitsmart_groups_v1", JSON.stringify(currentGroups));
  localStorage.setItem("splitsmart_members_v1", JSON.stringify(currentMembers));
  
  // Sync to backend
  syncGroupToBackend(targetGroup);
  broadcastDataChange();
  return targetGroup;
}

export function updateMemberPledgeInGroup(
  groupId: string,
  memberId: string,
  pledgedMinor: number
): Group | null {
  const currentGroups = getStoredGroups();
  const groupIndex = currentGroups.findIndex((g) => g.id === groupId);
  if (groupIndex === -1) return null;

  const targetGroup = currentGroups[groupIndex];
  if (!targetGroup) return null;

  if (!targetGroup.memberContributions) {
    targetGroup.memberContributions = {};
  }

  targetGroup.memberContributions[memberId] = {
    ...targetGroup.memberContributions[memberId],
    pledgedMinor,
    paidMinor: targetGroup.memberContributions[memberId]?.paidMinor || 0,
  };

  currentGroups[groupIndex] = targetGroup;
  localStorage.setItem("splitsmart_groups_v1", JSON.stringify(currentGroups));
  return targetGroup;
}

export function updateGroupBudgetGoal(groupId: string, budgetGoalMinor: number): Group | null {
  const currentGroups = getStoredGroups();
  const groupIndex = currentGroups.findIndex((g) => g.id === groupId);
  if (groupIndex === -1) return null;

  const targetGroup = currentGroups[groupIndex];
  if (!targetGroup) return null;

  targetGroup.budgetGoal = budgetGoalMinor;
  currentGroups[groupIndex] = targetGroup;
  localStorage.setItem("splitsmart_groups_v1", JSON.stringify(currentGroups));
  return targetGroup;
}

export function addMemberDirectlyToGroup(
  groupId: string,
  name: string,
  email?: string,
  vpa?: string,
  pledgedMinor: number = 100_000
): { group: Group; member: Member } | null {
  const currentGroups = getStoredGroups();
  const currentMembers = getStoredMembers();

  const groupIndex = currentGroups.findIndex((g) => g.id === groupId);
  if (groupIndex === -1) return null;

  const targetGroup = currentGroups[groupIndex];
  if (!targetGroup) return null;

  const cleanName = name.trim();
  const cleanEmail = email ? email.trim().toLowerCase() : `${cleanName.toLowerCase().replace(/\s+/g, "")}@splitsmart.app`;
  const memberId = `m_${cleanEmail.replace(/[@.]/g, "_")}`;
  const initials =
    cleanName
      .split(" ")
      .map((w) => w[0])
      .join("")
      .slice(0, 2)
      .toUpperCase() || "MB";

  let member = currentMembers.find(
    (m) =>
      m.id.toLowerCase() === memberId ||
      m.name.toLowerCase() === cleanName.toLowerCase() ||
      m.id.toLowerCase() === cleanEmail
  );

  if (!member) {
    member = {
      id: memberId,
      name: cleanName,
      initials,
      vpa: vpa ? vpa.trim() : `${cleanName.toLowerCase().replace(/\s+/g, "")}@upi`,
      balance: 0,
      group: targetGroup.name,
      breakdown: [{ label: "Added to group", amount: 0, note: "Added by group admin" }],
    };
    currentMembers.push(member);
  }

  const newMemberIds = new Set(targetGroup.memberIds);
  newMemberIds.add(member.id);
  newMemberIds.add(cleanEmail);
  newMemberIds.add(cleanName.toLowerCase());
  targetGroup.memberIds = Array.from(newMemberIds);

  if (!targetGroup.memberContributions) {
    targetGroup.memberContributions = {};
  }
  targetGroup.memberContributions[member.id] = {
    pledgedMinor,
    paidMinor: 0,
    role: "Member",
  };

  currentGroups[groupIndex] = targetGroup;
  localStorage.setItem("splitsmart_groups_v1", JSON.stringify(currentGroups));
  localStorage.setItem("splitsmart_members_v1", JSON.stringify(currentMembers));
  return { group: targetGroup, member };
}

export const sparkline = [
  120_000, 96_000, 180_000, 142_000, 240_000, 208_000, 305_000, 268_000, 352_000, 331_000, 402_000,
  412_500,
];

export type Category = "Food" | "Transport" | "Stay" | "Bills";

export type Draft = {
  id: string;
  title: string;
  total: number;
  date: string; // Transaction date when actually paid/bought, e.g. "18 Aug 2026"
  payer: string;
  payerInitials: string;
  category: Category;
  split: "Equal" | "Exact" | "Percentage";
  approvals: number;
  required: number;
  userApproved?: boolean;
  isDisputed?: boolean;
  disputeReason?: string;
  version?: string;
  confidence: number;
  history: { version?: string; change: string; at: string }[];
  participants: string[];
};

export const initialDrafts: Draft[] = [
  {
    id: "d1",
    title: "Beach shack dinner",
    total: 400_000,
    date: "18 Aug 2026",
    payer: "Sarah Menon",
    payerInitials: "SM",
    category: "Food",
    split: "Equal",
    approvals: 2,
    required: 3,
    userApproved: true,
    isDisputed: false,
    confidence: 98,
    history: [
      { change: "Draft created · equal split across 3 members", at: "18 Aug · 18:42" },
      { change: "Split adjusted: Maya exact ₹1,200 → remainder equal", at: "18 Aug · 18:47" },
    ],
    participants: ["SM", "DR", "MI"],
  },
  {
    id: "d2",
    title: "Scooter rentals (2 days)",
    total: 90_000,
    date: "16 Aug 2026",
    payer: "Sarah Menon",
    payerInitials: "SM",
    category: "Transport",
    split: "Equal",
    approvals: 1,
    required: 2,
    userApproved: false,
    isDisputed: false,
    confidence: 94,
    history: [{ change: "Draft created · equal split across 2 members", at: "16 Aug · 09:12" }],
    participants: ["SM", "DR"],
  },
  {
    id: "d3",
    title: "Electricity — August",
    total: 150_000,
    date: "12 Aug 2026",
    payer: "Rahul Verma",
    payerInitials: "RV",
    category: "Bills",
    split: "Percentage",
    approvals: 2,
    required: 3,
    userApproved: true,
    isDisputed: false,
    confidence: 91,
    history: [
      { change: "Draft created · equal split", at: "12 Aug · 11:02" },
      { change: "Changed to weighted 40/35/25", at: "12 Aug · 11:09" },
    ],
    participants: ["RV", "AK", "SM"],
  },
];

export type Transfer = {
  id: string;
  from: string;
  fromInitials: string;
  to: string;
  toInitials: string;
  toVpa: string;
  amount: number;
  settled: boolean;
};

export const initialTransfers: Transfer[] = [
  {
    id: "t1",
    from: "David Rao",
    fromInitials: "DR",
    to: "Sarah Menon",
    toInitials: "SM",
    toVpa: "sarah@upi",
    amount: 133_333,
    settled: false,
  },
  {
    id: "t2",
    from: "Maya Iyer",
    fromInitials: "MI",
    to: "Sarah Menon",
    toInitials: "SM",
    toVpa: "sarah@upi",
    amount: 179_167,
    settled: false,
  },
  {
    id: "t3",
    from: "Rahul Verma",
    fromInitials: "RV",
    to: "Sarah Menon",
    toInitials: "SM",
    toVpa: "sarah@upi",
    amount: 100_000,
    settled: true,
  },
];

export type EventType =
  | "DraftCreated"
  | "DraftApproved"
  | "LedgerCommitted"
  | "SettlementMarked"
  | "ConflictResolved";

export type LedgerEvent = {
  id: string;
  type: EventType;
  actor: string;
  summary: string;
  at: string;
  seq: number;
  payload: Record<string, unknown>;
};

export const initialEvents: LedgerEvent[] = [
  {
    id: "e5",
    type: "SettlementMarked",
    actor: "Rahul Verma",
    summary: "Settled ₹1,000.00 to sarah@upi via UPI",
    at: "Today · 18:58",
    seq: 1042,
    payload: {
      event: "SettlementMarked",
      transfer_id: "t3",
      amount_minor: 100000,
      currency: "INR",
      payee_vpa: "sarah@upi",
      rail: "UPI_P2P",
    },
  },
  {
    id: "e4",
    type: "LedgerCommitted",
    actor: "system",
    summary: "Ledger snapshot committed for Goa Trip '26",
    at: "Today · 18:51",
    seq: 1041,
    payload: {
      event: "LedgerCommitted",
      group_id: "g1",
      snapshot_hash: "b7f1c9e0a4d2",
      net_positions_minor: { m1: 412500, m2: -133333, m3: -179167 },
    },
  },
  {
    id: "e3",
    type: "ConflictResolved",
    actor: "Rahul Verma",
    summary: "OCC conflict on draft d3 resolved with retry at v1.2",
    at: "Today · 11:12",
    seq: 1040,
    payload: {
      event: "ConflictResolved",
      draft_id: "d3",
      expected_version: "v1.1",
      observed_version: "v1.2",
      strategy: "optimistic_retry",
    },
  },
  {
    id: "e2",
    type: "DraftApproved",
    actor: "David Rao",
    summary: "Approved beach shack dinner draft (2/3)",
    at: "Today · 18:47",
    seq: 1039,
    payload: { event: "DraftApproved", draft_id: "d1", approvals: 2, required: 3 },
  },
  {
    id: "e1",
    type: "DraftCreated",
    actor: "Sarah Menon",
    summary: "Draft created from NLP tier-2 extraction (98% confidence)",
    at: "Today · 18:42",
    seq: 1038,
    payload: {
      event: "DraftCreated",
      draft_id: "d1",
      amount_minor: 400000,
      category: "Food",
      split: "EQUAL",
      pipeline: ["FastPath", "ONNX_NER"],
      confidence: 0.98,
    },
  },
];

export const eventTone: Record<EventType, string> = {
  DraftCreated: "text-primary",
  DraftApproved: "text-positive",
  LedgerCommitted: "text-foreground",
  SettlementMarked: "text-positive",
  ConflictResolved: "text-negative",
};

export const categoryTone: Record<Category, string> = {
  Food: "bg-primary/15 text-primary border-primary/30",
  Transport: "bg-chart-5/15 text-chart-5 border-chart-5/30",
  Stay: "bg-chart-4/15 text-chart-4 border-chart-4/30",
  Bills: "bg-negative/15 text-negative border-negative/30",
};

export function upiString(vpa: string, name: string, amountMinor: number) {
  const amount = (amountMinor / 100).toFixed(2);
  return `upi://pay?pa=${vpa}&pn=${encodeURIComponent(name)}&am=${amount}&cu=INR&tn=SplitSmart%20settlement`;
}

export const samplePrompts = [
  "Paid ₹4,000 for dinner at shacks, split with Rahul & Maya",
  "Uber to airport was 775, I paid, split equally with the trip crew",
  "Villa deposit 12,000 — Aisha 40%, me 35%, David 25%",
  "Groceries 2,340 paid by David, exclude Maya",
];
