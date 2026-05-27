package com.sunwinkr.lottery.api.dto;

import javax.validation.constraints.NotNull;

/**
 * Wire DTO for {@code POST /api/v2/lottery/xsmb/admin/unsettle}.
 *
 * <ul>
 *   <li>{@code ticketId} — {@code lode.id} to void.</li>
 *   <li>{@code reason}   — free-text audit reason (written to wallet ledger description).</li>
 * </ul>
 */
public final class UnsettleRequestDto {

    @NotNull
    public Long ticketId;

    @NotNull
    public String reason;
}
