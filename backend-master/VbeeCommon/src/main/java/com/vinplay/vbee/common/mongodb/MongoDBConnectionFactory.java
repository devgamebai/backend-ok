/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.MongoClient
 *  com.mongodb.MongoCredential
 *  com.mongodb.ServerAddress
 *  com.mongodb.client.MongoDatabase
 */
package com.vinplay.vbee.common.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.config.VBeePath;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class MongoDBConnectionFactory {
    private static String MONGODB_HOST = "localhost";
    private static String MONGODB_DATABASE = "vinplay";
    private static String MONGODB_AUTH_DATABASE = "admin";
    private static String MONGODB_USERNAME = "vinplay";
    private static String MONGODB_PASSWORD = "vinplay@123";
    private static int MONGODB_PORT = 27017;
    private static MongoClient mongoClient;

    public static void init() throws IOException {
        Properties prop = new Properties();
        String mongoConfigPath = VBeePath.basePath.concat("config/mongo.properties");
        System.out.println("[MongoDB] Loading config from: " + mongoConfigPath + " basePath=" + VBeePath.basePath);
        FileInputStream input = new FileInputStream(mongoConfigPath);
        prop.load(input);
        MONGODB_HOST = prop.getProperty("host");
        MONGODB_DATABASE = prop.getProperty("database");
        MONGODB_AUTH_DATABASE = prop.getProperty("auth_database");
        MONGODB_PORT = Integer.parseInt(prop.getProperty("port"));
        MONGODB_USERNAME = prop.getProperty("username");
        System.out.println("[MongoDB] host=" + MONGODB_HOST + " user=" + MONGODB_USERNAME + " db=" + MONGODB_DATABASE);
        MONGODB_PASSWORD = prop.getProperty("password");
        MongoDBConnectionFactory.newConnection();
    }

    public static void newConnection() {
        MongoCredential credential = MongoCredential.createCredential((String)MONGODB_USERNAME, (String)MONGODB_AUTH_DATABASE, (char[])MONGODB_PASSWORD.toCharArray());
        // GitLab issue #1: with default MongoClientOptions the driver waits
        // serverSelectionTimeoutMS=30000 before giving up on a dead pool —
        // from the player's POV that looks like a 30-second freeze on every
        // round-start after Mongo bounces. Fail fast (5s), use a moderate
        // pool, and let the driver retry writes once before surfacing a
        // MongoException to the caller so the round loop survives blips.
        // SUN-1108/1110 hardening (2026-04-26): three log_gsc_bets writes were
        // lost in a 30-min window because of stale-pool TCP errors ("Exception
        // opening socket", "Prematurely reached end of stream"). The driver's
        // retryWrites only retries WRITE operations once the connection is up,
        // not the connection establishment itself. We:
        //   * tighten maxConnectionIdleTime 60s → 30s — recycle before any
        //     intermediate (LB, firewall) closes the socket on us
        //   * add maxConnectionLifeTime 5min — force pool churn so a single
        //     pinned connection doesn't accumulate undetected breakage
        //   * keep socketTimeout at 10s — long enough for the OS to surface
        //     a half-open socket but not so long that a hung write blocks the
        //     GSC API path
        // Application-level retry happens via MongoRetry.runWithRetry() at
        // the call site (WithdrawProcess, DepositProcess).
        MongoClientOptions options = MongoClientOptions.builder()
                .serverSelectionTimeout(5_000)     // default 30_000 → 5s
                .connectTimeout(5_000)             // default 10_000 → 5s
                .socketTimeout(10_000)             // default 0 (never) → 10s
                .maxConnectionIdleTime(30_000)     // 60s → 30s — recycle before LB/firewall idle close
                .maxConnectionLifeTime(300_000)    // 5 min hard cap on pool entries
                .heartbeatFrequency(10_000)        // probe every 10s
                .minHeartbeatFrequency(500)        // retry SDAM quickly on failure
                .connectionsPerHost(50)            // maxPoolSize = 50
                .retryWrites(true)                 // driver retries idempotent writes once
                .build();
        mongoClient = new MongoClient(
                new ServerAddress(MONGODB_HOST, MONGODB_PORT),
                Arrays.asList(new MongoCredential[]{credential}),
                options);
    }

    public static MongoDatabase getDB() {
        if (mongoClient == null) {
            MongoDBConnectionFactory.newConnection();
        }
        return mongoClient.getDatabase(MONGODB_DATABASE);
    }

    public static MongoDatabase getDB(String dbName) {
        if (mongoClient == null) {
            MongoDBConnectionFactory.newConnection();
        }
        return mongoClient.getDatabase(dbName);
    }

    /** Alias for getDB() — production code calls getDBSlave() for read operations */
    public static MongoDatabase getDBSlave() {
        return getDB();
    }
}

