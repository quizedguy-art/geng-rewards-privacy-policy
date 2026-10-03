import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";
import { getAuth } from "firebase/auth";

// Reel n Earn web app Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyDy0nEMUD1t5XDSFT3DAjepqwvyNbJMCqA",
  authDomain: "geng-money.firebaseapp.com",
  projectId: "geng-money",
  storageBucket: "geng-money.firebasestorage.app",
  messagingSenderId: "848825463828",
  appId: "1:848825463828:web:e11d2b413c8daa692f6301"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
const auth = getAuth(app);

export { db, auth };

