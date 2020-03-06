package UtilsTests;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import proto.types.block;
import utils.DiskUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.*;

public class DiskUtilsTest {
    Path storagePath = Paths.get("/tmp/test");

    @Test
    @DisplayName("test success of create and remove storage directory")
    public void testCreateAndRemoveStorageDir() {
        DiskUtils.createStorageDir(storagePath);
        DiskUtils.removeStorageDir(storagePath);
    }

    @Test(expected = Test.None.class)
    @DisplayName("test writing block to storage")
    public void testCutBlock() throws IOException {
        DiskUtils.createStorageDir(storagePath);
        DiskUtils.cutBlock(block.Block.getDefaultInstance(), storagePath);
        DiskUtils.removeStorageDir(storagePath);
    }

    @Test(expected = Test.None.class)
    @DisplayName("test get block to storage")
    public void testGetBlock() throws IOException {
        block.Block b = block.Block.getDefaultInstance();
        DiskUtils.createStorageDir(storagePath);
        DiskUtils.cutBlock(b, storagePath);
        block.Block b1 = DiskUtils.getBlockFromFile(b.getHeader().getHeight(), storagePath);
        DiskUtils.removeStorageDir(storagePath);
        assertEquals(b1, b);
    }

    @Test(expected = IOException.class)
    @DisplayName("test delete block to storage")
    public void testDeletetBlock() throws IOException {
        block.Block b = block.Block.getDefaultInstance();
        DiskUtils.createStorageDir(storagePath);
        DiskUtils.cutBlock(b, storagePath);
        DiskUtils.deleteBlockFile(b.getHeader().getHeight(), storagePath);
        DiskUtils.getBlockFromFile(b.getHeader().getHeight(), storagePath);
        DiskUtils.removeStorageDir(storagePath);
    }

    @Test(expected = Test.None.class)
    @DisplayName("test is block exist in storage")
    public void testIsBlockExist() throws IOException {
        block.Block b = block.Block.getDefaultInstance();
        DiskUtils.createStorageDir(storagePath);
        DiskUtils.cutBlock(b, storagePath);
        assertTrue(DiskUtils.isBlockExistInStorage(b.getHeader().getHeight(), storagePath));

        DiskUtils.deleteBlockFile(b.getHeader().getHeight(), storagePath);
        assertFalse(DiskUtils.isBlockExistInStorage(b.getHeader().getHeight(), storagePath));
        DiskUtils.removeStorageDir(storagePath);

    }

    @Test(expected = Test.None.class)
    @DisplayName("test writing block async to storage")
    public void testCutBlockAsync() throws IOException, InterruptedException {
        block.Block b = block.Block.getDefaultInstance();
        DiskUtils.createStorageDir(storagePath);
        DiskUtils.cutBlockAsync(b, storagePath);
        DiskUtils.cutBlock(b, storagePath);
        int tries = 0;
        while (!DiskUtils.isBlockExistInStorage(b.getHeader().getHeight(), storagePath) && tries < 10) {
            TimeUnit.SECONDS.sleep(1);
            tries++;
        }
        assertTrue(DiskUtils.isBlockExistInStorage(b.getHeader().getHeight(), storagePath));
        DiskUtils.removeStorageDir(storagePath);

    }
}
