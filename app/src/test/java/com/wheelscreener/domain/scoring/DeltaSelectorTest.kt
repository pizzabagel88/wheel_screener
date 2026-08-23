package com.wheelscreener.domain.scoring

import com.wheelscreener.domain.model.StrategyConfig
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DeltaSelectorTest {
    
    private lateinit var config: StrategyConfig
    
    @Before
    fun setup() {
        config = StrategyConfig.default()
    }
    
    @Test
    fun `test absolute delta calculation`() {
        assertEquals(0.25, DeltaSelector.getAbsoluteDelta(0.25), 0.01)
        assertEquals(0.25, DeltaSelector.getAbsoluteDelta(-0.25), 0.01)
        assertEquals(0.0, DeltaSelector.getAbsoluteDelta(null), 0.01)
    }
    
    @Test
    fun `test CSP delta range check for core symbols`() {
        assertTrue(DeltaSelector.isCSPDeltaInRange(0.25, true, config))
        assertTrue(DeltaSelector.isCSPDeltaInRange(0.20, true, config))
        assertTrue(DeltaSelector.isCSPDeltaInRange(0.30, true, config))
        assertFalse(DeltaSelector.isCSPDeltaInRange(0.15, true, config))
        assertFalse(DeltaSelector.isCSPDeltaInRange(0.35, true, config))
    }
    
    @Test
    fun `test CSP delta range check for satellite symbols`() {
        assertTrue(DeltaSelector.isCSPDeltaInRange(0.20, false, config))
        assertTrue(DeltaSelector.isCSPDeltaInRange(0.15, false, config))
        assertTrue(DeltaSelector.isCSPDeltaInRange(0.25, false, config))
        assertFalse(DeltaSelector.isCSPDeltaInRange(0.10, false, config))
        assertFalse(DeltaSelector.isCSPDeltaInRange(0.30, false, config))
    }
    
    @Test
    fun `test CC delta range check for core symbols`() {
        assertTrue(DeltaSelector.isCCDeltaInRange(0.25, true, config))
        assertTrue(DeltaSelector.isCCDeltaInRange(0.20, true, config))
        assertTrue(DeltaSelector.isCCDeltaInRange(0.30, true, config))
        assertFalse(DeltaSelector.isCCDeltaInRange(0.15, true, config))
        assertFalse(DeltaSelector.isCCDeltaInRange(0.35, true, config))
    }
    
    @Test
    fun `test CC delta range check for satellite symbols`() {
        assertTrue(DeltaSelector.isCCDeltaInRange(0.20, false, config))
        assertTrue(DeltaSelector.isCCDeltaInRange(0.15, false, config))
        assertTrue(DeltaSelector.isCCDeltaInRange(0.25, false, config))
        assertFalse(DeltaSelector.isCCDeltaInRange(0.10, false, config))
        assertFalse(DeltaSelector.isCCDeltaInRange(0.30, false, config))
    }
    
    @Test
    fun `test elevated assignment risk detection`() {
        assertFalse(DeltaSelector.hasElevatedAssignmentRisk(0.30))
        assertFalse(DeltaSelector.hasElevatedAssignmentRisk(0.40))
        assertTrue(DeltaSelector.hasElevatedAssignmentRisk(0.45))
        assertTrue(DeltaSelector.hasElevatedAssignmentRisk(0.50))
    }
    
    @Test
    fun `test delta suitability scoring`() {
        val perfectScore = DeltaSelector.scoreDeltaSuitability(0.25, true, config)
        val acceptableScore = DeltaSelector.scoreDeltaSuitability(0.20, true, config)
        val unacceptableScore = DeltaSelector.scoreDeltaSuitability(0.15, true, config)
        
        assertTrue(perfectScore > acceptableScore)
        assertTrue(acceptableScore > 0.0)
        assertEquals(0.0, unacceptableScore, 0.01)
    }
    
    @Test
    fun `test symbol classification by market cap`() {
        val highCap = 150_000_000_000L // $150B
        val mediumCap = 75_000_000_000L // $75B
        val lowCap = 25_000_000_000L // $25B
        
        assertTrue(DeltaSelector.classifySymbol(highCap, null, config))
        assertTrue(DeltaSelector.classifySymbol(mediumCap, null, config))
        assertFalse(DeltaSelector.classifySymbol(lowCap, null, config))
    }
    
    @Test
    fun `test symbol classification by IV rank`() {
        val mediumCap = 75_000_000_000L
        
        // High IV makes it satellite even with decent market cap
        assertTrue(DeltaSelector.classifySymbol(mediumCap, 30.0, config))
        assertFalse(DeltaSelector.classifySymbol(mediumCap, 60.0, config))
    }
}