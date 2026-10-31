// The HR Bots "AI assistant" visual: a glowing rose-to-blue orb with a slowly
// turning gold ring. size="lg" adds floating conversation cards (login page);
// size="sm" is the orb alone (dashboard greeting). Purely decorative.

export default function AiHrHero({ size = 'lg' }) {
  return (
    <div className={`ai-hero ai-hero-${size}`} aria-hidden="true">
      <svg className="ai-orb" viewBox="0 0 400 400" role="presentation">
        <defs>
          <radialGradient id="orbFill" cx="38%" cy="32%" r="75%">
            <stop offset="0%" stopColor="#FFE9F1" />
            <stop offset="30%" stopColor="#F2A3C2" />
            <stop offset="62%" stopColor="#B85A8C" />
            <stop offset="100%" stopColor="#4B6FD6" />
          </radialGradient>
          <radialGradient id="orbGlow" cx="50%" cy="50%" r="50%">
            <stop offset="55%" stopColor="#F2A3C2" stopOpacity="0.55" />
            <stop offset="100%" stopColor="#F2A3C2" stopOpacity="0" />
          </radialGradient>
          <linearGradient id="goldRing" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#F3D58A" />
            <stop offset="50%" stopColor="#B98A2E" />
            <stop offset="100%" stopColor="#F3D58A" />
          </linearGradient>
          <radialGradient id="shine" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="#FFFFFF" stopOpacity="0.9" />
            <stop offset="100%" stopColor="#FFFFFF" stopOpacity="0" />
          </radialGradient>
        </defs>

        {/* soft halo */}
        <circle cx="200" cy="200" r="190" fill="url(#orbGlow)" />

        {/* gold orbit ring with three jewels, turning slowly */}
        <g className="ai-ring">
          <ellipse cx="200" cy="200" rx="176" ry="176" fill="none" stroke="url(#goldRing)"
            strokeWidth="2" strokeDasharray="2 10" strokeLinecap="round" />
          <circle cx="200" cy="24" r="7" fill="#C99A3B" />
          <circle cx="352" cy="288" r="5" fill="#4B6FD6" />
          <circle cx="48" cy="288" r="5" fill="#D86A97" />
        </g>

        {/* the orb */}
        <g className="ai-core">
          <circle cx="200" cy="200" r="128" fill="url(#orbFill)" />
          <ellipse cx="160" cy="146" rx="56" ry="34" fill="url(#shine)" opacity="0.75" />

          {/* listening waves */}
          <circle cx="200" cy="208" r="64" fill="none" stroke="#FFFFFF" strokeOpacity="0.35" strokeWidth="1.5" />
          <circle cx="200" cy="208" r="88" fill="none" stroke="#FFFFFF" strokeOpacity="0.18" strokeWidth="1.5" />

          {/* four-point AI spark in gold */}
          <path
            d="M200 160 C206 192 216 202 248 208 C216 214 206 224 200 256 C194 224 184 214 152 208 C184 202 194 192 200 160 Z"
            fill="url(#goldRing)"
          />
          <path
            d="M262 150 C264 160 267 163 277 165 C267 167 264 170 262 180 C260 170 257 167 247 165 C257 163 260 160 262 150 Z"
            fill="#FFF4D6"
          />
        </g>
      </svg>

      {size === 'lg' && (
        <>
          <div className="ai-card ai-card-question">How many leave days do I have left?</div>
          <div className="ai-card ai-card-answer">
            You have 15 days left. Enjoy your break!
            <span className="ai-card-meta">Based on your HR record</span>
          </div>
          <div className="ai-card ai-card-status">
            <span className="ai-tick">✓</span> Leave approved, 3–7 Nov
          </div>
        </>
      )}
    </div>
  );
}