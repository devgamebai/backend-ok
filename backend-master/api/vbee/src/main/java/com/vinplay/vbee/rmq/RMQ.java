/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.rmq.RMQConnectionFactory
 *  org.apache.log4j.Logger
 */
package com.vinplay.vbee.rmq;

import com.vinplay.vbee.common.config.VBeePath;
import com.vinplay.vbee.common.rmq.RMQConnectionFactory;
import com.vinplay.vbee.rmq.RMQConsumer;
import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RMQ {
    private static final Logger logger = Logger.getLogger((String)"vbee");
    private static String basePath = VBeePath.basePath;

    public static void start() {
        logger.info((Object)"STARTING RABBITMQ ...!!!");
        try {
            File file = new File(basePath.concat("config/rabbitmq_config.xml"));
            DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();
            System.out.println(doc.getDocumentElement().getNodeName());
            NodeList nodeList = doc.getElementsByTagName("rabbitmq");
            Element el = (Element)nodeList.item(0);
            RMQConnectionFactory.USERNAME = el.getElementsByTagName("user").item(0).getTextContent();
            RMQConnectionFactory.PASSWORD = el.getElementsByTagName("pass").item(0).getTextContent();
            RMQConnectionFactory.SERVER_ADR = el.getElementsByTagName("server_addr").item(0).getTextContent();
            RMQConnectionFactory.SERVER_PORT = Integer.parseInt(el.getElementsByTagName("port").item(0).getTextContent());
            RMQConnectionFactory.RECONNECT_DELAY = Integer.parseInt(el.getElementsByTagName("reconnect_delay").item(0).getTextContent());
            RMQConnectionFactory.CONNECTION_TIMEOUT = Integer.parseInt(el.getElementsByTagName("connection_timeout").item(0).getTextContent());
            RMQConnectionFactory.HANDSHAKE_TIMEOUT = Integer.parseInt(el.getElementsByTagName("handshake_timeout").item(0).getTextContent());
            System.out.println("user: " + el.getElementsByTagName("user").item(0).getTextContent());
            Element queues = (Element)el.getElementsByTagName("queues").item(0);
            NodeList queueList = queues.getElementsByTagName("queue");
            for (int i = 0; i < queueList.getLength(); ++i) {
                RMQ.startConsumer((Element)queueList.item(i));
            }
        }
        catch (Exception e) {
            logger.error((Object)"Start RabbitMQ error: ", (Throwable)e);
        }
        logger.info((Object)"RABBITMQ Started ...!!!");
    }

    private static void startConsumer(Element config) {
        String queueName = config.getElementsByTagName("name").item(0).getTextContent();
        int numThreads = Integer.parseInt(config.getElementsByTagName("threads").item(0).getTextContent());

        // Parse optional prefetch count (defaults to 20 in RMQConsumer)
        int prefetchCount = -1;
        NodeList prefetchNodes = config.getElementsByTagName("prefetch");
        if (prefetchNodes.getLength() > 0) {
            try {
                prefetchCount = Integer.parseInt(prefetchNodes.item(0).getTextContent());
            } catch (NumberFormatException e) {
                logger.warn((Object)("Invalid prefetch for " + queueName + ", using default"));
            }
        }

        Element cmds = (Element)config.getElementsByTagName("commands").item(0);
        NodeList cmdList = cmds.getElementsByTagName("command");
        HashMap<Integer, String> commandsMap = new HashMap<Integer, String>();
        for (int i = 0; i < cmdList.getLength(); ++i) {
            Element eCmd = (Element)cmdList.item(i);
            Integer id = Integer.parseInt(eCmd.getElementsByTagName("id").item(0).getTextContent());
            String path = eCmd.getElementsByTagName("path").item(0).getTextContent().trim();
            commandsMap.put(id, path);
        }

        RMQConsumer consumer;
        if (prefetchCount > 0) {
            consumer = new RMQConsumer(queueName, numThreads, prefetchCount);
        } else {
            consumer = new RMQConsumer(queueName, numThreads);
        }
        consumer.start(commandsMap);
        logger.info((Object)("Start " + numThreads + " " + queueName
                + (prefetchCount > 0 ? " prefetch=" + prefetchCount : "")
                + ", num commands= " + commandsMap.size() + " success"));
    }

}

