package com.trademaster.pro.data.repo

import com.trademaster.pro.data.model.*

// Mirrors the `defaultStore` seed data from the original web app 1:1, so a
// fresh install looks and feels the same as the browser version did.
object SeedData {
    private val now = System.currentTimeMillis()

    val signals = listOf(
        SignalEntity(pair = "EUR/USD", type = SignalType.BUY, entry = "1.0845", tp = "1.0890", sl = "1.0820",
            status = SignalStatus.ACTIVE, pips = "+45", notes = "Bullish breakout on 4H", createdAt = now - 120_000),
        SignalEntity(pair = "GBP/JPY", type = SignalType.SELL, entry = "192.45", tp = "191.80", sl = "192.90",
            status = SignalStatus.ACTIVE, pips = "+65", notes = "Bearish engulfing on D1", createdAt = now - 900_000),
        SignalEntity(pair = "XAU/USD", type = SignalType.BUY, entry = "2345.20", tp = "2360.00", sl = "2338.00",
            status = SignalStatus.ACTIVE, pips = "+147", notes = "Gold demand zone hold", createdAt = now - 1_920_000),
        SignalEntity(pair = "USD/CAD", type = SignalType.PENDING, entry = "1.3640", tp = "1.3580", sl = "1.3680",
            status = SignalStatus.PENDING, pips = "--", notes = "Waiting for pullback", createdAt = now - 3_600_000),
        SignalEntity(pair = "AUD/USD", type = SignalType.SELL, entry = "0.6645", tp = "0.6600", sl = "0.6670",
            status = SignalStatus.CLOSED, pips = "+45", notes = "TP hit perfectly", createdAt = now - 10_800_000),
        SignalEntity(pair = "USD/JPY", type = SignalType.BUY, entry = "151.20", tp = "152.00", sl = "150.80",
            status = SignalStatus.ACTIVE, pips = "+22", notes = "Trend continuation", createdAt = now - 2_700_000),
    )

    val posts = listOf(
        PostEntity(author = "TradeMaster", avatar = "TM",
            text = "EUR/USD breaking above key resistance at 1.0840. Bullish momentum building on the 4H chart with RSI divergence confirming the move. Watch for a retest before entry.",
            tags = listOf("EURUSD", "Technical Analysis", "Buy Setup"), likes = 24, liked = false, comments = 8,
            pinned = true, createdAt = now - 300_000),
        PostEntity(author = "TradeMaster", avatar = "TM",
            text = "Gold (XAU/USD) update: Price has reached our first TP at \$2,350. Traders who entered at \$2,345 can move SL to breakeven and let the rest run to TP2 at \$2,360.",
            tags = listOf("Gold", "Signal Update", "Risk Management"), likes = 56, liked = false, comments = 12,
            pinned = false, createdAt = now - 3_600_000),
        PostEntity(author = "TradeMaster", avatar = "TM",
            text = "New educational post: Understanding Market Structure - How to identify swing highs and lows to determine trend direction. Essential reading for beginners.",
            tags = listOf("Education", "Market Structure", "Beginner"), likes = 89, liked = false, comments = 23,
            pinned = false, createdAt = now - 10_800_000),
    )

    val polls = listOf(
        PollEntity(question = "Which pair will move the most this week?", options = listOf(
            PollOption("EUR/USD", 65), PollOption("GBP/JPY", 44), PollOption("XAU/USD", 34), PollOption("USD/JPY", 13)
        ), active = true, userVoted = false, createdAt = now),
        PollEntity(question = "What is your primary trading style?", options = listOf(
            PollOption("Scalping (M1-M5)", 44), PollOption("Day Trading (M15-H1)", 85),
            PollOption("Swing Trading (H4-D1)", 92), PollOption("Position (W1+)", 22)
        ), active = true, userVoted = false, createdAt = now),
    )

    val qa = listOf(
        QaEntity(question = "What is the best time to trade Forex?",
            answer = "The optimal trading window is during the London-New York overlap (8:00 AM - 12:00 PM EST) when liquidity and volatility are highest.",
            votes = 45, voted = false, createdAt = now),
        QaEntity(question = "How much capital do I need to start?",
            answer = "You can start with as little as \$100 with a micro account, but \$1,000-\$5,000 is recommended for proper risk management. Never risk more than 1-2% per trade.",
            votes = 38, voted = false, createdAt = now),
        QaEntity(question = "Which timeframe is best for beginners?",
            answer = "H4 and D1 timeframes are ideal for beginners. They filter out market noise and align with institutional order flow. Avoid M1 and M5 until you've mastered higher timeframes.",
            votes = 52, voted = false, createdAt = now),
    )

    val courses = listOf(
        CourseEntity(title = "Price Action Mastery",
            desc = "Complete guide to reading raw price charts without indicators. Master candlestick patterns, order blocks, and fair value gaps.",
            duration = "2h 15m", lessons = 12, type = CourseType.VIDEO, category = CourseCategory.ADVANCED,
            enrolled = 342, createdAt = now),
        CourseEntity(title = "Risk Management Bible",
            desc = "Position sizing, drawdown control, and account protection strategies used by prop firms and institutional traders.",
            duration = "45 min", lessons = 8, type = CourseType.PDF, category = CourseCategory.BEGINNER,
            enrolled = 567, createdAt = now),
        CourseEntity(title = "Smart Money Concepts",
            desc = "Institutional order flow, liquidity sweeps, and breaker blocks. Learn how banks manipulate retail traders.",
            duration = "3h 30m", lessons = 18, type = CourseType.VIDEO, category = CourseCategory.ADVANCED,
            enrolled = 289, createdAt = now),
        CourseEntity(title = "Psychology of Trading",
            desc = "Master your emotions, eliminate revenge trading, and develop the disciplined mindset of a 7-figure trader.",
            duration = "1h 20m", lessons = 6, type = CourseType.VIDEO, category = CourseCategory.BEGINNER,
            enrolled = 412, createdAt = now),
        CourseEntity(title = "Support & Resistance",
            desc = "Drawing accurate levels that institutions actually respect. Multi-timeframe confluence techniques.",
            duration = "55 min", lessons = 10, type = CourseType.PDF, category = CourseCategory.INTERMEDIATE,
            enrolled = 198, createdAt = now),
    )

    val media = listOf(
        MediaEntity(name = "EURUSD_Setup_Aug28.png", type = MediaType.IMAGE, sizeLabel = "2.4 MB", dateLabel = "Today", downloads = 45, createdAt = now),
        MediaEntity(name = "Weekly_Market_Outlook.pdf", type = MediaType.PDF, sizeLabel = "1.8 MB", dateLabel = "Today", downloads = 128, createdAt = now),
        MediaEntity(name = "Risk_Management_Guide.pdf", type = MediaType.PDF, sizeLabel = "4.2 MB", dateLabel = "Yesterday", downloads = 89, createdAt = now),
        MediaEntity(name = "Live_Trading_Session_27.mp4", type = MediaType.VIDEO, sizeLabel = "156 MB", dateLabel = "Yesterday", downloads = 234, createdAt = now),
        MediaEntity(name = "Support_Resistance_Template.tpl", type = MediaType.FILE, sizeLabel = "12 KB", dateLabel = "2 days ago", downloads = 67, createdAt = now),
    )

    val ticker = listOf(
        TickerEntity("EUR/USD", 1.0847, 0.24, true),
        TickerEntity("GBP/USD", 1.2734, 0.18, true),
        TickerEntity("USD/JPY", 151.42, -0.12, false),
        TickerEntity("XAU/USD", 2347.80, 0.85, true),
        TickerEntity("USD/CAD", 1.3632, -0.05, false),
        TickerEntity("AUD/USD", 0.6641, 0.31, true),
        TickerEntity("NZD/USD", 0.6123, -0.08, false),
        TickerEntity("USD/CHF", 0.9021, 0.15, true),
        TickerEntity("GBP/JPY", 192.38, -0.22, false),
        TickerEntity("EUR/GBP", 0.8512, 0.05, true),
    )

    val flags = mapOf(
        "EUR/USD" to "🇪🇺🇺🇸", "GBP/USD" to "🇬🇧🇺🇸", "USD/JPY" to "🇺🇸🇯🇵", "XAU/USD" to "🥇🇺🇸",
        "USD/CAD" to "🇺🇸🇨🇦", "AUD/USD" to "🇦🇺🇺🇸", "NZD/USD" to "🇳🇿🇺🇸", "USD/CHF" to "🇺🇸🇨🇭",
        "GBP/JPY" to "🇬🇧🇯🇵", "EUR/GBP" to "🇪🇺🇬🇧"
    )
}
