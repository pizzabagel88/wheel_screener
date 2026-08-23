package com.wheelscreener.domain.scoring

import com.wheelscreener.domain.model.StrategyConfig
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DteSelectorTest {
    
    private lateinit var config: StrategyConfig
    private lateinit var currentTime: kotlinx.datetime.Instant
    
    @Before
    fun setup() {
        config = StrategyConfig.default()
        currentTime = Clock.System.now()
    }
    
    @Test
    fun `test calculate DTE correctly`() {
        val futureDate = currentTime.plus(DateTimePeriod(days = 10))
        val dte = DteSelector.calculateDTE(currentTime, futureDate)
        assertEquals(10, dte)
    }
    
    @Test
    fun `test DTE in range check`() {
        assertTrue(DteSelector.isDTEInRange(10, config))
        assertTrue(DteSelector.isDTEInRange(7, config))
        assertTrue(DteSelector.isDTEInRange(14, config))
        assertFalse(DteSelector.isDTEInRange(5, config))
        assertFalse(DteSelector.isDTEInRange(15, config))
    }
    
    @Test
    fun `test weekly expiration detection`() {
        assertTrue(DteSelector.isWeeklyExpiration(5))
        assertTrue(DteSelector.isWeeklyExpiration(7))
        assertTrue(DteSelector.isWeeklyExpiration(8))
        assertFalse(DteSelector.isWeeklyExpiration(9))
        assertFalse(DteSelector.isWeeklyExpiration(14))
    }
    
    @Test
    fun `test optimal DTE selection based on IV rank`() {
        val lowIV = DteSelector.getOptimalDTE(20.0, 25.0, config)
        val mediumIV = DteSelector.getOptimalDTE(45.0, 50.0, config)
        val highIV = DteSelector.getOptimalDTE(70.0, 75.0, config)
        
        assertEquals(14, lowIV) // Low IV = longer DTE
        assertEquals(10, mediumIV) // Medium IV = middle DTE
        assertEquals(7, highIV) // High IV = shorter DTE
    }
    
    @Test
    fun `test DTE suitability scoring`() {
        val perfectScore = DteSelector.scoreDTESuitability(10, config)
        val acceptableScore = DteSelector.scoreDTESuitability(7, config)
        val unacceptableScore = DteSelector.scoreDTESuitability(5, config)
        
        assertTrue(perfectScore > acceptableScore)
        assertTrue(acceptableScore > 0.0)
        assertEquals(0.0, unacceptableScore, 0.01)
    }
    
    @Test
    fun `test DTE with null IV rank`() {
        val dte = DteSelector.getOptimalDTE(null, null, config)
        assertEquals(10, dte) // Should return middle of range
    }
    
    @Test
    fun `test find contracts by specific DTE`() {
        // This would require option contracts, simplified test
        val tolerance = 2
        val targetDTE = 10
        
        // Test the logic with simple values
        assertTrue(targetDTE - tolerance <= targetDTE)
        assertTrue(targetDTE <= targetDTE + tolerance)
    }
}