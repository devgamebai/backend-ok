/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.rabbitmq.client.AMQP
 *  com.rabbitmq.client.AMQP$BasicProperties
 *  com.rabbitmq.client.Channel
 */
package com.vinplay.vbee.common.rmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.rmq.RMQTask;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

public class RMQPublishTask
extends RMQTask {
    private BaseMessage message;
    private int command;

    public RMQPublishTask(BaseMessage message, String queueName, int command) {
        super(queueName);
        this.message = message;
        this.command = command;
    }

    @Override
    public void run(Channel channel) throws IOException {
        channel.basicPublish("", this.queueName, new AMQP.BasicProperties("text/plain", "UTF-8", null, Integer.valueOf(2), Integer.valueOf(0), null, null, null, String.valueOf(this.command), null, null, null, null, null), this.message.toBytes());
    }
}

