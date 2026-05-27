package com.sunwinkr.lottery.engine.model;

/**
 * Settle lifecycle for a {@code lode} row — mirrors the DB
 * {@code ENUM('PENDING','SETTLED','VOIDED')} column added by SUN-1339 A1.
 *
 * <ul>
 *   <li>{@link #PENDING}  — bet accepted, draw result not yet applied.</li>
 *   <li>{@link #SETTLED}  — settle loop applied prize; wallet credit/confirm written.</li>
 *   <li>{@link #VOIDED}   — admin un-settled; wallet reversal written.</li>
 * </ul>
 */
public enum SettleStatus {
    PENDING,
    SETTLED,
    VOIDED
}
