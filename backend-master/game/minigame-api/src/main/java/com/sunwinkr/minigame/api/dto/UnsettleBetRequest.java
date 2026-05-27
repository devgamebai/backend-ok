package com.sunwinkr.minigame.api.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Request body for {@code POST /api/v2/admin/taixiu/unsettle}.
 *
 * <p>Plan SUN-1339 §B2.
 */
public final class UnsettleBetRequest {

    /** Primary key of the {@code taixiu_bet} row to reverse. */
    @NotNull
    @Min(1)
    public Long ticketId;

    /** Mandatory reason string written to the audit log. */
    @NotNull
    public String reason;
}
