# Phase 3 Implementation Plan - Wheel Screener Android App

## Overview
Phase 3 focuses on building the Compose UI screens to display the scoring engine results and provide user interaction capabilities. This phase connects the Phase 2 scoring engine with the Phase 1 architecture to create a functional user interface.

## Current Status
- ✅ Phase 1: Clean Architecture/MVVM skeleton, Hilt DI, Room, MockMarketDataProvider, demo mode
- ✅ Phase 2: Scoring engine with pure functions, comprehensive unit tests (73 tests), backtest module, correlation analysis
- ✅ Phase 3: Compose UI screens (complete)

## Phase 3 Objectives

### Primary Goals
1. Build complete Compose UI screens for the Wheel Screener functionality
2. Connect the Phase 2 scoring engine to ViewModels
3. Display score components transparently to users
4. Implement candidate ranking and filtering
5. Add watchlist management UI
6. Create settings screen for strategy configuration

### Secondary Goals
1. Implement navigation between screens
2. Add manual scan trigger functionality
3. Display flags and exclusion reasons
4. Show confidence level indicators
5. Implement candidate detail inspection
6. Add export functionality

---

## Implementation Tasks

### Task 1: UI State Models

**Files to Create:**
- `app/src/main/java/com/wheelscreener/presentation/ui/state/CspRankingUiState.kt`
- `app/src/main/java/com/wheelscreener/presentation/ui/state/CcRankingUiState.kt`
- `app/src/main/java/com/wheelscreener/presentation/ui/state/CandidateDetailUiState.kt`
- `app/src/main/java/com/wheelscreener/presentation/ui/state/WatchlistUiState.kt`
- `app/src/main/java/com/wheelscreener/presentation/ui/state/SettingsUiState.kt`

**Components:**
- CSP ranking state with score components
- CC ranking state with position details
- Candidate detail state with full score breakdown
- Watchlist state with CRUD operations
- Settings state with strategy configuration

### Task 2: Enhanced ViewModels

**Files to Modify:**
- `app/src/main/java/com/wheelscreener/presentation/viewmodel/DashboardViewModel.kt`
- Create: `app/src/main/java/com/wheelscreener/presentation/viewmodel/CspRankingViewModel.kt`
- Create: `app/src/main/java/com/wheelscreener/presentation/viewmodel/CcRankingViewModel.kt`
- Create: `app/src/main/java/com/wheelscreener/presentation/viewmodel/CandidateDetailViewModel.kt`
- Create: `app/src/main/java/com/wheelscreener/presentation/viewmodel/WatchlistViewModel.kt`
- Create: `app/src/main/java/com/wheelscreener/presentation/viewmodel/SettingsViewModel.kt`

**Enhancements:**
- Connect scoring engine to ViewModels
- Implement real-time score recalculation
- Add candidate ranking logic
- Implement filtering and sorting
- Add watchlist CRUD operations
- Add strategy configuration management

### Task 3: Compose UI Screens

#### 3.1 Enhanced Dashboard Screen
**File:** `app/src/main/java/com/wheelscreener/presentation/ui/screens/DashboardScreen.kt`

**Enhancements:**
- Quick access to CSP/CC rankings
- Last scan summary with top candidates
- Market status with IV regime indicator
- Quick scan trigger with progress indicator
- Navigation to other screens

#### 3.2 CSP Rankings Screen
**File:** `app/src/main/java/com/wheelscreener/presentation/ui/screens/CspRankingScreen.kt`

**Components:**
- Ranked list of CSP candidates
- Score component breakdown (25+20+20+15+10+10)
- Filter by score range, delta, DTE
- Sort by score, liquidity, IV rank
- Candidate card with key metrics
- Pullback category indicator
- Technical/fundamental flags
- Tap for candidate detail

#### 3.3 CC Rankings Screen
**File:** `app/src/main/java/com/wheelscreener/presentation/ui/screens/CcRankingScreen.kt`

**Components:**
- Ranked list of CC candidates
- Position details (premium, break-even, max profit)
- Score component breakdown
- Assignment risk indicator
- Dividend risk flag
- Filter and sort options
- Tap for candidate detail

#### 3.4 Candidate Detail Screen
**File:** `app/src/main/java/com/wheelscreener/presentation/ui/screens/CandidateDetailScreen.kt`

**Components:**
- Full score breakdown with visual progress bars
- Individual component scores (Liquidity, IV, Pullback, etc.)
- All flags and exclusion reasons
- Confidence level indicator
- Technical analysis details (SMA relationships, trend)
- Fundamental analysis details (FCF, debt, market cap)
- IV analysis details (IV rank, regime, percentile)
- Pullback analysis details (20d/60d, type, quality)
- Option contract details (strike, DTE, delta, theta, vega)
- Historical performance preview (from backtest data)
- Add to watchlist button
- Export candidate data button

#### 3.5 Watchlist Management Screen
**File:** `app/src/main/java/com/wheelscreener/presentation/ui/screens/WatchlistScreen.kt`

**Components:**
- List of watchlist symbols with tags
- Add new symbol dialog
- Edit symbol tags
- Remove symbol from watchlist
- Activate/deactivate symbols
- Import watchlist from CSV
- Export watchlist to CSV
- Default watchlist reset option

#### 3.6 Settings Screen
**File:** `app/src/main/java/com/wheelscreener/presentation/ui/screens/SettingsScreen.kt`

**Components:**
- Strategy configuration (DTE range, delta ranges)
- Scoring weights (sliders for component weights)
- Liquidity thresholds (OI, volume, spread)
- IV rank target range
- Pullback range preferences
- Technical analysis preferences
- Fundamental analysis preferences
- Scan scheduling settings
- Notification preferences
- Data provider selection
- Export/import settings
- Reset to defaults option

### Task 4: Navigation Implementation

**File:** `app/src/main/java/com/wheelscreener/presentation/navigation/Navigation.kt`

**Components:**
- Navigation graph setup
- Screen routes definition
- Navigation animations
- Deep linking support
- Back navigation handling

### Task 5: UI Components Library

**Files to Create:**
- `app/src/main/java/com/wheelscreener/presentation/ui/components/ScoreCard.kt`
- `app/src/main/java/com/wheelscreener/presentation/ui/components/CandidateCard.kt`
- `app/src/main/java/com/wheelscreener/presentation/ui/components/FlagBadge.kt`
- `app/src/main/java/com/wheelscreener/presentation/ui/components/ProgressBar.kt`
- `app/src/main/java/com/wheelscreener/presentation/ui/components/FilterChip.kt`
- `app/src/main/java/com/wheelscreener/presentation/ui/components/ConfidenceIndicator.kt`

**Components:**
- Reusable score card with progress bars
- Candidate card with key metrics
- Flag badge for warnings/exclusions
- Progress bar for score components
- Filter chip for filtering options
- Confidence level indicator

### Task 6: Theme and Styling

**Files to Modify:**
- `app/src/main/java/com/wheelscreener/presentation/ui/theme/Color.kt`
- `app/src/main/java/com/wheelscreener/presentation/ui/theme/Theme.kt`
- `app/src/main/java/com/wheelscreener/presentation/ui/theme/Type.kt`

**Enhancements:**
- Color scheme for score levels (high/medium/low)
- Color scheme for flags (warning/error/info)
- Typography for data display
- Dark mode support
- Custom color palettes for scoring components

### Task 7: Error Handling and Loading States

**Components:**
- Loading indicators for scans
- Error messages for failed operations
- Empty states for no results
- Retry mechanisms
- Offline mode indicators

---

## Implementation Order

### Sprint 1: Foundation (Week 1)
1. UI state models creation
2. Enhanced ViewModels with scoring engine integration
3. Navigation setup
4. UI components library

### Sprint 2: Core Screens (Week 2)
1. Enhanced Dashboard screen
2. CSP Rankings screen
3. CC Rankings screen
4. Basic candidate detail screen

### Sprint 3: Advanced Features (Week 3)
1. Watchlist management screen
2. Settings screen
3. Advanced candidate detail screen
4. Theme and styling enhancements

### Sprint 4: Polish and Testing (Week 4)
1. Error handling and loading states
2. UI testing and refinement
3. Performance optimization
4. Documentation updates

---

## Technical Considerations

### State Management
- Use `StateFlow` for reactive UI updates
- Implement proper state restoration
- Handle configuration changes
- Use `rememberSaveable` for critical UI state

### Performance
- LazyColumn for large lists
- Proper composition and recomposition
- Image loading optimization (if needed)
- Memory management for large datasets

### Accessibility
- Content descriptions for screen readers
- Proper touch target sizes
- Semantic colors and labels
- Keyboard navigation support

### User Experience
- Smooth animations and transitions
- Clear visual hierarchy
- Intuitive navigation
- Consistent design patterns

---

## Integration Points

### With Phase 1 Architecture
- Use existing Room database for persistence
- Use existing Hilt DI setup
- Use existing repository pattern
- Use existing data providers

### With Phase 2 Scoring Engine
- Integrate `ScoringEngine` into ViewModels
- Display score components from `Score` data class
- Use flags from scoring components
- Implement confidence levels from scoring engine

### With Phase 4 (Future)
- Prepare for AlarmManager integration
- Prepare for notification system
- Prepare for market calendar integration

---

## Testing Strategy

### UI Testing
- Compose UI tests for critical screens
- Navigation flow tests
- User interaction tests
- Component rendering tests

### Integration Testing
- ViewModel to UI integration tests
- Scoring engine to ViewModel integration tests
- Database to UI integration tests

### User Acceptance Testing
- Manual testing of all screens
- User workflow testing
- Performance testing on real devices
- Accessibility testing

---

## Success Criteria

### Functional Requirements
- ✅ All screens render correctly
- ✅ Scoring engine integration works
- ✅ Score components display transparently
- ✅ Candidate ranking functions correctly
- ✅ Watchlist management works
- ✅ Settings configuration persists
- ✅ Navigation flows work correctly

### Non-Functional Requirements
- ✅ App performs smoothly on target devices
- ✅ Memory usage is acceptable
- ✅ UI is responsive and intuitive
- ✅ Accessibility requirements are met
- ✅ Error handling is robust

### User Experience Requirements
- ✅ Clear visual hierarchy
- ✅ Intuitive navigation
- ✅ Consistent design patterns
- ✅ Helpful error messages
- ✅ Informative loading states

---

## Deliverables

### Code Files
- UI state models (5 files)
- Enhanced ViewModels (6 files)
- Compose UI screens (6 files)
- Navigation setup (1 file)
- UI components library (6 files)
- Theme enhancements (3 files)

### Documentation
- Phase 3 summary document
- UI component documentation
- Navigation flow documentation
- User guide updates

### Testing
- UI test suite
- Integration test suite
- User acceptance test results

---

## Dependencies

### Android SDK
- minSdk: 29
- targetSdk: 34
- compileSdk: 34

### Key Libraries
- Jetpack Compose (already configured)
- Navigation Compose (already configured)
- Hilt (already configured)
- Room (already configured)
- Coroutines (already configured)

---

## Risks and Mitigations

### Risk: Scoring Engine Performance
**Mitigation:** Implement background processing for scoring operations, use proper Flow-based updates

### Risk: UI Complexity
**Mitigation:** Break down complex screens into smaller components, use composable functions effectively

### Risk: State Management Complexity
**Mitigation:** Use clear state management patterns, implement proper state restoration

### Risk: Integration Issues
**Mitigation:** Thorough integration testing, incremental integration approach

---

## Timeline

### Week 1: Foundation
- UI state models and ViewModels
- Navigation setup
- UI components library

### Week 2: Core Screens
- Enhanced Dashboard
- CSP/CC Rankings
- Basic Candidate Detail

### Week 3: Advanced Features
- Watchlist Management
- Settings
- Advanced Candidate Detail

### Week 4: Polish and Testing
- Error handling
- UI testing
- Performance optimization
- Documentation

---

## Phase 3 Sign-off Criteria

- ✅ All UI screens implemented and functional
- ✅ Scoring engine integrated with ViewModels
- ✅ Score components displayed transparently
- ✅ Navigation flows work correctly
- ✅ Watchlist management functional
- ✅ Settings configuration working
- ✅ UI tests passing
- ✅ Performance acceptable
- ✅ Documentation complete
- ✅ Ready for Phase 4 (Scheduling)

---

## Next Steps After Phase 3

1. **Phase 4: Scheduling**
   - AlarmManager exact alarms
   - WorkManager fallback
   - Market calendar integration
   - Notification system

2. **Phase 5: Real Data Provider**
   - ORATS API integration (or alternative)
   - API key management
   - Error handling and rate limiting
   - Data validation

3. **Phase 6: Paper Trading**
   - Position ledger
   - CSV export
   - P&L tracking
   - Roll/assignment reminders

---

**Phase 3 Implementation Plan**
**Start Date:** August 25, 2026
**Estimated Duration:** 4 weeks
**Status:** Complete
