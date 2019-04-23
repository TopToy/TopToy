package utils;
import org.apache.commons.io.FileUtils;
import proto.Types;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.lang.String.format;

public class DiskUtils {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DiskUtils.class);
    static private ExecutorService worker = Executors.newSingleThreadExecutor();


    //    static Path path;
//    public DiskUtils(Path pathToDir) {
//        path = pathToDir;
//        File dir = new File(pathToDir.toString());
//        try {
//            if (dir.exists()) {
//                FileUtils.forceDelete(dir);
//            }
//            FileUtils.forceMkdir(dir);
//        } catch (IOException e) {
//            logger.error("", e);
//        }
//
//    }

    static public void shutdown() {
//        if (worker.isShutdown()) return;
        worker.shutdownNow();
    }

    public static void createStorageDir(Path pathToDir) {
        try {
//            Files.deleteIfExists(pathToDir);
            File dir = new File(pathToDir.toString());
            if (dir.exists()) {
                FileUtils.forceDelete(dir);
            }
            Files.createDirectories(pathToDir);
        } catch (IOException e) {
            logger.error(e);
        }
//        File dir = new File(pathToDir.toString());
//        try {
//            if (dir.exists()) {
//                FileUtils.forceDelete(dir);
//            }
//            FileUtils.forceMkdir(dir);
//        } catch (IOException e) {
//            logger.error("", e);
//        }
    }

    public static void cutBlock(Types.Block b, Path path) throws IOException {
        Path blockFile = Paths.get(path.toString(), String.valueOf(b.getHeader().getHeight()));
//        Files.createFile(blockFile);
        Files.write(blockFile, b.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
//        File f = new File(blockFile.toString());
//        RandomAccessFile f = new RandomAccessFile(blockFile.toString(), "rw");

//        try (FileChannel channel = f.getChannel()) {
//
//        }
//        File f = new File(blockFile.toString());
//        f.createNewFile();
//        try (FileOutputStream output = new FileOutputStream(blockFile.toString())) {
//            b.writeDelimitedTo(output);
//        }
    }

    public static Types.Block getBlockFromFile(int height, Path path) throws IOException {
        Path blockFile = Paths.get(path.toString(), String.valueOf(height));
        return Types.Block.parseFrom(Files.readAllBytes(blockFile));
//        try (FileInputStream input = new FileInputStream(blockFile.toString())) {
//            return Types.Block.parseDelimitedFrom((input));
//        }
    }

    public static void deleteBlockFile(int height, Path path) throws IOException {
        Path blockFile = Paths.get(path.toString(), String.valueOf(height));
        Files.deleteIfExists(blockFile);
//        File f = new File(blockFile.toString());
//        if (f.exists()) {
//            FileUtils.forceDelete(f);
//        }

    }
    public static Future cutBlockAsync(Types.Block b, Path path) {
        return worker.submit(() -> {
            try {
                cutBlock(b, path);
                logger.debug(format("have write to Disk (2) [c=%d, h=%d, pid=%d, bid=%d",
                        b.getHeader().getM().getChannel(), b.getHeader().getHeight(), b.getHeader().getBid().getPid(),
                        b.getHeader().getBid().getBid()));
            } catch (IOException e) {
                logger.error("", e);
            }
        });
    }
}
