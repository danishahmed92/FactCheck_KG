package config;

import java.sql.*;

public class Database {

    public static Database databaseInstance;
    static {
        try {
            databaseInstance = new Database();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection conn;

    public Database() throws SQLException {
        String dbHost = Config.configInstance.dbHost;
        String dbPort = Config.configInstance.dbPort;
        String database = Config.configInstance.database;
        String dbUser = Config.configInstance.dbUser;
        String dbPassword = Config.configInstance.dbPassword;

        String url = String.format("jdbc:mysql://%s:%s/%s", dbHost, dbPort, database);

        conn = DriverManager.getConnection(url, dbUser, dbPassword);
    }

    public static void main(String[] args) {
        /*String dbHost = Config.configInstance.dbHost;
        String dbPort = Config.configInstance.dbPort;
        String database = Config.configInstance.database;
        String dbUser = Config.configInstance.dbUser;
        String dbPassword = Config.configInstance.dbPassword;

        String url = String.format("jdbc:mysql://%s:%s/%s", dbHost, dbPort, database);

        try {

            Connection con = DriverManager.getConnection(url, dbUser, dbPassword);
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SHOW TABLES; ");

            if (rs.next()) {
                System.out.println(rs.getString(1));
            }

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }*/
    }
}
