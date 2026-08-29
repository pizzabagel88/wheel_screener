package com.wheelscreener.domain.scoring

import com.wheelscreener.data.remote.CorporateEvent
import com.wheelscreener.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Main scoring engine that combines all scoring components
 * All functions are pure and deterministic
 */
object ScoringEngine {
    
    /**
     * Score a CSP candidate comprehensively
     */
    fun scoreCSPCandidate(
        contract: OptionContract,
        underlying: Underlying,
        events: List<CorporateEvent>,
        config: StrategyConfig,
        currentTime: Instant = Clock.System.now()
    ): CSPScoringResult {
        val dte = DteSelector.calculateDTE(currentTime, contract.expiration)
        val absoluteDelta = DeltaSelector.getAbsoluteDelta(contract.delta)
        val isCore = DeltaSelector.classifySymbol(
            underlying.marketCap,
            contract.ivRank,
            config
        )
        
        // Calculate individual component scores
        val liquidityScore = LiquidityFilter.scoreLiquidity(
            contract, underlying, dte, config
        )
        
        val ivScore = IVAnalyzer.scoreIVOpportunity(
            contract, config, hasEventRisk = false
        )
        
        val pullbackScore = PullbackAnalyzer.scorePullback(
            underlying, config
        )
        
        val fundamentalScore = FundamentalAnalyzer.scoreFundamentals(
            underlying, config
        )
        
        val technicalScore = TechnicalAnalyzer.scoreTechnical(
            underlying, config
        )
        
        val diversificationScore = 10.0 // Placeholder - will be implemented with portfolio data
        
        // Calculate composite score
        val compositeScore = ScoreComponents.fromComponents(
            liquidityScore = liquidityScore,
            ivScore = ivScore,
            pullbackScore = pullbackScore,
            fundamentalScore = fundamentalScore,
            technicalScore = technicalScore,
            diversificationScore = diversificationScore
        )
        
        // Collect all flags
        val flags = mutableListOf<CandidateFlag>()
        
        // Liquidity flags
        flags.addAll(LiquidityFilter.checkLiquidity(
            contract, underlying, dte, config
        ))
        
        // Event flags
        flags.addAll(EventExclusion.getEventFlags(
            contract.expiration, events
        ))
        
        // Pullback flags
        flags.addAll(PullbackAnalyzer.getPullbackFlags(
            underlying, config
        ))
        
        // Technical flags
        flags.addAll(TechnicalAnalyzer.getTrendFlags(
            underlying, config
        ))
        
        // Fundamental flags
        flags.addAll(FundamentalAnalyzer.getFundamentalFlags(
            underlying, config
        ))
        
        // IV flags
        flags.addAll(IVAnalyzer.getIVFlags(
            contract, config
        ))
        
        // Determine confidence level
        val confidence = determineConfidence(
            contract, underlying, events, flags
        )
        
        // Check for exclusion
        val exclusionReason = getExclusionReason(
            contract, underlying, events, flags, config
        )
        
        return CSPScoringResult(
            contract = contract,
            underlying = underlying,
            dte = dte,
            absoluteDelta = absoluteDelta,
            isCore = isCore,
            scoreComponents = compositeScore,
            flags = flags,
            exclusionReason = exclusionReason,
            confidence = confidence
        )
    }
    
    /**
     * Score a CC candidate comprehensively
     */
    fun scoreCCCandidate(
        contract: OptionContract,
        underlying: Underlying,
        events: List<CorporateEvent>,
        shareCount: Int,
        costBasis: Double,
        config: StrategyConfig,
        currentTime: Instant = Clock.System.now()
    ): CCScoringResult {
        val dte = DteSelector.calculateDTE(currentTime, contract.expiration)
        val absoluteDelta = DeltaSelector.getAbsoluteDelta(contract.delta)
        val isCore = DeltaSelector.classifySymbol(
            underlying.marketCap,
            contract.ivRank,
            config
        )
        
        // Calculate individual component scores (same as CSP for now)
        val liquidityScore = LiquidityFilter.scoreLiquidity(
            contract, underlying, dte, config
        )
        
        val ivScore = IVAnalyzer.scoreIVOpportunity(
            contract, config, hasEventRisk = false
        )
        
        val pullbackScore = PullbackAnalyzer.scorePullback(
            underlying, config
        )
        
        val fundamentalScore = FundamentalAnalyzer.scoreFundamentals(
            underlying, config
        )
        
        val technicalScore = TechnicalAnalyzer.scoreTechnical(
            underlying, config
        )
        
        val diversificationScore = 10.0 // Placeholder
        
        val compositeScore = ScoreComponents.fromComponents(
            liquidityScore = liquidityScore,
            ivScore = ivScore,
            pullbackScore = pullbackScore,
            fundamentalScore = fundamentalScore,
            technicalScore = technicalScore,
            diversificationScore = diversificationScore
        )
        
        // Collect flags (similar to CSP)
        val flags = mutableListOf<CandidateFlag>()
        flags.addAll(LiquidityFilter.checkLiquidity(
            contract, underlying, dte, config
        ))
        flags.addAll(EventExclusion.getEventFlags(
            contract.expiration, events
        ))
        flags.addAll(PullbackAnalyzer.getPullbackFlags(
            underlying, config
        ))
        flags.addAll(TechnicalAnalyzer.getTrendFlags(
            underlying, config
        ))
        flags.addAll(FundamentalAnalyzer.getFundamentalFlags(
            underlying, config
        ))
        flags.addAll(IVAnalyzer.getIVFlags(
            contract, config
        ))
        
        val confidence = determineConfidence(
            contract, underlying, events, flags
        )
        
        val exclusionReason = getExclusionReason(
            contract, underlying, events, flags, config
        )
        
        // CC-specific calculations
        val effectiveExitPrice = contract.strike + contract.bid
        val returnIfCalled = (effectiveExitPrice - costBasis) / costBasis
        val isIncomeFirst = contract.strike < costBasis
        
        return CCScoringResult(
            contract = contract,
            underlying = underlying,
            dte = dte,
            absoluteDelta = absoluteDelta,
            isCore = isCore,
            shareCount = shareCount,
            costBasis = costBasis,
            effectiveExitPrice = effectiveExitPrice,
            returnIfCalled = returnIfCalled,
            isIncomeFirst = isIncomeFirst,
            scoreComponents = compositeScore,
            flags = flags,
            exclusionReason = exclusionReason,
            confidence = confidence
        )
    }
    
    /**
     * Determine confidence level based on data quality
     */
    private fun determineConfidence(
        contract: OptionContract,
        underlying: Underlying,
        events: List<CorporateEvent>,
        flags: List<CandidateFlag>
    ): DataConfidence {
        var missingCriticalData = 0
        var missingNonCriticalData = 0
        
        // Critical data
        if (contract.delta == null) missingCriticalData++
        if (contract.iv == null) missingCriticalData++
        if (underlying.sma200 == null) missingCriticalData++
        
        // Non-critical data
        if (contract.ivRank == null) missingNonCriticalData++
        if (!EventExclusion.hasSufficientEventData(events)) missingNonCriticalData++
        if (underlying.freeCashFlowTTM == null) missingNonCriticalData++
        
        return when {
            missingCriticalData > 0 -> DataConfidence.LOW
            missingNonCriticalData > 1 -> DataConfidence.LOW
            missingNonCriticalData == 1 -> DataConfidence.MEDIUM
            flags.contains(CandidateFlag.STALE_QUOTE) -> DataConfidence.LOW
            else -> DataConfidence.HIGH
        }
    }
    
    /**
     * Get exclusion reason if candidate should be excluded
     */
    private fun getExclusionReason(
        contract: OptionContract,
        underlying: Underlying,
        events: List<CorporateEvent>,
        flags: List<CandidateFlag>,
        config: StrategyConfig
    ): String? {
        // Event-based exclusion (hard exclusion)
        val eventExclusion = EventExclusion.getEventExclusionReason(
            contract.expiration, events
        )
        if (eventExclusion != null) return eventExclusion
        
        // Liquidity-based exclusion (hard exclusion)
        if (flags.contains(CandidateFlag.LOW_OPEN_INTEREST) ||
            flags.contains(CandidateFlag.WIDE_SPREAD)) {
            return "Insufficient liquidity"
        }
        
        // Fundamental-based exclusion (hard exclusion)
        if (flags.contains(CandidateFlag.BELOW_MARKET_CAP_THRESHOLD)) {
            return "Below market cap minimum"
        }
        
        // Technical breakdown (soft exclusion - can be overridden)
        if (flags.contains(CandidateFlag.POSSIBLE_TREND_BREAKDOWN)) {
            return "Possible trend breakdown (can be overridden)"
        }
        
        return null
    }
    
    /**
     * Rank candidates by composite score
     */
    fun rankCSPCandidates(results: List<CSPScoringResult>): List<CSPScoringResult> {
        return results
            .filter { it.exclusionReason == null }
            .sortedByDescending { it.scoreComponents.compositeScore }
    }
    
    /**
     * Rank CC candidates by composite score
     */
    fun rankCCCandidates(results: List<CCScoringResult>): List<CCScoringResult> {
        return results
            .filter { it.exclusionReason == null }
            .sortedByDescending { it.scoreComponents.compositeScore }
    }
}

data class CSPScoringResult(
    val contract: OptionContract,
    val underlying: Underlying,
    val dte: Int,
    val absoluteDelta: Double,
    val isCore: Boolean,
    val scoreComponents: ScoreComponents,
    val flags: List<CandidateFlag>,
    val exclusionReason: String?,
    val confidence: DataConfidence
)

data class CCScoringResult(
    val contract: OptionContract,
    val underlying: Underlying,
    val dte: Int,
    val absoluteDelta: Double,
    val isCore: Boolean,
    val shareCount: Int,
    val costBasis: Double,
    val effectiveExitPrice: Double,
    val returnIfCalled: Double,
    val isIncomeFirst: Boolean,
    val scoreComponents: ScoreComponents,
    val flags: List<CandidateFlag>,
    val exclusionReason: String?,
    val confidence: DataConfidence
)