/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 */
package game.third.migrations;

import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseTable {
    public static void checkAndCreateTableUsersGame568win(Connection connection) throws SQLException {
        String tableUsersGame568win;
        DatabaseMetaData dbm = connection.getMetaData();
        ResultSet tables = dbm.getTables(null, null, tableUsersGame568win = "users_game568win", null);
        if (tables.next()) {
            System.out.println("Table users_game568win already exists.");
        } else {
            String createTableSQL = "CREATE TABLE users_game568win (\n    id INT AUTO_INCREMENT PRIMARY KEY,\n    username VARCHAR(255) NOT NULL,\n    agent VARCHAR(255),\n    userGroup VARCHAR(255),\n    displayName VARCHAR(255),\n    serverId VARCHAR(255) NOT NULL,\n    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n);";
            Statement statement = connection.createStatement();
            statement.execute(createTableSQL);
            System.out.println("Table users_game568win created.");
        }
        tables.close();
    }

    public static void migration() {
        try (Connection connection = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            DatabaseTable.checkAndCreateTableUsersGame568win(connection);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

