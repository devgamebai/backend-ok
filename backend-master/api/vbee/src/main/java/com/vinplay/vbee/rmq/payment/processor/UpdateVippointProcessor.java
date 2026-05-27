/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.VippointMessage
 *  org.apache.log4j.Logger
 */
package com.vinplay.vbee.rmq.payment.processor;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.VippointMessage;
import com.vinplay.vbee.dao.impl.UserDaoImpl;
import org.apache.log4j.Logger;

public class UpdateVippointProcessor
implements BaseProcessor<byte[], Boolean> {
    private static final Logger logger = Logger.getLogger((String)"vbee");

    public Boolean execute(Param<byte[]> param) {
        // SUN-842 follow-up: this method used to `return false` unconditionally
        // even on success, which made the RMQ framework NACK *every* VipPoint
        // update → 3 retries → DLQ. The shared queue_payment consumer pool
        // (100 threads) was burning ~1300 cycles per minute on these false
        // negatives, starving the real cmd=16 (UpdateMoneyProcessor) bet
        // writes and amplifying the DB-vs-cache lag QC reported.
        //
        // Fix: ack on success, nack on real failure, and *log* the exception
        // (was logger.debug — invisible at default INFO level).
        try {
            byte[] body = (byte[])param.get();
            VippointMessage message = (VippointMessage)BaseMessage.fromBytes((byte[])body);
            UserDaoImpl dao = new UserDaoImpl();
            boolean ok = dao.updateVP(message);
            // Ack regardless — a VP update that affects 0 rows (unknown user id,
            // no-op delta) is NOT retryable. Retrying produces the same 0-row
            // result, just burns 3 consumer slots per message for nothing,
            // which is exactly the bug that starved cmd=16 and caused the
            // DB-vs-cache lag QC reported. Log below-threshold failures for
            // observability but return TRUE so RMQ acks and frees the thread.
            if (!ok) {
                logger.warn("UpdateVippointProcessor: updateVP affected 0 rows userId="
                        + message.getUserId() + " moneyVP=" + message.getMoneyVP()
                        + " vp=" + message.getVp() + " — acking anyway (non-retryable)");
            }
            return Boolean.TRUE;
        }
        catch (Exception e) {
            // Genuine exception → log + nack so framework retries once; but also
            // fine to ack+drop if the exception is deterministic. Keep retry
            // behaviour for now and rely on DLQ to surface real bugs.
            logger.error("UpdateVippointProcessor failed (will retry)", e);
            return Boolean.FALSE;
        }
    }
}

