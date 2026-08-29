# Wheel Screener Android App - Implementation Plan

## Data Provider Comparison (Free Tools Only)

Based on research for free-only tools, here are the best options data providers for the Wheel Screener app:

### 1. FlashAlpha API (Recommended for Free Tier)

**Cost:**
- ✅ **Free tier**: 5 requests/day, no credit card required
- Basic plan: $63/month (if upgrade needed)

**IV Rank/Percentile:**
- ✅ **Computed for you** - IV rank provided in stock summary
- Fields: `iv_rank`, ATM IV, volatility surface data
- BSM Greeks calculator included

**Greeks Source:**
- ✅ 15 BSM Greeks (1st/2nd/3rd order) - delta, gamma, theta, vega, rho, vanna, charm, etc.
- Server-side computation, no local math required
- SVI volatility surfaces

**Corporate Event/Earnings Calendar:**
- ❓ Limited - Not prominently featured in free tier documentation
- May need supplemental source for earnings dates

**Rate Limits:**
- Free tier: 5 requests/day (resets at midnight)
- Basic plan: Higher limits

**Pros:**
- Completely free with no credit card required
- IV rank computed out-of-the-box
- Comprehensive Greeks (15 different types)
- GEX data and gamma exposure
- Volatility surface data
- Official SDKs for multiple languages
- No trial period - permanent free tier

**Cons:**
- Only 5 requests/day on free tier (very limited for scanning)
- Earnings calendar not clearly available in free tier
- Rate limiting may restrict daily scanning functionality

**API Gaps/Approximations Needed:**
- Earnings calendar may need supplementation from other free sources
- 5 calls/day insufficient for full watchlist scanning (14+ symbols)
- Would need upgrade for production use

---

### 2. MagiQa Options API (Best Free Tier for This Use Case)

**Cost:**
- ✅ **Free tier**: 500 requests/month, 2 req/s, no credit card required
- Developer plan: $49/month (if upgrade needed)

**IV Rank/Percentile:**
- ❌ **Must be built** - IV rank available in paid plans only
- Free tier includes IV data but not computed rank/percentile
- Basic options data available

**Greeks Source:**
- ✅ Greeks included - Delta, Gamma, Theta, Vega available
- Covered calls & naked puts supported
- Options chains with Greeks

**Corporate Event/Earnings Calendar:**
- ❓ Limited - Not prominently featured in documentation
- May need supplemental source

**Rate Limits:**
- Free tier: 500 requests/month, 2 req/s
- Developer plan: 10,000 requests/month, 10 req/s

**Pros:**
- 500 requests/month is sufficient for daily scanning (14 symbols = ~420 requests/month)
- Options chains with Greeks included
- Stock quotes, history & fundamentals for 20 symbols
- No credit card required for free tier
- Research use allowed
- Python SDK available

**Cons:**
- IV rank not available in free tier (must compute)
- Limited to 20 symbols in free tier
- 500 requests/month may be tight for full functionality
- Earnings calendar not clearly available

**API Gaps/Approximations Needed:**
- IV rank/percentile calculation from historical IV data
- Earnings calendar supplementation
- Symbol limit may require watchlist management

---

### 3. Strasmore API (No Key Required)

**Cost:**
- ✅ **Completely free**: No API key required, no signup required
- Truly open API for AI agents and developers

**IV Rank/Percentile:**
- ✅ **Available** - `iv_leaders` endpoint provides IV data
- End-of-day implied volatility leaders
- IV averages per underlying

**Greeks Source:**
- ✅ **Available** - End-of-day Greeks and implied volatility
- Options data includes Greeks
- Most active options with average IV

**Corporate Event/Earnings Calendar:**
- ✅ **Available** - `upcoming_ex_dividends` endpoint
- Market holidays data available
- Corporate actions data

**Rate Limits:**
- Not specified (likely reasonable for free API)
- Edge-cached data

**Pros:**
- Completely free with no restrictions
- No API key required
- IV leaders and options data
- Upcoming dividends and market holidays
- Open to AI agents
- SQL queries transparent in responses

**Cons:**
- Limited curated endpoints (not full API)
- Data is end-of-day, not real-time
- May not have full options chain coverage
- Limited customization (curated queries only)

**API Gaps/Approximations Needed:**
- May not provide full option chains for all symbols
- Limited to curated endpoints
- Real-time data not available

---

## Recommendation for Free-Only Implementation

**Primary Approach: MockMarketDataProvider (Development + Production)**

**Rationale:**
1. **Zero cost** - No API fees or rate limits
2. **Full control** - Complete data generation and customization
3. **Unlimited requests** - No rate limiting concerns
4. **Educational value** - Users understand it's synthetic data
5. **Scoring engine focus** - The core value is in the scoring logic, not data sourcing

**Supplemental Real Data (Optional):**
- **Strasmore** for market holidays and dividends (no key required)
- **FlashAlpha** free tier for limited real IV data (5 calls/day for validation)
- **MagiQa** free tier if users want some real data (500 requests/month)

**Implementation Strategy:**
1. Use MockMarketDataProvider as primary data source
2. Build robust IV rank calculation in scoring engine
3. Implement earnings calendar from multiple free sources
4. Make provider swapping easy for future upgrades
5. Clearly label data as "synthetic/demo" in UI

**Cost: $0 total**

**Trade-offs:**
- No real market data (synthetic only)
- Users must understand limitations
- Scoring model validation more challenging
- Educational/tool focus rather than trading focus

---

## Implementation Approach

### Phase 1: Foundation (Complete)
- Clean Architecture/MVVM skeleton
- Hilt DI setup
- Room database schema
- MockMarketDataProvider
- Demo mode with 12+ symbols
- Basic UI skeleton

### Phase 2: Scoring Engine (Complete)
- Pure function scoring components
- Comprehensive unit tests
- Backtest module with synthetic data
- Score correlation analysis (pullback vs trend)

### Phase 3: Compose UI (Complete)
- Dashboard screen
- CSP/CC ranking screens
- Candidate detail screen
- Watchlist management
- Settings screen

### Phase 4: Scheduling (Complete)
- AlarmManager exact alarms
- WorkManager fallback
- Market calendar integration
- Notification system

### Phase 5: Real Data Provider (Complete)
- ORATS API integration
- API key management
- Error handling and rate limiting
- Data validation

### Phase 6: Paper Trading (Complete)
- Position ledger
- CSV export
- P&L tracking
- Roll/assignment reminders

---

## Technical Architecture

### Clean Architecture Layers

**Domain Layer:**
- Entities: OptionContract, Underlying, Score, StrategyConfig
- Use Cases: ScanMarket, ScoreCandidates, FilterOptions
- Repository Interfaces

**Data Layer:**
- Repository Implementations
- MarketDataProvider (interface + implementations)
- Room Database
- Retrofit API services
- Local data sources

**Presentation Layer:**
- Compose UI Screens
- ViewModels
- UI State models
- Navigation

### Key Design Decisions

1. **Single Activity, Multiple Compose Screens** - Modern navigation pattern
2. **Repository Pattern** - Abstract data sources for testability
3. **Strategy Config Data Class** - Versioned, serializable configuration
4. **Pure Function Scoring** - Testable, transparent score components
5. **Flow-based Reactive Updates** - Real-time UI updates
6. **Worker for Scans** - Background processing for market scans

---

## Known Limitations (v1)

1. **Single Daily Scan** - Not real-time, data stale between 10 AM scan and user action
2. **No Roll/Assignment Automation** - Manual review only
3. **Synthetic Backtest Data** - Real historical data not integrated yet
4. **US Equities Only** - No international markets
5. **No Brokerage Integration** - Paper trading only
6. **Approximate Scheduling** - Dependent on device and OS constraints
7. **Limited Corporate Actions** - Focus on earnings, other events may be incomplete

---

## Current Status

All six planned phases are implemented. The app remains usable in mock/demo mode by default; add an ORATS key in ignored `local.properties` to enable the real-data provider.
