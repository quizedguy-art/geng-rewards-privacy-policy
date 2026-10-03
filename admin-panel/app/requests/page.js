'use client';

import { useState, useEffect } from 'react';
import { db } from '../firebase';
import { collection, onSnapshot, query, orderBy, doc, updateDoc, writeBatch, increment, getDoc } from 'firebase/firestore';

export default function RequestsPage() {
  const [withdrawals, setWithdrawals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [userEmails, setUserEmails] = useState({});

  useEffect(() => {
    const fetchRequests = (collName) => {
      const q = collection(db, collName);
      return onSnapshot(q, (snapshot) => {
        const docs = snapshot.docs.map(doc => ({
          id: doc.id,
          collectionName: collName,
          ...doc.data()
        }));
        
        // Filter pending requests
        const pendingDocs = docs.filter(w => (w.status === 'Pending' || w.status === 'pending'));
        
        setWithdrawals(prev => {
          const map = new Map();
          [...prev, ...pendingDocs].forEach(item => map.set(item.id, item));
          const combined = Array.from(map.values())
            .filter(w => (w.status === 'Pending' || w.status === 'pending'))
            .sort((a, b) => (b.requestedAt || b.createdAt || 0) - (a.requestedAt || a.createdAt || 0));
          return combined;
        });

        // Fetch emails for unique users
        const uniqueUserIds = [...new Set(pendingDocs.map(w => w.userId).filter(Boolean))];
        uniqueUserIds.forEach(async (uid) => {
          try {
            const userSnap = await getDoc(doc(db, "users", uid));
            if (userSnap.exists() && (userSnap.data().email || userSnap.data().name)) {
              const uData = userSnap.data();
              setUserEmails(prev => ({
                ...prev, 
                [uid]: uData.email || uData.name || uid
              }));
            }
          } catch (e) {
            console.error("Error fetching user email:", e);
          }
        });

        setLoading(false);
      }, (error) => {
        console.error(`Firestore error on ${collName}:`, error);
        setLoading(false);
      });
    };

    const unsub1 = fetchRequests("withdrawalRequests");
    const unsub2 = fetchRequests("withdrawals");

    return () => {
      unsub1();
      unsub2();
    };
  }, []);

  const handleApprove = async (item) => {
    const code = prompt("Enter Gift Card Code / Transaction Reference for this user:");
    if (code) {
      try {
        const collName = item.collectionName || "withdrawals";
        const docRef = doc(db, collName, item.id);
        await updateDoc(docRef, {
          status: 'Approved',
          giftCardCode: code,
          adminNotes: `Approved with code/ref: ${code}`,
          processedAt: Date.now()
        });
        alert(`Success: Code issued and request approved for ${item.id}`);
      } catch (error) {
        alert("Error approving request: " + error.message);
      }
    }
  };

  const handleReject = async (item) => {
    if (confirm("Are you sure you want to reject this withdrawal request?")) {
      try {
        const batch = writeBatch(db);
        const collName = item.collectionName || "withdrawals";
        
        // 1. Update the withdrawal request status to 'Rejected'
        const docRef = doc(db, collName, item.id);
        batch.update(docRef, {
          status: 'Rejected',
          processedAt: Date.now()
        });

        // 2. Refund points or balance to user
        if (item.userId) {
          const userRef = doc(db, "users", item.userId);
          const refundAmount = item.amount || item.amountRs || 0;
          const pointsDeducted = item.pointsDeducted || 0;
          
          const updates = {};
          if (pointsDeducted > 0) {
            updates.points = increment(pointsDeducted);
          }
          if (refundAmount > 0) {
            updates.availableBalance = increment(refundAmount);
          }
          if (Object.keys(updates).length > 0) {
            batch.update(userRef, updates);
          }
        }

        await batch.commit();
        alert(`Success: Withdrawal request rejected and refunded to user.`);
      } catch (error) {
        alert("Error rejecting withdrawal: " + error.message);
      }
    }
  };

  const formatDate = (val) => {
    if (!val) return 'N/A';
    const d = val.toDate ? val.toDate() : new Date(val);
    return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="dashboard-page">
      <div className="header">
        <h1>Pending Withdrawal Requests</h1>
      </div>

      <div className="table-container">
        {loading ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: '#64748b' }}>Syncing Requests...</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Request Date</th>
                <th>User / Email</th>
                <th>Amount & Method</th>
                <th>Payment Details</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {withdrawals.length === 0 ? (
                <tr><td colSpan="5" style={{ textAlign: 'center', padding: '3rem' }}>No pending requests.</td></tr>
              ) : (
                withdrawals.map(w => (
                  <tr key={w.id}>
                    <td>{formatDate(w.requestedAt || w.createdAt)}</td>
                    <td>
                      <code style={{ fontSize: '0.8rem' }}>{w.userId}</code>
                      {(userEmails[w.userId] || w.userEmail || w.userName) && (
                        <div style={{ fontSize: '0.85rem', color: '#3b82f6', marginTop: '0.2rem', wordBreak: 'break-all' }}>
                          {userEmails[w.userId] || w.userEmail || w.userName}
                        </div>
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
                      <code style={{ fontSize: '0.85rem' }}>{w.paymentDetails || w.details || 'N/A'}</code>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '0.5rem' }}>
                        <button className="btn-approve" onClick={() => handleApprove(w)}>Issue Code / Pay</button>
                        <button onClick={() => handleReject(w)} style={{ background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer', fontWeight: 'bold', padding: '0.5rem' }}>Reject</button>
                      </div>
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
