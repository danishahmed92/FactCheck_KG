package config;

import ml.ExtractedFeatures;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
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
    public static final String QUERY_WRITE_EXTRACTED_FEATURES_TO_DB = "INSERT INTO %s(file_name, extracted_feature_obj) VALUES (?, ?);";
    public static final String QUERY_READ_EXTRACTED_FEATURES_FROM_DB = "SELECT extracted_feature_obj FROM %s WHERE file_name = ?;";

    public Database() throws SQLException {
        String dbHost = Config.configInstance.dbHost;
        String dbPort = Config.configInstance.dbPort;
        String database = Config.configInstance.database;
        String dbUser = Config.configInstance.dbUser;
        String dbPassword = Config.configInstance.dbPassword;

        String url = String.format("jdbc:mysql://%s:%s/%s", dbHost, dbPort, database);

        conn = DriverManager.getConnection(url, dbUser, dbPassword);
    }

    public static int saveExtractedFeaturesObjToDB (ExtractedFeatures extractedFeatures, Connection conn, String tableName, String fileName) {
        int id = -1;
        try {
            String query = String.format(QUERY_WRITE_EXTRACTED_FEATURES_TO_DB, tableName);

            PreparedStatement prepareStatement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            prepareStatement.setString(1, fileName);
            prepareStatement.setObject(2, extractedFeatures);
            prepareStatement.executeUpdate();

            ResultSet rs = prepareStatement.getGeneratedKeys();
            if (rs.next())
                id = rs.getInt(1);
            rs.close();
            prepareStatement.close();

            System.out.println(String.format("Object saved in table: %s\t id: %s", tableName, id));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
    }

    public static Object getExtractedFeaturesObj() {
        String fileName = "award_00034.ttl";
        Connection conn = Database.databaseInstance.conn;
        Object deSerializedObject = null;

        String query = String.format(QUERY_READ_EXTRACTED_FEATURES_FROM_DB, "award");
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, fileName);

            ResultSet rs = pstmt.executeQuery();
            rs.next();

            byte[] buf = rs.getBytes(1);
            ObjectInputStream objectIn = null;
            if (buf != null)
                objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));

            deSerializedObject = objectIn.readObject();
            ExtractedFeatures extractedFeatures = (ExtractedFeatures) deSerializedObject;
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return deSerializedObject;
    }

    public static void main(String[] args) {
        getExtractedFeaturesObj();
    }
}
