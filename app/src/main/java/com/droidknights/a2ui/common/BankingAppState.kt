package com.droidknights.a2ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BankTransactionItem(
    val id: String,
    val dateLabel: String,
    val title: String,
    val subtitle: String,
    val amountDelta: Long,
    val balanceAfter: Long,
    val category: String,
)

data class ExchangeVoucherItem(
    val voucherCode: String,
    val currency: String,
    val foreignAmount: Long,
    val krwAmount: Long,
    val branchLabel: String,
    val pickupMethodLabel: String,
    val recipientPhone: String,
    val issuedAt: String,
)

data class TravelBundleStatus(
    val destination: String,
    val jpyAmount: Long,
    val dccBlocked: Boolean,
    val insurancePlanLabel: String,
    val activatedAt: String,
)

@Stable
class BankingAppState {
    var mainAccountBalance by mutableLongStateOf(INITIAL_MAIN_BALANCE)
        private set

    var livingAccountBalance by mutableLongStateOf(INITIAL_LIVING_BALANCE)
        private set

    var cardStatementBalance by mutableLongStateOf(INITIAL_CARD_STATEMENT)
        private set

    var rewardPoints by mutableLongStateOf(INITIAL_REWARD_POINTS)
        private set

    var activeTravelBundle by mutableStateOf<TravelBundleStatus?>(null)
        private set

    val transactions = mutableStateListOf<BankTransactionItem>()
    val exchangeVouchers = mutableStateListOf<ExchangeVoucherItem>()

    init {
         populateInitialData()
    }

    private fun populateInitialData() {
        transactions.clear()
        transactions.addAll(
            listOf(
                BankTransactionItem(
                    id = "tx_init_3",
                    dateLabel = "10.27 19:42",
                    title = "김민수",
                    subtitle = "저녁 더치페이 · 수수료 면제",
                    amountDelta = -120_000L,
                    balanceAfter = 31_520_000L,
                    category = "송금",
                ),
                BankTransactionItem(
                    id = "tx_init_2",
                    dateLabel = "10.26 14:15",
                    title = "이지훈",
                    subtitle = "모임회비 · 수수료 면제",
                    amountDelta = -240_000L,
                    balanceAfter = 31_640_000L,
                    category = "송금",
                ),
                BankTransactionItem(
                    id = "tx_init_1",
                    dateLabel = "10.25 09:00",
                    title = "(주)드로이드나이츠 급여",
                    subtitle = "10월 정기 급여 입금",
                    amountDelta = +4_850_000L,
                    balanceAfter = 31_880_000L,
                    category = "급여",
                ),
                BankTransactionItem(
                    id = "tx_init_0",
                    dateLabel = "10.21 12:30",
                    title = "최유진",
                    subtitle = "경조사비 송금",
                    amountDelta = -80_000L,
                    balanceAfter = 27_030_000L,
                    category = "송금",
                ),
            )
        )
        exchangeVouchers.clear()
        activeTravelBundle = null
    }

    fun resetToInitial() {
        mainAccountBalance = INITIAL_MAIN_BALANCE
        livingAccountBalance = INITIAL_LIVING_BALANCE
        cardStatementBalance = INITIAL_CARD_STATEMENT
        rewardPoints = INITIAL_REWARD_POINTS
        populateInitialData()
    }

    fun recordCurrencyExchange(
        currency: String,
        foreignAmount: Long,
        krwAmount: Long,
        branchLabel: String,
        pickupMethodLabel: String = "모바일 QR 바우처",
        recipientPhone: String = "010-2026-8492",
        isSellBack: Boolean = false,
    ): ExchangeVoucherItem {
        val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
        val nowTime = currentTimeLabel()
        if (isSellBack) {
            mainAccountBalance += krwAmount
            transactions.add(
                0,
                BankTransactionItem(
                    id = "tx_ex_sell_${System.currentTimeMillis()}",
                    dateLabel = nowTime,
                    title = "외화 재환전 입금 ($currency ${fmt.format(foreignAmount)})",
                    subtitle = "80% 환율 우대 · 계좌 즉시 입금",
                    amountDelta = +krwAmount,
                    balanceAfter = mainAccountBalance,
                    category = "환전",
                ),
            )
        } else {
            mainAccountBalance = (mainAccountBalance - krwAmount).coerceAtLeast(0L)
            transactions.add(
                0,
                BankTransactionItem(
                    id = "tx_ex_buy_${System.currentTimeMillis()}",
                    dateLabel = nowTime,
                    title = "외화 환전 ($currency ${fmt.format(foreignAmount)})",
                    subtitle = "$branchLabel · 90% 우대",
                    amountDelta = -krwAmount,
                    balanceAfter = mainAccountBalance,
                    category = "환전",
                ),
            )
        }

        val voucherNumber = "QR-EX-2026-${(8492 + exchangeVouchers.size)}"
        val voucher =
            ExchangeVoucherItem(
                voucherCode = voucherNumber,
                currency = currency,
                foreignAmount = foreignAmount,
                krwAmount = krwAmount,
                branchLabel = branchLabel,
                pickupMethodLabel = pickupMethodLabel,
                recipientPhone = recipientPhone,
                issuedAt = nowTime,
            )
        if (!isSellBack) {
            exchangeVouchers.add(0, voucher)
        }
        return voucher
    }

    fun updateLatestVoucherPickup(
        branchLabel: String,
        pickupMethodLabel: String,
        recipientPhone: String,
    ): ExchangeVoucherItem? {
        val current = exchangeVouchers.firstOrNull() ?: return null
        val updated =
            current.copy(
                branchLabel = branchLabel.ifBlank { current.branchLabel },
                pickupMethodLabel = pickupMethodLabel.ifBlank { current.pickupMethodLabel },
                recipientPhone = recipientPhone.ifBlank { current.recipientPhone },
            )
        exchangeVouchers[0] = updated
        return updated
    }

    fun cancelExchangeVoucher(voucherCode: String? = null): ExchangeVoucherItem? {
        val target =
            if (!voucherCode.isNullOrBlank()) {
                exchangeVouchers.firstOrNull { it.voucherCode == voucherCode }
                    ?: exchangeVouchers.firstOrNull()
            } else {
                exchangeVouchers.firstOrNull()
            } ?: return null

        exchangeVouchers.remove(target)
        mainAccountBalance += target.krwAmount
        val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
        transactions.add(
            0,
            BankTransactionItem(
                id = "tx_ex_cancel_${System.currentTimeMillis()}",
                dateLabel = currentTimeLabel(),
                title = "환전 신청 취소 환불 (${target.currency} ${fmt.format(target.foreignAmount)})",
                subtitle = "${target.voucherCode} 바우처 취소 · 전액 환불",
                amountDelta = +target.krwAmount,
                balanceAfter = mainAccountBalance,
                category = "환전취소",
            ),
        )
        return target
    }

    fun recordTransfer(
        recipient: String,
        amount: Long,
        memo: String,
        fromSalaryAccount: Boolean = true,
    ) {
        val cleanRecipient = recipient.substringBefore(" (").trim().ifBlank { "이지훈" }
        val nowTime = currentTimeLabel()
        if (fromSalaryAccount) {
            mainAccountBalance = (mainAccountBalance - amount).coerceAtLeast(0L)
        } else {
            livingAccountBalance = (livingAccountBalance - amount).coerceAtLeast(0L)
        }
        transactions.add(
            0,
            BankTransactionItem(
                id = "tx_tr_${System.currentTimeMillis()}",
                dateLabel = nowTime,
                title = cleanRecipient,
                subtitle = "${memo.ifBlank { "간편송금" }} · 수수료 면제",
                amountDelta = -amount,
                balanceAfter = mainAccountBalance,
                category = "송금",
            ),
        )
    }

    fun recordCardBillPayment(
        payAmount: Long,
        usePoints: Boolean,
    ): Long {
        val pointsUsed = if (usePoints && rewardPoints >= 10_000L && payAmount > 10_000L) 10_000L else 0L
        val actualCashDeducted = (payAmount - pointsUsed).coerceAtLeast(0L)
        if (pointsUsed > 0L) {
            rewardPoints = (rewardPoints - pointsUsed).coerceAtLeast(0L)
        }
        mainAccountBalance = (mainAccountBalance - actualCashDeducted).coerceAtLeast(0L)
        cardStatementBalance = (cardStatementBalance - payAmount).coerceAtLeast(0L)

        transactions.add(
            0,
            BankTransactionItem(
                id = "tx_card_${System.currentTimeMillis()}",
                dateLabel = currentTimeLabel(),
                title = "11월 카드명세서 즉시결제",
                subtitle = if (pointsUsed > 0L) "포인트 10,000P 차감 적용" else "DK 주거래 통장 출금",
                amountDelta = -actualCashDeducted,
                balanceAfter = mainAccountBalance,
                category = "카드결제",
            ),
        )
        return actualCashDeducted
    }

    fun recordTravelBundle(
        jpyAmount: Long,
        krwAmount: Long,
        dccBlocked: Boolean,
        insPlanLabel: String,
        insFee: Long,
    ) {
        val totalDeducted = krwAmount + insFee
        mainAccountBalance = (mainAccountBalance - totalDeducted).coerceAtLeast(0L)
        val nowTime = currentTimeLabel()
        val fmt = NumberFormat.getNumberInstance(Locale.KOREA)

        transactions.add(
            0,
            BankTransactionItem(
                id = "tx_bundle_${System.currentTimeMillis()}",
                dateLabel = nowTime,
                title = "도쿄 출국 3종 패키지",
                subtitle = "JPY ${fmt.format(jpyAmount)} 환전 + 해외여행자보험($insPlanLabel)",
                amountDelta = -totalDeducted,
                balanceAfter = mainAccountBalance,
                category = "해외여행",
            ),
        )

        val voucherNumber = "QR-EX-2026-${(8492 + exchangeVouchers.size)}"
        exchangeVouchers.add(
            0,
            ExchangeVoucherItem(
                voucherCode = voucherNumber,
                currency = "JPY",
                foreignAmount = jpyAmount,
                krwAmount = krwAmount,
                branchLabel = "인천공항 1터미널",
                pickupMethodLabel = "모바일 QR 바우처 (출국 3종)",
                recipientPhone = "010-2026-8492",
                issuedAt = nowTime,
            ),
        )

        activeTravelBundle =
            TravelBundleStatus(
                destination = "도쿄 (일본)",
                jpyAmount = jpyAmount,
                dccBlocked = dccBlocked,
                insurancePlanLabel = insPlanLabel,
                activatedAt = nowTime,
            )
    }

    private fun currentTimeLabel(): String {
        val sdf = SimpleDateFormat("MM.dd HH:mm", Locale.KOREA)
        return sdf.format(Date())
    }

    companion object {
        const val INITIAL_MAIN_BALANCE = 31_520_000L
        const val INITIAL_LIVING_BALANCE = 623_540L
        const val INITIAL_CARD_STATEMENT = 2_161_314L
        const val INITIAL_REWARD_POINTS = 10_000L
    }
}

val LocalBankingAppState = compositionLocalOf { BankingAppState() }

@Composable
fun rememberBankingAppState(): BankingAppState = remember { BankingAppState() }
