package com.sunwinkr.lottery.api.dto;

import javax.validation.constraints.NotNull;

/**
 * Wire DTO for {@code POST /api/v2/lottery/admin/xsmb/settle} body.
 *
 * <p>{@code date} is the Vietnam-wall draw date to settle, ISO-8601
 * ({@code yyyy-MM-dd}).
 */
public final class AdminSettleRequestDto {

    @NotNull
    public String date;

    /** Dual-control acknowledgement — second admin's confirmation. */
    public String secondApproverNickname;
}
