package utils;
import blockchain.BaseBlockchain;
import org.apache.commons.io.FileUtils;
import proto.Types;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class DiskUtils {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DiskUtils.class);
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
    public static void createStorageDir(Path pathToDir) {
        File dir = new File(pathToDir.toString());
        try {
            if (dir.exists()) {
                FileUtils.forceDelete(dir);
            }
            FileUtils.forceMkdir(dir);
        } catch (IOException e) {
            logger.error("", e);
        }
    }
//    public static void cut(BaseBlockchain bc, int start, int end, Path path) throws IOException {
//        for (Types.Block b : new ArrayList<>(bc.getBlocksCopy(start, end))) {
//            cutBlock(b, path);
//            bc.setBlock(b.getHeader().getHeight(), Types.Block.newBuilder()
//                    .setHeader(b.getHeader())
//                    .setSt(b.getSt())
//                    .build());
//        }
//    }

    public static void cutBlock(Types.Block b, Path path) throws IOException {
        Path blockFile = Paths.get(path.toString(), String.valueOf(b.getHeader().getHeight()));
        File f = new File(blockFile.toString());
        f.createNewFile();
        try (FileOutputStream output = new FileOutputStream(blockFile.toString())) {
            b.writeDelimitedTo(output);
        }
    }

    public static Types.Block getBlockFromFile(int height, Path path) throws IOException {
        Path blockFile = Paths.get(path.toString(), String.valueOf(height));
        Types.Block.Builder b = Types.Block.newBuilder();
        try (FileInputStream input = new FileInputStream(blockFile.toString())) {
            return Types.Block.parseDelimitedFrom((input));
        }
    }

    public static void deleteBlockFile(int height, Path path) throws IOException {
        Path blockFile = Paths.get(path.toString(), String.valueOf(height));
        File f = new File(blockFile.toString());
        if (f.exists()) {
            FileUtils.forceDelete(f);
        }
    }
}
