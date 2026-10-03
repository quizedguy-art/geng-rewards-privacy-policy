'use client';

import { useState, useEffect } from 'react';
import { db } from '../firebase';
import { collection, onSnapshot, query, orderBy, doc, updateDoc } from 'firebase/firestore';

export default function UsageReviewPage() {
  const [usageRecords, setUsageRecords] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const qUsage = query(collection(db, "daily_usage"), orderBy("date", "desc"));
    const unsubscribeUsage = onSnapshot(qUsage, (snapshot) => {
      const docs = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
      setUsageRecords(docs);
      setLoading(false);
    }, (error) => {
      console.error("Usage fetch error:", error);
      setLoading(false);
    });

    return () => unsubscribeUsage();
  }, []);

  const handleCreditPoints = async (record) => {
    const pointsToCredit = prompt(`Enter points to credit for ${record.userId} on ${record.date}:`, record.pointsPotential || 0);
    if (pointsToCredit !== null) {
      const points = parseInt(pointsToCredit);
      if (isNaN(points)) return alert("Invalid points value");

      try {
        const usageRef = doc(db, "daily_usage", record.id);
        await updateDoc(usageRef, {
          isApproved: true,
          isCollected: false,
          pointsPotential: points // Store the actual points credited
        });

        alert(`Successfully approved/credited ${points} points. The user can now collect them in the app.`);
      } catch (error) {
        alert("Error approving points: " + error.message);
      }
    }
  };

  const formatUsage = (millis) => {
    if (!millis) return '0h 0m';
    const hours = Math.floor(millis / 3600000);
    const minutes = Math.floor((millis % 3600000) / 60000);
    return `${hours}h ${minutes}m`;
  };

  const getTodayDateString = () => {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };
  const todayStr = getTodayDateString();

  return (
    <div className="dashboard-page">
      <div className="header">
        <h1>Daily Usage Review</h1>
      </div>

      <div className="table-container">
        {loading ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: '#64748b' }}>Syncing Usage Data...</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Usage Date</th>
                <th>User ID</th>
                <th>Screen Time</th>
                <th>Suggested Points</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {usageRecords.length === 0 ? (
                <tr><td colSpan="6" style={{ textAlign: 'center', padding: '3rem' }}>No usage records found.</td></tr>
              ) : (
                usageRecords.map(r => (
                  <tr key={r.id}>
                    <td>{r.date}</td>
                    <td><code style={{ fontSize: '0.8rem' }}>{r.userId}</code></td>
                    <td style={{ fontWeight: 'bold', color: r.totalMillis > 7 * 3600000 ? '#ef4444' : '#10b981' }}>{formatUsage(r.totalMillis)}</td>
                    <td>{r.pointsPotential} pts</td>
                    <td>
                      <span className={`badge-${r.isCollected ? 'approved' : r.isApproved ? 'credited' : 'pending'}`}>
                        {r.isCollected ? 'Collected ✅' : r.isApproved ? 'Approved (Awaiting Collection)' : 'Pending Review'}
                      </span>
                    </td>
                    <td>
                      {!r.isApproved ? (
                        r.date === todayStr ? (
                          <span style={{ fontSize: '0.8rem', color: '#f59e0b', fontWeight: 'bold' }}>Tracking in Progress</span>
                        ) : (
                          <button 
                            className="btn-approve" 
                            onClick={() => handleCreditPoints(r)}
                          >
                            Credit Points
                          </button>
                        )
                      ) : (
                        r.isCollected ? (
                          <span style={{ fontSize: '0.8rem', color: '#16a34a', fontWeight: 'bold' }}>Collected ✅</span>
                        ) : (
                          <span style={{ fontSize: '0.8rem', color: '#2563eb', fontWeight: 'bold' }}>Awaiting Collection ⏳</span>
                        )
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
