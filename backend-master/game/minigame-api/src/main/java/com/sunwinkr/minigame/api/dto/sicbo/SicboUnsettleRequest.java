package com.sunwinkr.minigame.api.dto.sicbo;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Request body for {@code POST /api/v2/admin/sicbo/unsettle} (SUN-1339 §B2).
 *
 * <pre>{@code
 * {
 *   "ticketId": 12345,
 *   "reason":   "duplicate settle — admin correction"
 * }
 * }</pre>
 */
public class SicboUnsettleRequest {

    /** Primary key of the {@code sicbo_bet} row to void. */
    @NotNull(message = "ticketId is required")
    public Long ticketId;

    /** Operator reason for the void — written to the audit log. */
    @NotBlank(message = "reason is required")
    public String reason;
}
