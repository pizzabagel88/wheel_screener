package com.wheelscreener.domain.scoring

import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

fun Instant.plus(period: DateTimePeriod): Instant = plus(period, TimeZone.of("America/New_York"))
