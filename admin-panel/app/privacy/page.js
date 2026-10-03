import './privacy.css';

export const metadata = {
  title: 'Privacy Policy â€” Reel n Earn',
  description: 'Reel n Earn Official Privacy Policy â€” Google Play Developer Policy Compliant.',
};

export default function PrivacyPage() {
  return (
    <div className="privacy-page">
      {/* Header */}
      <div className="privacy-header">
        <div className="privacy-header-inner">
          <div className="privacy-logo">ðŸŽ</div>
          <h1>Privacy Policy</h1>
          <p className="privacy-subtitle">Reel n Earn (Package: com.quizedguy.Reel n Earn)</p>
          <span className="privacy-badge">Google Play Compliant | Effective: May 18, 2026</span>
        </div>
      </div>

      {/* Content */}
      <div className="privacy-container">

        {/* Intro */}
        <div className="privacy-intro">
          <p>
            At <strong>Reel n Earn</strong>, your privacy is our highest priority. This policy explains
            what data we collect, why we collect it, how it is used, and your rights regarding your personal data.
          </p>
        </div>

        {/* Quick Summary Cards */}
        <div className="summary-grid">
          <div className="summary-card safe">
            <span className="summary-icon">ðŸ”’</span>
            <p>No personal app activity or browsing tracked</p>
          </div>
          <div className="summary-card safe">
            <span className="summary-icon">ðŸ“µ</span>
            <p>Your data is never sold to third parties</p>
          </div>
          <div className="summary-card safe">
            <span className="summary-icon">âœ…</span>
            <p>Encrypted data transfer via HTTPS/TLS</p>
          </div>
          <div className="summary-card warning">
            <span className="summary-icon">ðŸ“¢</span>
            <p>Google AdMob & Firebase integration</p>
          </div>
        </div>

        {/* Section 1 */}
        <section className="privacy-section">
          <div className="section-header">
            <span className="section-number">01</span>
            <h2>Usage Access Permission (PACKAGE_USAGE_STATS) Disclosure</h2>
          </div>
          <div className="section-body">
            <div className="info-block">
              <h3>ðŸ“± Why Usage Permission is Requested</h3>
              <p>
                Reel n Earn requests Android's <code>PACKAGE_USAGE_STATS</code> permission strictly to measure your
                <strong> aggregate daily screen time duration</strong> in milliseconds for daily wellness goals.
              </p>
              <ul>
                <li>Only total daily duration is processed to determine goal progress.</li>
                <li>We <strong>NEVER</strong> track, view, collect, or store individual app names, browsing histories, or personal app content.</li>
              </ul>
            </div>
          </div>
        </section>

        {/* Section 2 */}
        <section className="privacy-section">
          <div className="section-header">
            <span className="section-number">02</span>
            <h2>Information We Collect</h2>
          </div>
          <div className="section-body">
            <div className="info-block">
              <h3>ðŸ‘¤ Profile & Financial Information</h3>
              <ul>
                <li><strong>Account Data:</strong> Display Name, Email address, and Firebase User ID (UID).</li>
                <li><strong>Payment Data:</strong> Voluntary UPI VPAs or Email addresses provided for gift card fulfillment. We do not collect credit cards or bank login credentials.</li>
                <li><strong>Referral Data:</strong> 6-character referral code, referral links, and awarded bonus points (500 Pts).</li>
                <li><strong>Device & Ad Identifiers:</strong> Advertising ID (AAID), device model, OS version collected via Google AdMob for ad delivery and fraud prevention.</li>
              </ul>
            </div>
          </div>
        </section>

        {/* Section 3 */}
        <section className="privacy-section">
          <div className="section-header">
            <span className="section-number">03</span>
            <h2>Account & Data Deletion Policy</h2>
          </div>
          <div className="section-body">
            <div className="info-block">
              <p>
                You have the right to request deletion of your account and all associated data. Send an email to
                <strong> reelnearn@gmail.com</strong> with the subject line <code>"Account Deletion Request"</code>. Your data will be permanently removed within 7 business days.
              </p>
            </div>
          </div>
        </section>

        {/* Section 4 */}
        <section className="privacy-section">
          <div className="section-header">
            <span className="section-number">04</span>
            <h2>Contact Us</h2>
          </div>
          <div className="section-body">
            <div className="info-block">
              <p>For questions or privacy requests, contact our Developer Controller:</p>
              <p><strong>Email:</strong> reelnearn@gmail.com</p>
              <p><strong>App:</strong> Reel n Earn (<code>com.quizedguy.Reel n Earn</code>)</p>
            </div>
          </div>
        </section>

      </div>
    </div>
  );
}

