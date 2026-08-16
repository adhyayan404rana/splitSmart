const API_BASE_URL = "http://localhost:8080/api/v1";

export interface BackendNlpRequest {
  rawInput: string;
  payerName: string;
}

export interface BackendExpenseDraft {
  totalAmountCents: number;
  payerName: string;
  currency: string;
  description: string;
  category: string;
  participants: string[];
  excludedParticipants: string[];
  splitLogic: string;
  confidenceScore: number;
  extractionSource: string;
}

export interface BackendSettlementRequest {
  groupId: string;
  debtorId: string;
  creditorId: string;
  amountCents: number;
  note?: string;
}

export async function parseNlpInput(rawInput: string, payerName = "Sarah"): Promise<BackendExpenseDraft | null> {
  try {
    const res = await fetch(`${API_BASE_URL}/ingestion/parse`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ rawInput, payerName }),
    });

    if (!res.ok) return null;
    return await res.json();
  } catch (err) {
    console.warn("Backend API offline or unreachable, using local fallback NLP parsing:", err);
    return null;
  }
}

export async function fetchBackendGroupBalances(groupId = "g1") {
  try {
    const res = await fetch(`${API_BASE_URL}/ledger/groups/${groupId}/balances`);
    if (!res.ok) return null;
    return await res.json();
  } catch (err) {
    console.warn("Backend API unreachable:", err);
    return null;
  }
}

export async function fetchBackendMinimizedSettlement(groupId = "g1") {
  try {
    const res = await fetch(`${API_BASE_URL}/settlement/groups/${groupId}/minimize`);
    if (!res.ok) return null;
    return await res.json();
  } catch (err) {
    console.warn("Backend API unreachable:", err);
    return null;
  }
}

export async function postBackendSettlement(req: BackendSettlementRequest) {
  try {
    const res = await fetch(`${API_BASE_URL}/payments/settle`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(req),
    });
    if (!res.ok) return null;
    return await res.json();
  } catch (err) {
    console.warn("Backend API unreachable:", err);
    return null;
  }
}

export async function fetchBackendAuditEvents(groupId = "g1") {
  try {
    const res = await fetch(`${API_BASE_URL}/ledger/groups/${groupId}/events`);
    if (!res.ok) return null;
    return await res.json();
  } catch (err) {
    console.warn("Backend API unreachable:", err);
    return null;
  }
}
