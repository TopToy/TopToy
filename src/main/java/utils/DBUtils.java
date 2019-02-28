package utils;

import org.h2.jdbcx.JdbcDataSource;
import proto.Types;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;

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

    static private String getTableName(int channel) {
        return txTableName + "_" + channel;
    }
    static public void initTables(int channel) throws SQLException {
        Connection conn = eds.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("DROP INDEX IF EXISTS id");
        stmt.execute("DROP TABLE IF EXISTS " +
                getTableName(channel));
        stmt.execute("CREATE TABLE IF NOT EXISTS " +
                getTableName(channel) +
                " (pid INTEGER not NULL, " +
                "tid BIGINT not NULL, " +
                "cid INTEGER not NULL, " +
                "cts BIGINT, " +
                "sts BIGINT, " +
                "data BINARY)");
        stmt.execute("CREATE INDEX IF NOT EXISTS id ON " + getTableName(channel) + " (pid, cid)");
        stmt.close();
        logger.debug(format("successfully created a table for [channel=%d]", channel));
    }

    static public Connection getConnection() throws SQLException {
        return eds.getConnection();

    }

    static public void writeTxToTable(int channel, Types.Transaction tx) {
        if (!wc.containsKey(channel)) {
            try {
                wc.put(channel, eds.getConnection());
            } catch (SQLException e) {

                return;
            }
        }
        try {
            PreparedStatement stmt = wc.get(channel).prepareStatement(
                    format("INSERT INTO %s VALUES(?, ?, ?, ?, ?, ?)",
                            getTableName(channel))
            );
            stmt.setInt(1, tx.getId().getProposerID());
            stmt.setLong(2, tx.getId().getTxNum());
            stmt.setInt(3, tx.getClientID());
            stmt.setLong(4, tx.getClientTs());
            stmt.setLong(5, tx.getServerTs());
            stmt.setBytes(6, tx.getData().toByteArray());
            if (!stmt.execute()) {
                logger.debug(format("unable to add tx [%d ; %d] to [%s]", tx.getId().getProposerID(),
                        tx.getId().getTxNum(), getTableName(channel)));
            }
        } catch (SQLException e) {
            logger.error(format("unable to create statement [channel=%d]", channel));
        }
    }
}
