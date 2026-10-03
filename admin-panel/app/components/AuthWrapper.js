'use client';

import { useState, useEffect } from 'react';
import { db, auth } from '../firebase';
import { doc, getDoc } from 'firebase/firestore';
import { signInWithPopup, GoogleAuthProvider, onAuthStateChanged, signOut } from 'firebase/auth';
import Link from 'next/link';
import { usePathname } from 'next/navigation';

export default function AuthWrapper({ children }) {
  const [user, setUser] = useState(null);
  const [isAdmin, setIsAdmin] = useState(false);
  const [authLoading, setAuthLoading] = useState(true);
  const pathname = usePathname();

  useEffect(() => {
    const unsubscribeAuth = onAuthStateChanged(auth, async (user) => {
      setAuthLoading(true);
      if (user) {
        setUser(user);
        // Check if user is admin in Firestore or matches the primary admin email
        const userDoc = await getDoc(doc(db, "users", user.uid));
        const isUserAdmin = userDoc.exists() && userDoc.data().isAdmin === true;
        const isPrimaryAdmin = user.email === 'luckykaseqq@gmail.com' || user.email === 'reelnearn@gmail.com';
        
        setIsAdmin(isUserAdmin || isPrimaryAdmin);
      } else {
        setUser(null);
        setIsAdmin(false);
      }
      setAuthLoading(false);
    });

    return () => unsubscribeAuth();
  }, []);

  const handleLogin = async () => {
    const provider = new GoogleAuthProvider();
    try {
      await signInWithPopup(auth, provider);
    } catch (error) {
      alert("Login failed: " + error.message);
    }
  };

  const handleLogout = async () => {
    await signOut(auth);
  };

  // Public routes â€” skip auth entirely
  const publicRoutes = ['/privacy'];
  if (publicRoutes.includes(pathname)) {
    return <>{children}</>;
  }

  if (authLoading) {
    return <div className="loading-state" style={{ textAlign: 'center', padding: '5rem', color: '#64748b' }}>Authenticating...</div>;
  }

  if (!user) {
    return (
      <div className="login-container" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', gap: '2rem', padding: '2rem', background: '#f8fafc' }}>
        <h1 style={{ color: '#0f172a', margin: 0 }}>Reel n Earn Admin</h1>
        <p style={{ color: '#64748b', margin: 0 }}>Please sign in with an authorized admin account.</p>
        <button 
          onClick={handleLogin}
          style={{ 
            padding: '1rem 2rem', 
            background: '#6366f1', 
            color: 'white', 
            border: 'none', 
            borderRadius: '8px', 
            fontWeight: 'bold', 
            cursor: 'pointer',
            fontSize: '1.1rem',
            boxShadow: '0 4px 10px rgba(99, 102, 241, 0.3)'
          }}
        >
          Sign in with Google
        </button>
      </div>
    );
  }

  if (!isAdmin) {
    return (
      <div className="login-container" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', gap: '2rem', padding: '2rem', background: '#f8fafc' }}>
        <h1 style={{ color: '#ef4444', margin: 0 }}>Access Denied</h1>
        <p style={{ color: '#64748b', textAlign: 'center', margin: 0 }}>Account <strong>{user.email}</strong> is not authorized.<br/>Please contact the system administrator.</p>
        <button onClick={handleLogout} style={{ color: '#3b82f6', background: 'none', border: 'none', cursor: 'pointer', textDecoration: 'underline', fontSize: '1rem' }}>
          Sign out and try another account
        </button>
      </div>
    );
  }

  return (
    <div className="admin-container" style={{ display: 'flex', minHeight: '100vh' }}>
      <aside className="sidebar" style={{ width: '250px', background: 'white', borderRight: '1px solid #e2e8f0', display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: '1.5rem' }}>
          <h2 style={{ margin: '0 0 0.5rem 0', fontSize: '1.25rem', color: '#0f172a' }}>Reel n Earn</h2>
          <div style={{ fontSize: '0.8rem', color: '#64748b', wordBreak: 'break-all' }}>{user.email}</div>
        </div>
        
        <nav style={{ display: 'flex', flexDirection: 'column', padding: '0 1rem', gap: '0.5rem' }}>
          {[
            { name: 'Overview', path: '/admin' },
            { name: 'Daily Usage', path: '/usage' },
            { name: 'Pending Requests', path: '/requests' },
            { name: 'Reward History', path: '/history' }
          ].map(link => {
            const isActive = pathname === link.path;
            return (
              <Link 
                key={link.path} 
                href={link.path} 
                style={{ 
                  padding: '0.75rem 1rem', 
                  borderRadius: '8px', 
                  color: isActive ? '#6366f1' : '#334155', 
                  background: isActive ? '#eeefee' : 'transparent',
                  textDecoration: 'none', 
                  fontWeight: isActive ? 'bold' : '500' 
                }}
              >
                {link.name}
              </Link>
            )
          })}
        </nav>
        
        <div style={{ marginTop: 'auto', padding: '1.5rem' }}>
          <button onClick={handleLogout} style={{ width: '100%', padding: '0.75rem', background: 'rgba(239, 68, 68, 0.1)', color: '#ef4444', border: '1px solid #ef4444', borderRadius: '8px', cursor: 'pointer', fontWeight: 'bold' }}>
            Logout
          </button>
        </div>
      </aside>
      <main className="content" style={{ flex: 1, overflowY: 'auto' }}>
        {children}
      </main>
    </div>
  );
}

