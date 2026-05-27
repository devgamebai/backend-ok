package com.sunwinkr.minigame.api.adapter;

import com.sunwinkr.minigame.engine.port.MoneyResult;
import com.sunwinkr.minigame.engine.port.TransKind;
import com.vinplay.usercore.service.UserService;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.response.MoneyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for JdbcWalletPort.
 * Verifies debit negates amount, credit passes positive, and TransKind maps correctly.
 *
 * Plan §4.1.
 */
@ExtendWith(MockitoExtension.class)
class JdbcWalletPortTest {

    @Mock UserService userService;
    JdbcWalletPort port;

    @BeforeEach
    void setUp() {
        port = new JdbcWalletPort(userService);
    }

    @Test
    void debit_negatesAmount_andCallsUpdateMoney() throws Exception {
        MoneyResponse resp = new MoneyResponse(true, "0");
        resp.setCurrentMoney(49000L);
        when(userService.updateMoney(anyString(), anyLong(), anyString(),
            anyString(), anyString(), anyString(), anyLong(), anyLong(), any(TransType.class)))
            .thenReturn(resp);

        MoneyResult result = port.debit("player1", 1000L, "vin", "TaiXiu",
            2L, "bet", 0L, 99L, TransKind.START);

        ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
        verify(userService).updateMoney(anyString(), amountCaptor.capture(), anyString(),
            anyString(), anyString(), anyString(), anyLong(), anyLong(), any(TransType.class));

        assertThat(amountCaptor.getValue()).isEqualTo(-1000L);  // negated for debit
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCurrentMoney()).isEqualTo(49000L);
    }

    @Test
    void credit_passesPositiveAmount() throws Exception {
        MoneyResponse resp = new MoneyResponse(true, "0");
        resp.setCurrentMoney(51000L);
        when(userService.updateMoney(anyString(), anyLong(), anyString(),
            anyString(), anyString(), anyString(), anyLong(), anyLong(), any(TransType.class)))
            .thenReturn(resp);

        MoneyResult result = port.credit("player1", 1000L, "vin", "TaiXiu",
            2L, "prize", 0L, 100L, TransKind.END);

        ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
        verify(userService).updateMoney(anyString(), amountCaptor.capture(), anyString(),
            anyString(), anyString(), anyString(), anyLong(), anyLong(), any(TransType.class));

        assertThat(amountCaptor.getValue()).isEqualTo(1000L);  // positive for credit
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void mapTransType_start_mapsToStartTrans() {
        assertThat(JdbcWalletPort.mapTransType(TransKind.START)).isEqualTo(TransType.START_TRANS);
    }

    @Test
    void mapTransType_in_mapsToInTrans() {
        assertThat(JdbcWalletPort.mapTransType(TransKind.IN)).isEqualTo(TransType.IN_TRANS);
    }

    @Test
    void mapTransType_end_mapsToEndTrans() {
        assertThat(JdbcWalletPort.mapTransType(TransKind.END)).isEqualTo(TransType.END_TRANS);
    }
}
