package utils;

import org.h2.jdbcx.JdbcDataSource;
import proto.Types;

import java.sql.*;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.String.format;

public class DBUtils {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DBUtils.class);
    static JdbcDataSource eds = null;
    static String txTableName = "BLOCKSMAP";
    static private ExecutorService worker = Executors.newSingleThreadExecutor();
    private static HashMap<Integer, Connection> rc = new HashMap<>();
    private static HashMap<Integer, Connection> wc = new HashMap<>();
    private static final Object newWriteNotifier = new Object();
    public DBUtils(int workers) {
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
        for (int i = 0; i < workers ; i++) {
            createWriteConn(i);
            createReadConn(i);

        }
    }

//    static public void shutdown(int channel) {
//        try {
//            rc.get(channel).close();
//            wc.get(channel).close();
//        } catch (SQLException e) {
//            logger.error(format("unable shutdown connection [channel=%d]", channel));
//        }
//        worker.shutdownNow();
//    }

    static public void shutdown() {

        try {
            for (Connection c : rc.values()) {
                c.close();
            }
            for (Connection c : wc.values()) {
                c.close();
            }
        } catch (SQLException e) {
            logger.error(format("Unable to shutdown connection"));
            return;
        }
        worker.shutdownNow();
//        if (rc.size() == 0 && wc.size() == 0) {

//        }
    }

    static private String getTableName(int channel) {
        return txTableName + "_" + channel;
    }
    static public void initTables(int channel) throws SQLException {
        Connection conn = wc.get(channel);
        Statement stmt = conn.createStatement();
        stmt.execute("DROP INDEX IF EXISTS id");
        stmt.execute("DROP TABLE IF EXISTS " +
                getTableName(channel));
        stmt.execute("CREATE TABLE IF NOT EXISTS " +
                getTableName(channel) +
                " (pid INTEGER not NULL, " +
                "bid INTEGER not NULL, " +
                "height INTEGER not NULL)"
                );
        stmt.execute("CREATE INDEX IF NOT EXISTS id ON " + getTableName(channel) + " (pid, bid)");
        stmt.close();
//        conn.close();
        logger.info(format("successfully created a table for [channel=%d]", channel));
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

    static private void createReadConn(int channel) {
        if (!rc.containsKey(channel)) {
            try {
                rc.put(channel, eds.getConnection());
            } catch (SQLException e) {
                logger.error(format("unable to create read connection [channel=%d]", channel));
            }
        }
    }

    static private void writeBlockToTable(int channel, int pid, int bid, int height) {
//        createWriteConn(channel);
        synchronized (newWriteNotifier) {
            try {
                Statement stmt = wc.get(channel).createStatement();
                stmt.executeUpdate(format("INSERT INTO %s VALUES(%d, %d, %d)", getTableName(channel),
                        pid, bid, height));
                stmt.close();
//            System.out.println(format("insert [%d, %d, %d]", pid, bid, height));
            } catch (SQLException e) {
                logger.error(format("unable to create statement for tx [channel=%d]", channel), e);
            }
            newWriteNotifier.notifyAll();
        }

    }

    static public void writeBlockToTable(Types.Block b) {
        int pid = b.getHeader().getBid().getPid();
        int bid = b.getId().getBid();
        int height = b.getHeader().getHeight();
        int channel = b.getHeader().getM().getChannel();
        worker.execute(() -> {
            writeBlockToTable(channel, pid, bid, height);
            logger.debug(format("have write to DB [c=%d, h=%d, pid=%d, bid=%d", channel, height, pid, bid));
        });
    }

//    static public void writeBlockToTable(int channel, int height, Types.Block b) {
//        createWriteConn(channel);
//        try {
//            Statement stmt = wc.get(channel).createStatement();
//            for (Types.Transaction tx : b.getDataList()) {
//                stmt.addBatch(format("INSERT INTO %s VALUES(%d, %d, %d)", getTableName(channel),
//                        tx.getId().getProposerID(), tx.getId().getTxNum(), bid));
//            }
//            stmt.executeBatch();
//            stmt.close();
//        } catch (SQLException e) {
//            logger.error(format("unable to create statement for block [channel=%d]", channel), e);
//        }
//    }

    static public int getBlockRecord(int worker, int pid, int bid, boolean blocking) throws InterruptedException {
        if (!blocking) return getBlockRecord(worker, pid, bid);
        int h = getBlockRecord(worker, pid, bid);
        synchronized (newWriteNotifier) {
            while (h == -1) {
                newWriteNotifier.wait();
                h = getBlockRecord(worker, pid, bid);
            }
        }
        return h;

    }

    static private int getBlockRecord(int worker, int pid, int bid) {
//        createReadConn(channel);
        try {
            Statement stmt = rc.get(worker).createStatement();
            ResultSet rs = stmt.executeQuery(format("SELECT height FROM %s WHERE pid=%d AND bid=%d",
                    getTableName(worker), pid, bid));
            if (!rs.next()) return -1;
            return rs.getInt("height");
        } catch (SQLException e) {
            logger.error(format("unable to create statement for read [channel=%d]", worker), e);
            return -1;
        }
    }
}
