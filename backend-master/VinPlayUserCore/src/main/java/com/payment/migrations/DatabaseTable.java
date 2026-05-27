/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 */
package com.payment.migrations;

import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseTable {
    public static void checkAndCreateTableHistoryBank(Connection connection) throws SQLException {
        String table;
        DatabaseMetaData dbm = connection.getMetaData();
        ResultSet tables = dbm.getTables(null, null, table = "history_bank", null);
        if (tables.next()) {
            System.out.println("Table " + table + " already exists.");
        } else {
            String createTableSQL = "create table history_bank(\n    id        int auto_increment primary key,\n    fid       text             not null,\n    key_id    text             not null,\n    nick_name text             not null,\n    cash      int              not null,\n    cash_real int              not null,\n    type      text             not null,\n    text      text             not null,\n    status    int              not null,\n    day       tinyint          not null,\n    month     tinyint          not null,\n    year      smallint         not null,\n    time      int              not null,\n    number    int(1) default 0 null\n)\n    collate = utf8_unicode_ci;";
            Statement statement = connection.createStatement();
            statement.execute(createTableSQL);
            System.out.println("Table " + table + " created.");
        }
        tables.close();
    }

    public static void checkAndCreateTableHistoryApplyFor(Connection connection) throws SQLException {
        String table;
        DatabaseMetaData dbm = connection.getMetaData();
        ResultSet tables = dbm.getTables(null, null, table = "history_applyfor", null);
        if (tables.next()) {
            System.out.println("Table " + table + " already exists.");
        } else {
            String createTableSQL = "create table history_applyfor(\n    id        int auto_increment primary key,\n    fid       text     not null,\n    key_id    text     not null,\n    nick_name text     not null,\n    cash      int      not null,\n    cash_real int      not null,\n    type      text     not null,\n    text      text     not null,\n    status    int      not null,\n    day       tinyint  not null,\n    month     tinyint  not null,\n    year      smallint not null,\n    time      int      not null\n)\n    collate = utf8_unicode_ci;\n\n";
            Statement statement = connection.createStatement();
            statement.execute(createTableSQL);
            System.out.println("Table " + table + " created.");
        }
        tables.close();
    }

    public static void checkAndCreateTableTopup(Connection connection) throws SQLException {
        String table;
        DatabaseMetaData dbm = connection.getMetaData();
        ResultSet tables = dbm.getTables(null, null, table = "topup", null);
        if (tables.next()) {
            System.out.println("Table " + table + " already exists.");
        } else {
            String createTableSQL = "create table topup (\n    id        int auto_increment\n        primary key,\n    fid       text     not null,\n    key_id    text     not null,\n    nick_name text     not null,\n    serial    text     not null,\n    code      text     not null,\n    cash      int      not null,\n    cash_real int      not null,\n    type      text     not null,\n    text      text     not null,\n    status    int      not null,\n    day       tinyint  not null,\n    month     tinyint  not null,\n    year      smallint not null,\n    time      int      not null\n)\n    collate = utf8_unicode_ci;";
            Statement statement = connection.createStatement();
            statement.execute(createTableSQL);
            System.out.println("Table " + table + " created.");
        }
        tables.close();
    }

    public static void migration() {
        try (Connection connection = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            DatabaseTable.checkAndCreateTableHistoryBank(connection);
            DatabaseTable.checkAndCreateTableHistoryApplyFor(connection);
            DatabaseTable.checkAndCreateTableTopup(connection);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

