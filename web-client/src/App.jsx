import { useEffect, useState } from "react";
import "./App.css";

const API = "http://localhost:8888";

async function api(path, options = {}) {
  const res = await fetch(API + path, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  const data = await res.json().catch(() => null);
  if (!res.ok) throw new Error(data?.error || `Erreur HTTP ${res.status}`);
  return data;
}

export default function App() {
  const [tab, setTab] = useState("accounts");
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [notice, setNotice] = useState(null);

  const [payment, setPayment] = useState({
    accountId: "", amount: "", currency: "EUR", merchantCategory: "GROCERY", country: "MA",
  });
  const [transfer, setTransfer] = useState({
    fromAccountId: "", toAccountId: "", amount: "", currency: "EUR",
  });

  const [chatInput, setChatInput] = useState("");
  const [chat, setChat] = useState([]);
  const [chatLoading, setChatLoading] = useState(false);

  const refCustomer = (id) => accounts.find((a) => a.id === id)?.customerReference || id?.slice(0, 8);

  async function load() {
    try {
      const [acc, trx] = await Promise.all([api("/api/accounts"), api("/api/transactions")]);
      setAccounts(acc);
      setTransactions([...trx].sort((a, b) => b.createdAt.localeCompare(a.createdAt)));
    } catch (e) {
      setNotice({ type: "error", text: "Gateway injoignable : " + e.message });
    }
  }

  useEffect(() => { load(); }, []);

  async function submitPayment(e) {
    e.preventDefault();
    try {
      await api("/api/transactions", {
        method: "POST",
        body: JSON.stringify({ ...payment, amount: Number(payment.amount) }),
      });
      setNotice({ type: "success", text: "Paiement enregistre avec succes" });
      setPayment({ ...payment, amount: "" });
      load();
    } catch (err) {
      setNotice({ type: "error", text: err.message });
    }
  }

  async function submitTransfer(e) {
    e.preventDefault();
    try {
      await api("/api/transactions/transfer", {
        method: "POST",
        body: JSON.stringify({ ...transfer, amount: Number(transfer.amount) }),
      });
      setNotice({ type: "success", text: "Virement effectue avec succes" });
      setTransfer({ ...transfer, amount: "" });
      load();
    } catch (err) {
      setNotice({ type: "error", text: err.message });
    }
  }

  async function toggleStatus(acc) {
    const status = acc.status === "ACTIVE" ? "BLOCKED" : "ACTIVE";
    try {
      await api(`/api/accounts/${acc.id}/status?status=${status}`, { method: "PATCH" });
      load();
    } catch (err) {
      setNotice({ type: "error", text: err.message });
    }
  }

  async function sendChat(e) {
    e.preventDefault();
    if (!chatInput.trim()) return;
    const question = chatInput;
    setChat((c) => [...c, { role: "user", text: question }]);
    setChatInput("");
    setChatLoading(true);
    try {
      const data = await api("/api/chat", {
        method: "POST",
        body: JSON.stringify({ message: question, conversationId: "web-client" }),
      });
      setChat((c) => [...c, { role: "bot", text: data.answer }]);
      load();
    } catch (err) {
      setChat((c) => [...c, { role: "bot", text: "Erreur : " + err.message }]);
    } finally {
      setChatLoading(false);
    }
  }

  const activeAccounts = accounts.filter((a) => a.status === "ACTIVE");

  return (
    <div className="app">
      <header>
        <h1>🏦 Bank Platform</h1>
        <nav>
          {[
            ["accounts", "Comptes"],
            ["transactions", "Transactions"],
            ["operations", "Paiement / Virement"],
            ["chat", "Assistant IA"],
          ].map(([key, label]) => (
            <button key={key} className={tab === key ? "active" : ""} onClick={() => setTab(key)}>
              {label}
            </button>
          ))}
        </nav>
      </header>

      {notice && (
        <div className={`notice ${notice.type}`} onClick={() => setNotice(null)}>
          {notice.text} <span>✕</span>
        </div>
      )}

      {tab === "accounts" && (
        <section>
          <div className="section-head">
            <h2>Comptes bancaires</h2>
            <button onClick={load}>↻ Rafraichir</button>
          </div>
          <table>
            <thead>
              <tr><th>Client</th><th>ID</th><th>Statut</th><th>Devise</th><th>Solde</th><th></th></tr>
            </thead>
            <tbody>
              {accounts.map((a) => (
                <tr key={a.id}>
                  <td><b>{a.customerReference}</b></td>
                  <td className="mono">{a.id}</td>
                  <td><span className={`badge ${a.status}`}>{a.status}</span></td>
                  <td>{a.currency}</td>
                  <td className="num">{a.balance.toFixed(2)}</td>
                  <td>
                    <button className="small" onClick={() => toggleStatus(a)}>
                      {a.status === "ACTIVE" ? "Bloquer" : "Activer"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      {tab === "transactions" && (
        <section>
          <div className="section-head">
            <h2>Transactions</h2>
            <button onClick={load}>↻ Rafraichir</button>
          </div>
          <table>
            <thead>
              <tr><th>Date</th><th>Client</th><th>Type</th><th>Montant</th><th>Categorie</th><th>Pays</th></tr>
            </thead>
            <tbody>
              {transactions.map((t) => (
                <tr key={t.id}>
                  <td>{new Date(t.createdAt).toLocaleString("fr-FR")}</td>
                  <td>{refCustomer(t.accountId)}</td>
                  <td>{t.type}</td>
                  <td className={`num ${t.type === "TRANSFER_CREDIT" ? "credit" : "debit"}`}>
                    {t.type === "TRANSFER_CREDIT" ? "+" : "-"}{Number(t.amount).toFixed(2)} {t.currency}
                  </td>
                  <td>{t.merchantCategory}</td>
                  <td>{t.country || "-"}</td>
                </tr>
              ))}
              {transactions.length === 0 && (
                <tr><td colSpan="6" className="empty">Aucune transaction</td></tr>
              )}
            </tbody>
          </table>
        </section>
      )}

      {tab === "operations" && (
        <section className="forms">
          <form onSubmit={submitPayment}>
            <h2>Nouveau paiement</h2>
            <label>Compte</label>
            <select value={payment.accountId} required
                    onChange={(e) => {
                      const acc = accounts.find((a) => a.id === e.target.value);
                      setPayment({ ...payment, accountId: e.target.value, currency: acc?.currency || "EUR" });
                    }}>
              <option value="">-- choisir --</option>
              {activeAccounts.map((a) => (
                <option key={a.id} value={a.id}>{a.customerReference} ({a.balance.toFixed(2)} {a.currency})</option>
              ))}
            </select>
            <label>Montant</label>
            <input type="number" step="0.01" required value={payment.amount}
                   onChange={(e) => setPayment({ ...payment, amount: e.target.value })} />
            <label>Devise</label>
            <input value={payment.currency} onChange={(e) => setPayment({ ...payment, currency: e.target.value })} />
            <label>Categorie marchand</label>
            <input value={payment.merchantCategory}
                   onChange={(e) => setPayment({ ...payment, merchantCategory: e.target.value })} />
            <label>Pays</label>
            <input value={payment.country} onChange={(e) => setPayment({ ...payment, country: e.target.value })} />
            <button type="submit">Payer</button>
          </form>

          <form onSubmit={submitTransfer}>
            <h2>Nouveau virement</h2>
            <label>Compte source</label>
            <select value={transfer.fromAccountId} required
                    onChange={(e) => {
                      const acc = accounts.find((a) => a.id === e.target.value);
                      setTransfer({ ...transfer, fromAccountId: e.target.value, currency: acc?.currency || "EUR" });
                    }}>
              <option value="">-- choisir --</option>
              {activeAccounts.map((a) => (
                <option key={a.id} value={a.id}>{a.customerReference} ({a.balance.toFixed(2)} {a.currency})</option>
              ))}
            </select>
            <label>Compte destination</label>
            <select value={transfer.toAccountId} required
                    onChange={(e) => setTransfer({ ...transfer, toAccountId: e.target.value })}>
              <option value="">-- choisir --</option>
              {accounts.filter((a) => a.id !== transfer.fromAccountId).map((a) => (
                <option key={a.id} value={a.id}>{a.customerReference} ({a.currency})</option>
              ))}
            </select>
            <label>Montant</label>
            <input type="number" step="0.01" required value={transfer.amount}
                   onChange={(e) => setTransfer({ ...transfer, amount: e.target.value })} />
            <label>Devise</label>
            <input value={transfer.currency} onChange={(e) => setTransfer({ ...transfer, currency: e.target.value })} />
            <button type="submit">Virer</button>
          </form>
        </section>
      )}

      {tab === "chat" && (
        <section className="chat">
          <h2>Assistant bancaire IA</h2>
          <div className="messages">
            {chat.length === 0 && (
              <p className="hint">Exemples : « Liste les comptes », « Solde de CUST-001 ? », « Paye 40 EUR chez GROCERY pour CUST-002 »</p>
            )}
            {chat.map((m, i) => (
              <div key={i} className={`msg ${m.role}`}>{m.text}</div>
            ))}
            {chatLoading && <div className="msg bot">…</div>}
          </div>
          <form onSubmit={sendChat} className="chat-form">
            <input value={chatInput} placeholder="Posez votre question..."
                   onChange={(e) => setChatInput(e.target.value)} />
            <button type="submit" disabled={chatLoading}>Envoyer</button>
          </form>
        </section>
      )}
    </div>
  );
}