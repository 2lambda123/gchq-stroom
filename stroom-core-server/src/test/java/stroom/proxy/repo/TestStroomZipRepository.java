package stroom.proxy.repo;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.feed.MetaMap;
import stroom.util.io.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

public class TestStroomZipRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestStroomZipRepository.class);

    @Test
    public void testScan() throws IOException {
        final String repoDir = FileUtil.getCanonicalPath(Files.createTempDirectory("stroom").resolve("repo1"));

        final StroomZipRepository stroomZipRepository = new StroomZipRepository(repoDir, null, true, 100);

        final StroomZipOutputStream out1 = stroomZipRepository.getStroomZipOutputStream();
        StroomZipOutputStreamUtil.addSimpleEntry(out1, new StroomZipEntry(null, "file", StroomZipFileType.Data),
                "SOME_DATA".getBytes(CharsetConstants.DEFAULT_CHARSET));
        out1.close();

        stroomZipRepository.setCount(10000000000L);

        final StroomZipOutputStream out2 = stroomZipRepository.getStroomZipOutputStream();
        StroomZipOutputStreamUtil.addSimpleEntry(out2, new StroomZipEntry(null, "file", StroomZipFileType.Data),
                "SOME_DATA".getBytes(CharsetConstants.DEFAULT_CHARSET));
        out2.close();

        stroomZipRepository.finish();

        // Re open.
        final StroomZipRepository reopenStroomZipRepository = new StroomZipRepository(repoDir, null, false, 100);

        reopenStroomZipRepository.scanRepository((min, max) -> {
            Assert.assertTrue(1L == min);
            Assert.assertTrue(10000000001L == max);
        });

        final List<Path> allZips = reopenStroomZipRepository.listAllZipFiles();
        Assert.assertEquals(2, allZips.size());
        try (final Stream<Path> stream = allZips.stream()) {
            stream.forEach(p -> reopenStroomZipRepository.delete(new StroomZipFile(p)));
        }

        Assert.assertTrue(reopenStroomZipRepository.deleteIfEmpty());
        Assert.assertFalse("Deleted REPO", Files.isDirectory(Paths.get(repoDir)));
    }

    @Test
    public void testClean() throws IOException {
        final String repoDir = FileUtil.getCanonicalPath(Files.createTempDirectory("stroom").resolve("repo2"));

        StroomZipRepository stroomZipRepository = new StroomZipRepository(repoDir, null, false, 10000);

        final StroomZipOutputStreamImpl out1 = (StroomZipOutputStreamImpl) stroomZipRepository.getStroomZipOutputStream();
        StroomZipOutputStreamUtil.addSimpleEntry(out1, new StroomZipEntry(null, "file", StroomZipFileType.Data),
                "SOME_DATA".getBytes(CharsetConstants.DEFAULT_CHARSET));
        Assert.assertFalse(Files.isRegularFile(out1.getFile()));
        out1.close();
        Assert.assertTrue(Files.isRegularFile(out1.getFile()));

        final StroomZipOutputStreamImpl out2 = (StroomZipOutputStreamImpl) stroomZipRepository.getStroomZipOutputStream();
        StroomZipOutputStreamUtil.addSimpleEntry(out2, new StroomZipEntry(null, "file", StroomZipFileType.Data),
                "SOME_DATA".getBytes(CharsetConstants.DEFAULT_CHARSET));
        Assert.assertFalse(Files.isRegularFile(out2.getFile()));
        Assert.assertTrue(Files.isRegularFile(out2.getLockFile()));

        // Leave open

        stroomZipRepository = new StroomZipRepository(repoDir, null, false, 1000);
        Assert.assertTrue("Expecting pucker file to be left", Files.isRegularFile(out1.getFile()));
        Assert.assertTrue("Expecting lock file to not be deleted", Files.isRegularFile(out2.getLockFile()));

        final StroomZipOutputStreamImpl out3 = (StroomZipOutputStreamImpl) stroomZipRepository.getStroomZipOutputStream();
        StroomZipOutputStreamUtil.addSimpleEntry(out3, new StroomZipEntry(null, "file", StroomZipFileType.Data),
                "SOME_DATA".getBytes(CharsetConstants.DEFAULT_CHARSET));
        final Path lockFile3 = out3.getLockFile();
        Assert.assertTrue(Files.isRegularFile(lockFile3));

        stroomZipRepository.clean();
        Assert.assertTrue(Files.isRegularFile(lockFile3));

        try {
            Files.setLastModifiedTime(lockFile3, FileTime.fromMillis(System.currentTimeMillis() - (48 * 60 * 60 * 1000)));
        } catch (final Exception e) {
            Assert.fail("Unable to set LastModified");
        }
        stroomZipRepository.clean();
        Assert.assertFalse("Expecting old lock file to be deleted", Files.isRegularFile(lockFile3));
    }

    @Test
    public void testTemplatedFilename() throws IOException {
        // template should be case insensitive as far as key names go as the metamap is case insensitive
        final String repositoryFormat = "${id}_${FEED}_${key2}_${kEy1}_${Key3}";

        final String repoDir = FileUtil.getCanonicalPath(Files.createTempDirectory("stroom").resolve("repo3"));
        StroomZipRepository stroomZipRepository = new StroomZipRepository(repoDir, repositoryFormat, false, 10000);

        MetaMap metaMap = new MetaMap();
        metaMap.put("feed", "myFeed");
        metaMap.put("key1", "myKey1");
        metaMap.put("key2", "myKey2");
        metaMap.put("key3", "myKey3");

        final StroomZipOutputStreamImpl out1 = (StroomZipOutputStreamImpl) stroomZipRepository.getStroomZipOutputStream(metaMap);

        StroomZipOutputStreamUtil.addSimpleEntry(out1, new StroomZipEntry(null, "file", StroomZipFileType.Data),
                "SOME_DATA".getBytes(CharsetConstants.DEFAULT_CHARSET));
        Assert.assertFalse(Files.isRegularFile(out1.getFile()));
        out1.close();
        Path zipFile = out1.getFile();
        Assert.assertTrue(Files.isRegularFile(zipFile));
        final String expectedFilename = "001_myFeed_myKey2_myKey1_myKey3.zip";
        Assert.assertEquals(expectedFilename, zipFile.getFileName().toString());

        stroomZipRepository.scanRepository((min, max) -> {
            Assert.assertTrue(1L == min);
            Assert.assertTrue(1L == max);
        });
    }

    @Test
    public void testTemplatedFilenameWithDate() throws IOException {
        // template should be case insensitive as far as key names go as the metamap is case insensitive
        final String repositoryFormat = "${year}-${month}-${day}/${feed}/${id}";
        final String FEED_NAME = "myFeed";

        final String repoDir = FileUtil.getCanonicalPath(Files.createTempDirectory("stroom").resolve("repo3"));
        StroomZipRepository stroomZipRepository = new StroomZipRepository(repoDir, repositoryFormat, false, 10000);

        MetaMap metaMap = new MetaMap();
        metaMap.put("feed", FEED_NAME);

        final StroomZipOutputStreamImpl out1 = (StroomZipOutputStreamImpl) stroomZipRepository.getStroomZipOutputStream(metaMap);

        StroomZipOutputStreamUtil.addSimpleEntry(out1, new StroomZipEntry(null, "file", StroomZipFileType.Data),
                "SOME_DATA".getBytes(CharsetConstants.DEFAULT_CHARSET));
        Assert.assertFalse(Files.isRegularFile(out1.getFile()));
        out1.close();
        Path zipFile = out1.getFile();
        Path feedDir = zipFile.getParent();
        Path dateDir = feedDir.getParent();
        String dateDirStr = dateDir.getFileName().toString();

        Assert.assertTrue(Files.isRegularFile(zipFile));

        Assert.assertEquals(FEED_NAME, feedDir.getFileName().toString());

        String pattern = "yyyy-MM-dd";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String nowStr = simpleDateFormat.format(new Date());
        Assert.assertEquals(nowStr, dateDirStr);
    }

    @Test
    public void testInvalidDelimiter() throws IOException {
        final MetaMap metaMap = new MetaMap();
        metaMap.put("feed", "myFeed");
        metaMap.put("key1", "myKey1");

        final String repositoryFormat = "%{id}_${id}_${FEED}_${kEy1}";
        final String repoDir = FileUtil.getCanonicalPath(Files.createTempDirectory("stroom").resolve("repo3"));

        final StroomZipRepository stroomZipRepository = new StroomZipRepository(repoDir, repositoryFormat, false, 10000);
        final StroomZipOutputStreamImpl out1 = (StroomZipOutputStreamImpl) stroomZipRepository.getStroomZipOutputStream(metaMap);

        StroomZipOutputStreamUtil.addSimpleEntry(out1, new StroomZipEntry(null, "file", StroomZipFileType.Data),
                "SOME_DATA".getBytes(CharsetConstants.DEFAULT_CHARSET));
        Assert.assertFalse(Files.isRegularFile(out1.getFile()));
        out1.close();
        Path zipFile = out1.getFile();
        Assert.assertTrue(Files.isRegularFile(zipFile));
        final String expectedFilename = "__id__001_myFeed_myKey1.zip";
        Assert.assertEquals(expectedFilename, zipFile.getFileName().toString());
    }
}
