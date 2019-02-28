package utils;

import org.h2.jdbcx.JdbcDataSource;
import proto.Types;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.protobuf.Internal.toStringUtf8;
import static java.lang.String.format;

public class DBUtils {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DBUtils.class);
    static JdbcDataSource eds = null;
    static String txTableName = "TRANSACTIONS";
    private static HashMap<Integer, Connection> rc = new HashMap<>();
    private static HashMap<Integer, Connection> wc = new HashMap<>();
    public DBUtils() {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("", e);
            return;
        }
        eds = new JdbcDataSource();
        eds.setURL("jdbc:h2:./TDB");
        eds.setUser("sa");
        eds.setPassword("sa");
    }

    static void shutdown(int channel) {
        try {
            rc.get(channel).close();
            wc.get(channel).close();
        } catch (SQLException e) {
            logger.error(format("unable shutdown connection [channel=%d]", channel));
        }
    }
    static private String getTableName(int channel) {
        return txTableName + "_" + channel;
    }
    static public void initTables(int channel) throws SQLException {
        Connection conn = eds.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("DROP INDEX IF EXISTS id");
        stmt.execute("DROP TABLE IF EXISTS " +
                getTableName(channel));
//        stmt.execute("CREATE TABLE IF NOT EXISTS " +
//                getTableName(channel) +
//                " (pid INTEGER not NULL, " +
//                "tid BIGINT not NULL, " +
//                "cid INTEGER not NULL, " +
//                "cts BIGINT, " +
//                "sts BIGINT, " +
//                "data BINARY)");
        stmt.execute("CREATE TABLE IF NOT EXISTS " +
                getTableName(channel) +
                " (pid INTEGER not NULL, " +
                "tid BIGINT not NULL, " +
                "bid INTEGER not NULL)"
                );
        stmt.execute("CREATE INDEX IF NOT EXISTS id ON " + getTableName(channel) + " (pid, tid)");
        stmt.close();
        conn.close();
        logger.debug(format("successfully created a table for [channel=%d]", channel));
    }

    static public Connection getConnection() throws SQLException {
        return eds.getConnection();

    }

    static private void createWriteConn(int channel) {
        if (!wc.containsKey(channel)) {
            try {
                wc.put(channel, eds.getConnection());
            } catch (SQLException e) {
                logger.error(format("unable to create write connection [channel=%d]", channel));
            }
        }
    }
    static public void writeTxToTable(int channel, int bid, Types.Transaction tx) {
        createWriteConn(channel);
        try {
            Statement stmt = wc.get(channel).createStatement();
            stmt.executeUpdate(format("INSERT INTO %s VALUES(%d, %d, %d)", getTableName(channel),
                    tx.getId().getProposerID(), tx.getId().getTxNum(), bid));
            stmt.close();
        } catch (SQLException e) {
            logger.error(format("unable to create statement for tx [channel=%d]", channel), e);
        }
    }

    static public void writeBlockToTable(int channel, int bid, Types.Block b) {
        createWriteConn(channel);
        try {
            Statement stmt = wc.get(channel).createStatement();
            for (Types.Transaction tx : b.getDataList()) {
                stmt.addBatch(format("INSERT INTO %s VALUES(%d, %d, %d)", getTableName(channel),
                        tx.getId().getProposerID(), tx.getId().getTxNum(), bid));
            }
            stmt.executeBatch();
            stmt.close();
        } catch (SQLException e) {
            logger.error(format("unable to create statement for block [channel=%d]", channel), e);
        }
    }
}
