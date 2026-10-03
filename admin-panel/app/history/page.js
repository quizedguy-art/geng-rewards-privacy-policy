'use client';

import { useState, useEffect } from 'react';
import { db } from '../firebase';
import { collection, onSnapshot, query, orderBy } from 'firebase/firestore';

export default function HistoryPage() {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchHistory = (collName) => {
      const q = collection(db, collName);
      return onSnapshot(q, (snapshot) => {
        const docs = snapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));

        const nonPending = docs.filter(w => (w.status !== 'Pending' && w.status !== 'pending'));

        setHistory(prev => {
          const map = new Map();
          [...prev, ...nonPending].forEach(item => map.set(item.id, item));
          const combined = Array.from(map.values())
            .sort((a, b) => (b.processedAt || b.requestedAt || b.createdAt || 0) - (a.processedAt || a.requestedAt || a.createdAt || 0));
          return combined;
        });
        setLoading(false);
      }, (error) => {
        console.error(`History fetch error on ${collName}:`, error);
        setLoading(false);
      });
    };

    const unsub1 = fetchHistory("withdrawalRequests");
    const unsub2 = fetchHistory("withdrawals");

    return () => {
      unsub1();
      unsub2();
    };
  }, []);

  const formatDate = (val) => {
    if (!val) return 'N/A';
    const d = val.toDate ? val.toDate() : new Date(val);
    return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="dashboard-page">
      <div className="header">
        <h1>Reward & Withdrawal History</h1>
      </div>

      <div className="table-container">
        {loading ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: '#64748b' }}>Syncing History...</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Processed Date</th>
                <th>User ID / Email</th>
                <th>Amount & Method</th>
                <th>Status</th>
                <th>Code / Ref</th>
              </tr>
            </thead>
            <tbody>
              {history.length === 0 ? (
                <tr><td colSpan="5" style={{ textAlign: 'center', padding: '3rem' }}>No history found.</td></tr>
              ) : (
                history.map(w => (
                  <tr key={w.id}>
                    <td>{formatDate(w.processedAt || w.requestedAt || w.createdAt)}</td>
                    <td>
                      <code style={{ fontSize: '0.8rem' }}>{w.userId}</code>
                      {(w.userEmail || w.userName) && (
                        <div style={{ fontSize: '0.8rem', color: '#64748b' }}>{w.userEmail || w.userName}</div>
                      )}
                    </td>
                    <td style={{ fontWeight: 'bold' }}>
                      ₹{w.amount || w.amountRs}
                      {(w.paymentMethod || w.rewardName) && (
                        <span style={{ fontSize: '0.8rem', fontWeight: 'normal', display: 'block', color: '#64748b' }}>
                          ({w.paymentMethod || w.rewardName})
                        </span>
                      )}
                    </td>
                    <td>
                      <span className={`badge-${(w.status || '').toLowerCase()}`}>{w.status}</span>
                    </td>
                    <td>
                      {(w.status === 'Approved' || w.status === 'approved' || w.status === 'completed') ? (
                        <code style={{ fontWeight: 'bold', color: '#10b981' }}>{w.giftCardCode || w.adminNotes || 'Completed'}</code>
                      ) : (
                        <span style={{ color: '#64748b', fontSize: '0.9rem' }}>N/A</span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
