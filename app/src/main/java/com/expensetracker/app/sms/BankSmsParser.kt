package com.expensetracker.app.sms

import android.util.Log
import com.expensetracker.app.domain.model.Category
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.format.SignStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.util.Locale
import java.util.regex.Pattern

enum class TransactionType {
    DEBIT,
    CREDIT,
}

data class ParsedSmsTransaction(
    val type: TransactionType,
    val amount: Double,
    val bankName: String,
    val accountLast4: String?,
    val date: LocalDate,
    val time: LocalTime?,
    val category: Category,
    val description: String,
    val rawSms: String,
    val smsDate: Long,
)

object BankSmsParser {

    private const val TAG = "BankSmsParser"

    private val transactionAmountPattern = Pattern.compile(
        "(?:Rs\\s*\\.?|INR\\s*|₹|USD\\s*\\.?)\\s*([0-9]{1,3}(?:,[0-9]{2,3})+(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE,
    )

    private val balancePatterns = listOf(
        Pattern.compile(
            "(?:available|avl|avbl|avbl\\.?|bal|balance|total\\s*bal|outstanding)(?:\\s*(?:balance|bal|amt|amount)?)(?:\\s*[:\\-]?\\s*)(?:Rs\\s*\\.?|INR\\s*|₹|USD\\s*\\.?)?\\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE,
        ),
    )

    private val accountPatterns = listOf(
        Pattern.compile("(?:a/c|account|card|ac)(?:\\s*(?:no)?\\.?\\s*)(?:\\.{3}|xx|ending|ending\\s+with|#)?\\s*([0-9]{2,6})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:xx|\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.)([0-9]{2,6})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b([0-9]{4})\\b(?=.*(?:a/c|account|card|debit|credited|debited))", Pattern.CASE_INSENSITIVE),
    )

    private val datePatterns = listOf(
        // 4-digit year formats first to avoid ambiguity with YY-MM-DD
        DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
            .toFormatter(Locale.ENGLISH),
        DateTimeFormatterBuilder()
            .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral('-')
            .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT)
            .appendLiteral('-')
            .appendValueReduced(ChronoField.YEAR, 2, 4, 2000)
            .toFormatter(Locale.ENGLISH),
        DateTimeFormatterBuilder()
            .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral('/')
            .appendValue(ChronoField.MONTH_OF_YEAR, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral('/')
            .appendValueReduced(ChronoField.YEAR, 2, 4, 2000)
            .toFormatter(Locale.ENGLISH),
        DateTimeFormatterBuilder()
            .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral('-')
            .appendValueReduced(ChronoField.YEAR, 2, 4, 2000)
            .toFormatter(Locale.ENGLISH),
        DateTimeFormatterBuilder()
            .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT)
            .appendLiteral(' ')
            .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral(", ")
            .appendValue(ChronoField.YEAR, 4)
            .toFormatter(Locale.ENGLISH),
    )

    private val timePattern = Pattern.compile(
        "(?:at|on|time)\\s+([0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?\\s*(?:AM|PM|am|pm)?)",
        Pattern.CASE_INSENSITIVE,
    )

    private val bankKeywords = mapOf(
        "SBI" to "SBI",
        "HDFC" to "HDFC Bank",
        "ICICI" to "ICICI Bank",
        "AXIS" to "Axis Bank",
        "KOTAK" to "Kotak Mahindra Bank",
        "PNB" to "Punjab National Bank",
        "BARODA" to "Bank of Baroda",
        "BOB" to "Bank of Baroda",
        "CANARA" to "Canara Bank",
        "KBLBNK" to "Karnataka Bank",
        "KARNATAKA" to "Karnataka Bank",
        "YESBNK" to "Yes Bank",
        "YESBANK" to "Yes Bank",
        "INDUSIND" to "IndusInd Bank",
        "IDFC" to "IDFC First Bank",
        "AU" to "AU Small Finance Bank",
        "FEDERAL" to "Federal Bank",
        "UNION" to "Union Bank",
        "INDIAN" to "Indian Bank",
        "IOB" to "Indian Overseas Bank",
        "UCO" to "UCO Bank",
        "BANDHAN" to "Bandhan Bank",
        "RBL" to "RBL Bank",
        "CITI" to "Citibank",
        "HSBC" to "HSBC",
        "SCB" to "Standard Chartered",
        "AMEX" to "American Express",
    )

    private val debitIndicators = listOf(
        "debited", "debit", "spent", "withdrawn", "withdrawal", "paid", "payment",
        "deducted", "purchase", "charged", "txn", "transaction of", "sent",
    )

    private val creditIndicators = listOf(
        "credited", "credit", "deposited", "deposit", "received", "added",
        "refund", "cashback",
    )

    private val skipKeywords = listOf(
        "DECLINED", "UNABLE TO CONFIRM", "WILL BE DEBITED", "MANDATE SET",
        "MANDATE CANCELLED", "AUTO-PAY", "ANNUAL MAINTENANCE CHARGES",
        "INSTALLATION AND DEMO", "FOREX MARKUP", "WILL BE UNAVAILABLE",
        "REVISED DYNAMIC CURRENCY CONVERSION", "CASH LOAN", "LOAN OFFER",
        "PRE-APPROVED", "ELIGIBLE FOR", "CREDIT CARD APPLICATION",
        "AUTOPAY (E-MANDATE) SUCCESS", "AUTOPAY (E‑MANDATE) SUCCESS",
    )

    private val categoryKeywords = mapOf(
        Category.FOOD to listOf("swiggy", "zomato", "food", "restaurant", "dominos", "pizza", "burger", "grocery", "groceries", "blinkit", "zepto", "bigbasket"),
        Category.TRANSPORT to listOf("uber", "ola", "rapido", "namma yatri", "petrol", "fuel", "diesel", "metro", "toll", "parking", "irctc", "train", "bus", "flight", "goindigo", "airasia"),
        Category.SHOPPING to listOf("amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "shopify", "mart", "store", "shopping"),
        Category.ENTERTAINMENT to listOf("netflix", "spotify", "hotstar", "prime", "disney", "sony", "youtube", "movie", "theatre", "cinema"),
        Category.HEALTH to listOf("hospital", "pharmacy", "medical", "medicine", "dr", "doctor", "apollo", "medplus", "1mg", "pharmeasy"),
        Category.BILLS to listOf("electricity", "water", "gas", "broadband", "wifi", "mobile", "recharge", "dth", "insurance", "emi", "loan", "rent"),
        Category.SALARY to listOf("salary", "payroll", "income", "interest", "dividend", "maturity"),
        Category.CASH to listOf("atm", "cash withdrawal", "cash", "self"),
        Category.TRANSFER to listOf("upi", "neft", "rtgs", "imps", "transfer", "paytm", "phonepe", "gpay", "google pay"),
    )

    fun parse(message: SmsMessage): ParsedSmsTransaction? {
        val body = message.body
        val upperBody = body.uppercase(Locale.ENGLISH)

        if (isPromotionalOrOtp(upperBody) || shouldSkip(upperBody, body)) {
            Log.d(TAG, "SKIP promo/otp/skip: ${body.take(60)}")
            return null
        }

        val type = detectTransactionType(upperBody) ?: run {
            Log.d(TAG, "SKIP no type: ${body.take(80)}")
            return null
        }
        val amount = extractAmount(body, upperBody, type) ?: run {
            Log.d(TAG, "SKIP no amount: ${body.take(80)}")
            return null
        }
        val bankName = extractBankName(message.address, upperBody)
        val accountLast4 = extractAccountLast4(upperBody)
        val date = extractDate(upperBody, message.date)
        val time = extractTime(upperBody)
        val description = extractDescription(body, type)
        val category = classifyCategory(upperBody, type)

        Log.d(TAG, "PARSED amount=$amount type=$type bank=$bankName desc='$description' category=$category raw=${body.take(100)}")

        return ParsedSmsTransaction(
            type = type,
            amount = amount,
            bankName = bankName,
            accountLast4 = accountLast4,
            date = date,
            time = time,
            category = category,
            description = description,
            rawSms = message.body,
            smsDate = message.date,
        )
    }

    private fun isPromotionalOrOtp(body: String): Boolean {
        val otpKeywords = listOf("OTP", "ONE TIME PASSWORD", "VERIFICATION CODE", "LOGIN CODE", "DO NOT SHARE")
        val promoKeywords = listOf("CASHBACK OFFER", "DISCOUNT", "SHOP NOW", "BESTSELLER", "FESTIVAL", "SALE IS LIVE", "XMAS")
        return otpKeywords.any { body.contains(it) } || promoKeywords.any { body.contains(it) }
    }

    private fun shouldSkip(upperBody: String, body: String): Boolean {
        for (kw in skipKeywords) {
            if (upperBody.contains(kw)) return true
        }
        if (upperBody.contains("MANDATE") &&
            !upperBody.contains("EXECUTED")
        ) {
            return true
        }
        if (Regex("\\bLOAN\\b.*\\bOFFER\\b").containsMatchIn(upperBody)) return true
        if (Regex("\\bCAN\\s+BE\\s+CREDITED\\b").containsMatchIn(upperBody)) return true
        if (upperBody.contains("DAILY LIMIT") && upperBody.contains("INCREASED")) return true
        if (upperBody.contains("REQUESTED") && upperBody.contains("WILL BE DEBITED")) return true
        if (upperBody.contains("APPROVAL UP TO")) return true
        if (upperBody.contains("COMPLETE KYC")) return true
        if (upperBody.contains("CASHBACK ON YOUR")) return true
        if (upperBody.contains("HTTP://") || upperBody.contains("HTTPS://")) {
            if (upperBody.contains("CASHBACK") || upperBody.contains("OFFER") || upperBody.contains("APPROVAL")) return true
        }
        return false
    }

    private fun detectTransactionType(body: String): TransactionType? {
        val upperBody = body.uppercase(Locale.ENGLISH)

        val debitKeywords = debitIndicators.map { it.uppercase(Locale.ENGLISH) }
        val creditKeywords = creditIndicators.map { it.uppercase(Locale.ENGLISH) }

        val debitPositions = debitKeywords.mapNotNull { kw ->
            val idx = upperBody.indexOf(kw)
            if (idx >= 0) idx to kw else null
        }
        val creditPositions = creditKeywords.mapNotNull { kw ->
            val idx = upperBody.indexOf(kw)
            if (idx >= 0) idx to kw else null
        }

        if (debitPositions.isEmpty() && creditPositions.isEmpty()) return null
        if (debitPositions.isNotEmpty() && creditPositions.isEmpty()) return TransactionType.DEBIT
        if (creditPositions.isNotEmpty() && debitPositions.isEmpty()) return TransactionType.CREDIT

        // Both exist — use strong verbs and position to decide.
        // "debited"/"credited"/"spent"/"deposited" are primary verbs.
        // Bare "credit"/"debit" near the payee are weaker indicators.
        val strongDebitWords = setOf("DEBITED", "SPENT", "PAID", "WITHDRAWN", "DEDUCTED", "SENT")
        val strongCreditWords = setOf("CREDITED", "DEPOSITED", "RECEIVED")

        val debitStrong = debitPositions.any { it.second in strongDebitWords }
        val creditStrong = creditPositions.any { it.second in strongCreditWords }

        return when {
            debitStrong && !creditStrong -> TransactionType.DEBIT
            creditStrong && !debitStrong -> TransactionType.CREDIT
            else -> {
                // Fall back to whichever strong/primary verb appears first
                val firstDebit = debitPositions.filter { it.second in strongDebitWords }.minByOrNull { it.first }
                val firstCredit = creditPositions.filter { it.second in strongCreditWords }.minByOrNull { it.first }

                when {
                    firstDebit != null && firstCredit == null -> TransactionType.DEBIT
                    firstCredit != null && firstDebit == null -> TransactionType.CREDIT
                    firstDebit != null && firstCredit != null -> {
                        if (firstDebit.first < firstCredit.first) TransactionType.DEBIT else TransactionType.CREDIT
                    }
                    else -> {
                        // No strong verbs; pick the earliest keyword
                        val firstDebitAny = debitPositions.minByOrNull { it.first }
                        val firstCreditAny = creditPositions.minByOrNull { it.first }
                        if (firstDebitAny != null && firstCreditAny != null) {
                            if (firstDebitAny.first < firstCreditAny.first) TransactionType.DEBIT else TransactionType.CREDIT
                        } else {
                            TransactionType.DEBIT
                        }
                    }
                }
            }
        }
    }

    private fun extractAmount(body: String, upperBody: String, type: TransactionType): Double? {
        // Step 1: Collect all Rs./INR/₹ prefixed amounts with their positions
        val allAmounts = mutableListOf<Pair<Int, Double>>()
        val txMatcher = transactionAmountPattern.matcher(body)
        while (txMatcher.find()) {
            val raw = txMatcher.group(1)?.replace(",", "") ?: continue
            raw.toDoubleOrNull()?.let { value ->
                if (value > 0 && value < 50_000_000) {
                    allAmounts.add(txMatcher.start() to value)
                }
            }
        }

        if (allAmounts.isEmpty()) return null

        // Step 2: Collect balance section positions to exclude them
        val balanceRanges = mutableListOf<IntRange>()
        for (bPattern in balancePatterns) {
            val bMatcher = bPattern.matcher(upperBody)
            while (bMatcher.find()) {
                balanceRanges.add(bMatcher.start()..bMatcher.end())
            }
        }

        // Step 3: Filter out amounts that appear inside balance sections
        val transactionAmounts = allAmounts.filter { (pos, _) ->
            balanceRanges.none { range -> pos in range }
        }

        if (transactionAmounts.isEmpty()) return null

        // Step 4: Find the keyword position for the transaction type
        val keywords = if (type == TransactionType.DEBIT) debitIndicators else creditIndicators
        val keywordPos = keywords.mapNotNull { kw ->
            val idx = upperBody.indexOf(kw.uppercase(Locale.ENGLISH))
            if (idx >= 0) idx else null
        }.minOrNull()

        // Step 5: Pick the amount closest to (and before) the keyword
        // In Indian SMS: "Rs.500 debited from..." — amount comes BEFORE the keyword
        val result = if (keywordPos != null) {
            val beforeKeyword = transactionAmounts.filter { it.first < keywordPos }
            val afterKeyword = transactionAmounts.filter { it.first >= keywordPos }
            beforeKeyword.lastOrNull() ?: afterKeyword.firstOrNull()
        } else {
            // No keyword found — first Rs. amount is the transaction (balance comes later)
            transactionAmounts.firstOrNull()
        }

        return result?.second
    }

    private fun extractBankName(address: String, body: String): String {
        val searchText = "$address $body".uppercase(Locale.ENGLISH)
        for ((keyword, bankName) in bankKeywords) {
            if (searchText.contains(keyword)) return bankName
        }
        return "Bank"
    }

    private fun extractAccountLast4(body: String): String? {
        for (pattern in accountPatterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                matcher.group(1)?.let { return it }
            }
        }
        return null
    }

    private fun extractDate(body: String, fallback: Long): LocalDate {
        for (formatter in datePatterns) {
            try {
                val regex = dateRegexFor(formatter)
                val matcher = regex.matcher(body)
                if (matcher.find()) {
                    return LocalDate.parse(matcher.group(), formatter)
                }
            } catch (_: DateTimeParseException) {
                // continue to next pattern
            }
        }
        return LocalDate.ofEpochDay(fallback / (1000 * 60 * 60 * 24))
    }

    private fun extractTime(body: String): LocalTime? {
        val matcher = timePattern.matcher(body)
        if (matcher.find()) {
            val raw = matcher.group(1) ?: return null
            return try {
                val formatter = DateTimeFormatterBuilder()
                    .appendValue(ChronoField.HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
                    .appendLiteral(':')
                    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                    .optionalStart()
                    .appendLiteral(':')
                    .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                    .optionalEnd()
                    .optionalStart()
                    .appendPattern(" a")
                    .optionalEnd()
                    .toFormatter(Locale.ENGLISH)
                LocalTime.parse(raw.uppercase(Locale.ENGLISH).trim(), formatter)
            } catch (_: DateTimeParseException) {
                null
            }
        }
        return null
    }

    private fun extractDescription(body: String, type: TransactionType): String {
        val lowerBody = body.lowercase(Locale.ENGLISH)
        val upperBody = body.uppercase(Locale.ENGLISH)

        // Pattern 0a: HDFC "Sent Rs.X From A/C *NNNN To NAME On DD/MM/YY"
        if (upperBody.contains("FROM HDFC BANK A/C") || upperBody.contains("SENT RS")) {
            val sentToPattern = Pattern.compile(
                "\\bto\\s+([A-Za-z0-9&\\s._\\-]{2,40}?)(?:\\s+on|\\s+ref|\\s+from|\\s+via|\\s+upi|\\. |,|;|$)",
                Pattern.CASE_INSENSITIVE or Pattern.DOTALL,
            )
            sentToPattern.matcher(body).let { matcher ->
                if (matcher.find()) {
                    matcher.group(1)?.trim()?.let {
                        val cleaned = cleanName(it)
                        if (isValidDescription(cleaned)) return cleaned
                    }
                }
            }
        }

        // Pattern 0b: "For X" in AutoPay / mandate executed messages
        val forPattern = Pattern.compile(
            "\\bfor\\s+([A-Za-z0-9&\\s._\\-]{2,35}?)(?:\\s+(?:starting|mandate|id|txn|dt|via|on\\s+\\d|from|$))",
            Pattern.CASE_INSENSITIVE,
        )
        forPattern.matcher(body).let { matcher ->
            if (matcher.find()) {
                matcher.group(1)?.trim()?.let {
                    val cleaned = cleanName(it)
                    if (isValidDescription(cleaned) && cleaned.lowercase(Locale.ENGLISH) !in setOf("payment", "ollamainc")) {
                        return cleaned
                    }
                }
            }
        }

        // Pattern 0c: "towards X"
        val towardsPattern = Pattern.compile(
            "\\btowards\\s+([A-Za-z0-9&\\s._\\-]{2,35}?)(?:\\.|,|;|\\s+(?:from|on|ref|upi|for|$))",
            Pattern.CASE_INSENSITIVE,
        )
        towardsPattern.matcher(body).let { matcher ->
            if (matcher.find()) {
                matcher.group(1)?.trim()?.let {
                    val cleaned = cleanName(it)
                    if (isValidDescription(cleaned)) return cleaned
                }
            }
        }

        // Pattern 0d: "Your Swiggy Order #..." / "your Swiggy Money" etc.
        val merchantOrderPattern = Pattern.compile(
            "your\\s+([A-Za-z0-9&\\s]{2,25}?)\\s+(?:order|money|wallet)\\b",
            Pattern.CASE_INSENSITIVE,
        )
        merchantOrderPattern.matcher(body).let { matcher ->
            if (matcher.find()) {
                matcher.group(1)?.trim()?.let {
                    val cleaned = cleanName(it)
                    if (isValidDescription(cleaned)) return cleaned
                }
            }
        }

        // Pattern 1: UPI / VPA — "from VPA xxx@bank", "paid to xxx@upi"
        // Also handle `?` as a malformed VPA separator seen in some SMS.
        val vpaPattern = Pattern.compile(
            "(?:from vpa|to vpa|paid to)\\s+([a-zA-Z0-9._@\\-]{3,50}?)(?:\\s|\\.|,|;|upi|ref|on\\s+\\d|at\\s+[A-Z]|\\s+for)",
            Pattern.CASE_INSENSITIVE,
        )
        vpaPattern.matcher(lowerBody).let { matcher ->
            if (matcher.find()) {
                val vpa = matcher.group(1)?.trim() ?: return@let
                if (vpa.contains("@") || vpa.contains("?")) {
                    return cleanVpa(vpa.replace("?", "@"))
                }
            }
        }

        // Pattern 2a: Strong UPI pattern — capture everything between "trf/paid/transfer to" and "UPI:ref"
        val upiPayeePattern = Pattern.compile(
            "(?:trf to|transfer to|paid to|for payment to|payment to)\\s+(.+?)(?:\\s*\\.\\s*upi:|\\s+upi:|\\s+\\.)",
            Pattern.CASE_INSENSITIVE,
        )
        upiPayeePattern.matcher(lowerBody).let { matcher ->
            if (matcher.find()) {
                matcher.group(1)?.trim()?.let {
                    val cleaned = cleanName(it)
                    if (isValidDescription(cleaned)) return cleaned
                }
            }
        }

        // Pattern 2b: "trf to NAME" / "transfer to NAME" / "paid to NAME" (UPI person/merchant)
        // Allow hyphen in names, stop before question marks or UPI refs.
        val transferToPattern = Pattern.compile(
            "(?:trf to|transfer to|paid to|for payment to|payment to)\\s+([A-Za-z0-9&\\s._\\-]{2,60}?)(?:\\.|,|;|\\?|\\s+(?:via|through|using|on\\s+\\d|upi|ref|for dispute))",
            Pattern.CASE_INSENSITIVE,
        )
        transferToPattern.matcher(lowerBody).let { matcher ->
            if (matcher.find()) {
                matcher.group(1)?.trim()?.let {
                    val cleaned = cleanName(it)
                    if (isValidDescription(cleaned)) return cleaned
                }
            }
        }

        // Pattern 3: "at/on MERCHANT NAME" for card transactions
        val atPattern = Pattern.compile(
            "(?:\\bat\\b|\\bon\\b)\\s+([A-Za-z0-9&\\s]{2,35}?)(?:\\.|,|;|\\s+(?:for|using|with|on\\s+\\d|Rs|UPI|Ref|available|avl|bal))",
            Pattern.CASE_INSENSITIVE,
        )
        atPattern.matcher(lowerBody).let { matcher ->
            while (matcher.find()) {
                val candidate = matcher.group(1)?.trim() ?: continue
                val cleaned = cleanName(candidate)
                if (isValidDescription(cleaned)) return cleaned
            }
        }

        // Pattern 4: Payee after semicolon — general pattern in debit SMS like
        // "...debited for Rs X; PAYEE credited" or "...debit; MERCHANT"
        val semicolonPayeePattern = Pattern.compile(
            ";\\s*([A-Za-z0-9\\s.&]{2,40}?)(?:\\s+credit|\\s+credited|\\s+debited|\\s+debit|$)",
            Pattern.CASE_INSENSITIVE,
        )
        semicolonPayeePattern.matcher(lowerBody).let { matcher ->
            if (matcher.find()) {
                matcher.group(1)?.trim()?.let {
                    val cleaned = cleanName(it)
                    if (isValidDescription(cleaned)) return cleaned
                }
            }
        }

        // Pattern 5: "Info: XXXX" / narration fields — stop before balance keywords
        val infoPattern = Pattern.compile(
            "(?:info|narration|remarks?|note)[:\\-\\s]+([A-Za-z0-9\\s&._\\-*]{2,80}?)(?:\\.|,|;|\\s+(?:Ref|UPI|NEFT|IMPS|RTGS|available|avl|bal|balance))",
            Pattern.CASE_INSENSITIVE,
        )
        infoPattern.matcher(lowerBody).let { matcher ->
            if (matcher.find()) {
                val infoText = matcher.group(1)?.trim() ?: return@let
                if (infoText.contains("-") || infoText.contains("*")) {
                    val parts = infoText.split("[-*]".toRegex())
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    val meaningful = parts.filter { part ->
                        part.isNotBlank() &&
                            !part.matches(Regex("^[0-9]+$")) &&
                            !part.matches(Regex("^[A-Za-z0-9]{10,}$")) &&
                            part.uppercase(Locale.ENGLISH) !in setOf("INF", "INFT", "NEFT", "IMPS", "RTGS", "CAM", "CA") &&
                            part.length <= 30
                    }
                    if (meaningful.isNotEmpty()) {
                        val candidate = cleanName(meaningful.last())
                        if (isValidDescription(candidate)) return candidate
                    }
                }
                // Only use the raw info text if it looks like a real phrase, not a code chain.
                if (infoText.contains(" ") && !infoText.matches(Regex("^[A-Za-z0-9*\\-]+$"))) {
                    val cleaned = cleanName(infoText)
                    if (isValidDescription(cleaned)) return cleaned
                }
            }
        }

        // Pattern 6: Remittance / received in Nostro
        val remitPattern = Pattern.compile(
            "remittance\\s+([A-Za-z0-9]{5,30})\\s+of",
            Pattern.CASE_INSENSITIVE,
        )
        remitPattern.matcher(lowerBody).let { matcher ->
            if (matcher.find()) {
                matcher.group(1)?.trim()?.let {
                    return cleanName("Remittance $it")
                }
            }
        }

        // Pattern 7: "from XXXX" / "by XXXX" for credits — exclude Rs/INR/₹/USD
        if (type == TransactionType.CREDIT) {
            val fromPattern = Pattern.compile(
                "(?:from|by)\\s+(?!Rs|INR|₹|USD)([A-Za-z0-9&\\s]{2,35}?)(?:\\.|,|;|\\s+(?:to|for|ref|upi|neft|imps|rtgs|date|on\\s+\\d))",
                Pattern.CASE_INSENSITIVE,
            )
            fromPattern.matcher(lowerBody).let { matcher ->
                if (matcher.find()) {
                    val candidate = matcher.group(1)?.trim() ?: return@let
                    if (!candidate.uppercase(Locale.ENGLISH).startsWith("RS") &&
                        !candidate.uppercase(Locale.ENGLISH).startsWith("INR") &&
                        !candidate.uppercase(Locale.ENGLISH).startsWith("USD") &&
                        !candidate.startsWith("₹")
                    ) {
                        val cleaned = cleanName(candidate)
                        if (isValidDescription(cleaned)) return cleaned
                    }
                }
            }
        }

        // Pattern 8: Try a looser VPA match on the entire body (including `?` separator)
        val looseVpaPattern = Pattern.compile(
            "\\b([a-zA-Z0-9._\\-]+(?:@|\\?)[a-zA-Z0-9._\\-]+)\\b",
            Pattern.CASE_INSENSITIVE,
        )
        looseVpaPattern.matcher(lowerBody).let { matcher ->
            if (matcher.find()) {
                val vpa = matcher.group(1)?.replace("?", "@") ?: return@let
                val name = vpa.substringBefore("@").replace(".", " ").replace("_", " ")
                return cleanName(name)
            }
        }

        // Pattern 9: NEFT/IMPS/RTGS reference with meaningful text
        val transferPattern = Pattern.compile(
            "(?:neft|imps|rtgs|upi)\\s*(?:transfer)?[:\\-\\s]*([A-Za-z0-9\\s&._]{2,30}?)(?:\\.|,|;|\\s+ref)",
            Pattern.CASE_INSENSITIVE,
        )
        transferPattern.matcher(lowerBody).let { matcher ->
            if (matcher.find()) {
                matcher.group(1)?.trim()?.let {
                    val cleaned = cleanName(it)
                    if (isValidDescription(cleaned)) return cleaned
                }
            }
        }

        // Pattern 10: trailing payee after "debited from / credited to / paid on / spent on"
        val trailingPattern = Pattern.compile(
            "(?:debited|credited|paid|spent)\\s+(?:from|to|on)\\s+.*?(?:\\s+|\\n)([A-Za-z][A-Za-z0-9\\s&.*_]{2,35}?)(?:\\.|,|;|\\s+Bal|\\s+Not\\s+You|\\s+Call|\\s+If)",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL,
        )
        trailingPattern.matcher(body).let { matcher ->
            if (matcher.find()) {
                matcher.group(1)?.trim()?.trim('.', '*')?.let {
                    val cleaned = cleanName(it)
                    if (isValidDescription(cleaned)) return cleaned
                }
            }
        }

        return when {
            lowerBody.contains("credited") -> {
                when {
                    lowerBody.contains("neft") -> "NEFT Credit"
                    lowerBody.contains("imps") -> "IMPS Credit"
                    lowerBody.contains("rtgs") -> "RTGS Credit"
                    lowerBody.contains("inft") -> "NEFT Credit"
                    else -> "Credit"
                }
            }
            else -> {
                when {
                    lowerBody.contains("neft") -> "NEFT Debit"
                    lowerBody.contains("imps") -> "IMPS Debit"
                    lowerBody.contains("rtgs") -> "RTGS Debit"
                    else -> "Debit"
                }
            }
        }
    }

    private fun isValidDescription(name: String): Boolean {
        if (name.length < 2) return false
        if (name.replace(" ", "").replace(".", "").all { it.isDigit() }) return false
        val generic = setOf("Credit", "Debit", "Upi", "Ref", "Bank", "A C", "Ac", "Card", "Atm", "Self", "Account", "Customer", "Our Website", "Rs", "Inr", "Ca")
        return !generic.any { name.equals(it, ignoreCase = true) || name.startsWith(it, ignoreCase = true) }
    }

    private fun cleanName(raw: String): String {
        return raw
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^A-Za-z0-9&\\s.]"), "")
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.lowercase(Locale.ENGLISH).replaceFirstChar { c -> c.uppercaseChar() } }
    }

    private fun cleanVpa(vpa: String): String {
        val localPart = vpa.substringBefore("@")
        val cleanedLocal = localPart.replace(".", " ").replace("_", " ")

        // If the local part is just a phone number, show the full VPA
        return if (localPart.length >= 10 && localPart.all { it.isDigit() }) {
            vpa.lowercase(Locale.ENGLISH)
        } else {
            cleanName(cleanedLocal)
        }
    }


    private fun classifyCategory(body: String, type: TransactionType): Category {
        if (type == TransactionType.CREDIT) {
            if (body.contains("SALARY")) return Category.SALARY
            if (body.contains("CASHBACK")) return Category.SALARY
            if (body.contains("REFUND")) return Category.SHOPPING
            return Category.OTHER
        }

        for ((category, keywords) in categoryKeywords) {
            if (category == Category.SALARY) continue
            if (keywords.any { keyword -> bodyMatchesKeyword(body.uppercase(Locale.ENGLISH), keyword) }) {
                return category
            }
        }
        return Category.OTHER
    }

    private fun bodyMatchesKeyword(bodyUpper: String, keyword: String): Boolean {
        val keywordUpper = keyword.uppercase(Locale.ENGLISH)
        // Special-case "hospital" so "hospitality" / "hospitali..." is not treated as a hospital charge.
        if (keywordUpper == "HOSPITAL") {
            return bodyUpper.contains(Regex("HOSPITAL(?!I)"))
        }
        return bodyUpper.contains(keywordUpper)
    }

    private fun dateRegexFor(formatter: DateTimeFormatter): Pattern {
        val pattern = formatter.toString()
        return when {
            // ISO-like 4-digit year first: 2026-06-08
            pattern.startsWith("Value(Year,") || pattern.startsWith("Value(YearOfEra,") -> {
                Pattern.compile("\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}")
            }
            pattern.contains("yyyy") && pattern.contains("MMM") -> {
                Pattern.compile("\\d{1,2}-[A-Za-z]{3,9}-\\d{4}|\\d{1,2}/\\d{1,2}/\\d{4}|[A-Za-z]{3,9} \\d{1,2}, \\d{4}")
            }
            pattern.contains("yy") && pattern.contains("MMM") -> {
                Pattern.compile("\\d{1,2}-[A-Za-z]{3,9}-\\d{2}")
            }
            else -> {
                Pattern.compile("\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}")
            }
        }
    }
}
