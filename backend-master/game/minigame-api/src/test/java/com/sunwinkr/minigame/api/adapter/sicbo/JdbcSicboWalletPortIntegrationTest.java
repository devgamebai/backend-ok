package com.sunwinkr.minigame.api.adapter.sicbo;

import com.sunwinkr.minigame.engine.port.MoneyResult;
import com.sunwinkr.minigame.engine.port.TransKind;
import com.vinplay.usercore.service.UserService;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Confirms Sicbo wallet adapter behaviour: bots ALWAYS debit (SUN-880),
 * which is distinct from TaiXiu where bots skip the wallet path.
 *
 * <p>Adapter is dumb — the bot vs human policy lives in
 * {@code SicboBetService}. This test verifies that the adapter forwards
 * every debit call (any caller, including bots) to the legacy
 * {@code UserServiceImpl.updateMoney}; nothing in the adapter blocks
 * the call based on the caller's bot status.
 */
class JdbcSicboWalletPortIntegrationTest {

    @Test
    void debitForwardsNegativeAmountAndStartTransKind() {
        UserService stub = mock(UserService.class);
        MoneyResponse okResp = mock(MoneyResponse.class);
        when(okResp.isSuccess()).thenReturn(true);
        when(okResp.getCurrentMoney()).thenReturn(49000L);
        when(okResp.getErrorCode()).thenReturn("0");
        when(stub.updateMoney(anyString(), anyLong(), anyString(), anyString(),
            anyString(), anyString(), anyLong(), anyLong(), any(TransType.class)))
            .thenReturn(okResp);

        JdbcSicboWalletPort adapter = new JdbcSicboWalletPort(stub);
        MoneyResult r = adapter.debit("player1", 1000L, "vin", "Sicbo", 5L,
            "sicbo bet", 0L, 42L, TransKind.START);

        assertThat(r.isSuccess()).isTrue();

        ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TransType> kindCaptor = ArgumentCaptor.forClass(TransType.class);
        verify(stub).updateMoney(eq("player1"), amountCaptor.capture(),
            eq("vin"), eq("Sicbo"), eq("5"), eq("sicbo bet"),
            eq(0L), eq(42L), kindCaptor.capture());
        // Negative amount forwarded; TransKind.START maps to START_TRANS.
        assertThat(amountCaptor.getValue()).isEqualTo(-1000L);
        assertThat(kindCaptor.getValue()).isEqualTo(TransType.START_TRANS);
    }

    @Test
    void debitDoesNotSkipBots_SUN880() {
        // Distinct from TaiXiu — adapter forwards ALL debit calls regardless
        // of caller. Engine wires bot debits through SicboBetService which
        // unconditionally calls debit() per SBR:568-577 + SUN-880.
        UserService stub = mock(UserService.class);
        MoneyResponse okResp = mock(MoneyResponse.class);
        when(okResp.isSuccess()).thenReturn(true);
        when(okResp.getCurrentMoney()).thenReturn(0L);
        when(okResp.getErrorCode()).thenReturn("0");
        when(stub.updateMoney(anyString(), anyLong(), anyString(), anyString(),
            anyString(), anyString(), anyLong(), anyLong(), any(TransType.class)))
            .thenReturn(okResp);

        JdbcSicboWalletPort adapter = new JdbcSicboWalletPort(stub);
        // Call twice — once with "human" name, once with "bot" name.
        adapter.debit("player1", 1000L, "vin", "Sicbo", 5L, "d1", 0L, 1L, TransKind.START);
        adapter.debit("Sicbo_AI_bot42", 1000L, "vin", "Sicbo", 5L, "d2", 0L, 2L, TransKind.START);

        // Both forwarded — adapter doesn't filter on name.
        verify(stub, times(2)).updateMoney(anyString(), anyLong(), anyString(),
            anyString(), anyString(), anyString(), anyLong(), anyLong(), any(TransType.class));
    }

    @Test
    void creditForwardsPositiveAmountAndEndTransKind() {
        UserService stub = mock(UserService.class);
        MoneyResponse okResp = mock(MoneyResponse.class);
        when(okResp.isSuccess()).thenReturn(true);
        when(okResp.getCurrentMoney()).thenReturn(50000L);
        when(okResp.getErrorCode()).thenReturn("0");
        when(stub.updateMoney(anyString(), anyLong(), anyString(), anyString(),
            anyString(), anyString(), anyLong(), anyLong(), any(TransType.class)))
            .thenReturn(okResp);

        JdbcSicboWalletPort adapter = new JdbcSicboWalletPort(stub);
        MoneyResult r = adapter.credit("player1", 1000L, "vin", "SicboHoanTien", 5L,
            "refund", 0L, 99L, TransKind.END);

        assertThat(r.isSuccess()).isTrue();
        verify(stub).updateMoney(eq("player1"), eq(1000L), eq("vin"),
            eq("SicboHoanTien"), eq("5"), eq("refund"),
            eq(0L), eq(99L), eq(TransType.END_TRANS));
    }
}
