import './globals.css';
import AuthWrapper from './components/AuthWrapper';

export const metadata = {
  title: 'Reel n Earn Admin',
  description: 'Admin panel for Reel n Earn app',
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body style={{ margin: 0, padding: 0 }}>
        <AuthWrapper>
          {children}
        </AuthWrapper>
      </body>
    </html>
  );
}

