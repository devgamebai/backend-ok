/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 */
package com.gamebase.migrations;

import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseTable {
    public static void checkAndCreateTableMission(Connection connection) throws SQLException {
        String tableName;
        DatabaseMetaData dbm = connection.getMetaData();
        ResultSet tables = dbm.getTables(null, null, tableName = "mission", null);
        if (tables.next()) {
            System.out.println("Table " + tableName + " already exists.");
        } else {
            String createTableSQL = "CREATE TABLE mission (\n    id CHAR(36) PRIMARY KEY,\n    name VARCHAR(255) NOT NULL,\n    description TEXT,\n    type VARCHAR(255),\n    point BIGINT,\n    event_id INT,\n    game_id INT,\n    reward BIGINT,\n    status INT,\n    rules      text  null,\n    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP\n);";
            Statement statement = connection.createStatement();
            statement.execute(createTableSQL);
            System.out.println("Table " + tableName + " created.");
        }
        tables.close();
    }

    public static void checkAndCreateTableEventMission(Connection connection) throws SQLException {
        String tableName;
        DatabaseMetaData dbm = connection.getMetaData();
        ResultSet tables = dbm.getTables(null, null, tableName = "event_mission", null);
        if (tables.next()) {
            System.out.println("Table " + tableName + " already exists.");
        } else {
            String createTableSQL = "CREATE TABLE event_mission (\n    id INT PRIMARY KEY AUTO_INCREMENT,\n    name VARCHAR(255) NOT NULL,\n    content TEXT,\n    status INT,\n    `show` BOOLEAN,\n    expired_at TIMESTAMP NOT NULL,\n    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n    deleted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);";
            Statement statement = connection.createStatement();
            statement.execute(createTableSQL);
            System.out.println("Table " + tableName + " created.");
        }
        tables.close();
    }

    public static void checkAndCreateTableUserMission(Connection connection) throws SQLException {
        String tableName;
        DatabaseMetaData dbm = connection.getMetaData();
        ResultSet tables = dbm.getTables(null, null, tableName = "user_mission", null);
        if (tables.next()) {
            System.out.println("Table " + tableName + " already exists.");
        } else {
            String createTableSQL = "CREATE TABLE user_mission (\n    id INT PRIMARY KEY AUTO_INCREMENT,\n    event_id INT,\n    user_id INT,\n    nickname VARCHAR(255),\n    mission_id VARCHAR(255),\n    progress INT,\n    point BIGINT,\n    work BIGINT,\n    status VARCHAR(255),\n    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n    updated_at timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,\n    uid        varchar(200)                        not null,\n    rules      text                                null,\n    extra      text                                null,\n    constraint uid_user_mission_pk\n        unique (uid));";
            Statement statement = connection.createStatement();
            statement.execute(createTableSQL);
            System.out.println("Table " + tableName + " created.");
        }
        tables.close();
    }

    public static void migration() {
        try (Connection connection = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            DatabaseTable.checkAndCreateTableEventMission(connection);
            DatabaseTable.checkAndCreateTableUserMission(connection);
            DatabaseTable.checkAndCreateTableMission(connection);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

