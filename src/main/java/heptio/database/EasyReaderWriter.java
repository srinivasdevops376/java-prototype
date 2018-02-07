package heptio.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;


public class EasyReaderWriter {


    private static final String mysqlConnectionString = System.getenv("JAVAPROTOTYPE_MYSQL_CONNECTION_STRING");
    private static final String mysqlConnectionUser = System.getenv("JAVAPROTOTYPE_MYSQL_CONNECTION_USER");
    private static final String mysqlConnectionPass = System.getenv("JAVAPROTOTYPE_MYSQL_CONNECTION_PASS");
    private static final Logger logger = LoggerFactory.getLogger(EasyReaderWriter.class);
    private static EasyReaderWriter ourInstance = new EasyReaderWriter();
    private Connection connection = null;

    public static EasyReaderWriter getInstance() {
        return ourInstance;
    }


    private EasyReaderWriter() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            logger.warn("Unable to load MySQL driver");
            e.printStackTrace();
            return;
        }
        logger.info("MySQL JDBC driver registered");
        try {
            this.connection = DriverManager
                    .getConnection(EasyReaderWriter.mysqlConnectionString, EasyReaderWriter.mysqlConnectionUser, EasyReaderWriter.mysqlConnectionPass);

        } catch (SQLException e) {
            logger.warn("Connection to MySQL failed!");
            e.printStackTrace();
            return;
        }
        if (this.connection != null) {
            logger.info("Successfully connected to MySQL server");
        } else {
            logger.error("Unable to connect to MySQL server");
        }
    }


    public void write(String data) {
        logger.info("Writing data");
        logger.info(data);
        Statement statement;
        try {
            statement = this.connection.createStatement();
        } catch (SQLException e) {
            logger.error("Unable to create statement object for MySQL: "+e.getMessage());
            return;
        }
        java.util.Date dt = new java.util.Date();
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(dt);
        try {
            statement.executeUpdate("INSERT INTO getrequests(timestamp, hash) VALUES ('" + currentTime + "', '" + data + "');");
        } catch (SQLException e) {
            logger.error("Unable to query data: " + e.getMessage());
            return;
        }
    }
}
