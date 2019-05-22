package utils;
import org.apache.commons.io.FileUtils;
import proto.Types;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.lang.String.format;

public class DiskUtils {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DiskUtils.class);
    static private ExecutorService worker = Executors.newSingleThreadExecutor();

    static public void shutdown() {
        worker.shutdownNow();
    }

    public static void createStorageDir(Path pathToDir) {
        try {
            File dir = new File(pathToDir.toString());
            if (dir.exists()) {
                FileUtils.forceDelete(dir);
            }
            Files.createDirectories(pathToDir);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public static void cutBlock(Types.Block b, Path path) throws IOException {
        Path blockFile = Paths.get(path.toString(), String.valueOf(b.getHeader().getHeight()));
        Files.write(blockFile, b.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    public static Types.Block getBlockFromFile(int height, Path path) throws IOException {
        Path blockFile = Paths.get(path.toString(), String.valueOf(height));
        return Types.Block.parseFrom(Files.readAllBytes(blockFile));
    }

    public static void deleteBlockFile(int height, Path path) throws IOException {
        Path blockFile = Paths.get(path.toString(), String.valueOf(height));
        Files.deleteIfExists(blockFile);
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
