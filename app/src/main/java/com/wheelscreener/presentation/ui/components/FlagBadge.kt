package com.wheelscreener.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wheelscreener.domain.model.CandidateFlag

@Composable
fun FlagBadge(flag: CandidateFlag) {
    val (backgroundColor, textColor) = when (flag) {
        CandidateFlag.LOW_OPEN_INTEREST,
        CandidateFlag.LOW_VOLUME,
        CandidateFlag.WIDE_SPREAD -> Color(0xFFFFB300) to Color.Black // Warning / Amber

        CandidateFlag.EARNINGS_IN_EXPIRATION,
        CandidateFlag.DIVIDEND_ASSIGNMENT_RISK,
        CandidateFlag.MAJOR_BINARY_EVENT -> Color(0xFFEF5350) to Color.White // Danger / Red

        CandidateFlag.POSSIBLE_TREND_BREAKDOWN,
        CandidateFlag.BELOW_200_SMA -> Color(0xFFFF7043) to Color.White // Trend Risk / Orange

        CandidateFlag.BELOW_MARKET_CAP_THRESHOLD,
        CandidateFlag.NEGATIVE_FREE_CASH_FLOW,
        CandidateFlag.HIGH_NET_DEBT -> Color(0xFFAB47BC) to Color.White // Fundamental Risk / Purple

        CandidateFlag.STALE_QUOTE -> Color(0xFF78909C) to Color.White // Stale data / Grey
        else -> Color(0xFF757575) to Color.White // Default / Grey
    }

    Box(
        modifier = Modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = flag.name.replace("_", " "),
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}
