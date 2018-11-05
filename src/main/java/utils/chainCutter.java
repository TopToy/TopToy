package utils;
import org.apache.commons.io.FileUtils;
import proto.Types;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class chainCutter {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(chainCutter.class);
    static Path path;
    public chainCutter(Path pathToDir) {
        path = pathToDir;
        File dir = new File(pathToDir.toString());
        try {
            FileUtils.forceDelete(dir);
            FileUtils.forceMkdir(dir);
        } catch (IOException e) {
            logger.error("", e);
        }

    }
    public static void cut(List<Types.Block> blocks) throws IOException {
        for (Types.Block b : blocks) {
            cutBlock(b);
        }
    }

    static void cutBlock(Types.Block b) throws IOException {
        Path blockFile = Paths.get(path.toString(), String.valueOf(b.getHeader().getHeight()));
        File f = new File(blockFile.toString());
        f.createNewFile();
        try (FileOutputStream output = new FileOutputStream(blockFile.toString())) {
            b.writeDelimitedTo(output);
        }
        b.getDataList().clear();
    }

    public static Types.Block getBlockFromFile(int height) throws IOException {
        Path blockFile = Paths.get(path.toString(), String.valueOf(height));
        Types.Block.Builder b = Types.Block.newBuilder();
        try (FileInputStream input = new FileInputStream(blockFile.toString())) {
            return Types.Block.parseDelimitedFrom((input));
        }
    }
}
