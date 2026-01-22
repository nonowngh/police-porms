package mb.fw.atb.job.file;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.util.PathComparatorUtil;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Component()
public class DirectoryPolling {

    /**
     * 송신파일 목록을 조회한다.
     *
     * @param txid
     * @param context
     * @return
     * @throws Exception
     */
    @TimeTrace
    public List<Path> fileSendPoll(String txid, IFContext context) throws Exception {
        Path sendPath = null;
        Path errorPath = null;
        Path tempPath = null;
        Path complete = null;

        try {
            String fileSendPath = context.getFileSendPath();
            String fileErrorPath = context.getFileErrorPath();
            String fileTempPath = context.getFileTempPath();
            String fileCompletePath = context.getFileCompletePath();

            sendPath = Paths.get(fileSendPath);
            errorPath = Paths.get(fileErrorPath);
            tempPath = Paths.get(fileTempPath);
            complete = Paths.get(fileCompletePath);
            log.info("SEND_PATH ==> {}", sendPath);
            checkAndCreateDirectories(errorPath);
            checkAndCreateDirectories(tempPath);
            checkAndCreateDirectories(complete);

        } catch (Exception e) {
            String errorMsg = "[" + txid + "] FILE " + context.getInterfaceId() + " DIRECTORIES CHECK ERROR";
            log.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }

        int detectCount = 0;

        List<Path> pollList = Lists.newArrayList();

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(sendPath, context.getFileGlobPattern())) {

            Stream<Path> targetStream = StreamSupport
                    .stream(Spliterators.spliteratorUnknownSize(directoryStream.iterator(), Spliterator.ORDERED), false);

            log.debug("SEND PATH SORT START");

            targetStream = targetStream.sorted(PathComparatorUtil.createLastModifiedTime());

            log.debug("SEND PATH SORT END");

            Iterator<Path> iterator = targetStream.iterator();

            while (iterator.hasNext()) {
                if (detectCount >= context.getFileSendCount()) {
                    break;
                }

                Path path = iterator.next();

                if (Files.isRegularFile(path)) {
                    pollList.add(path);
                    detectCount++;
                    log.debug("POLLING FILE ADD : {}", path.toString());
                }
            }

        } catch (IOException e) {
            throw new IOException(e);
        } finally {
            log.info("POLLING DETECT COUNT : {}", detectCount);
            return pollList;
        }

    }

    /**
     * @param txid
     * @param context
     * @return
     * @throws Exception
     */
    @TimeTrace
    public static List<Path> fileRecvPoll(String txid, IFContext context) throws Exception {
        Path recvPath = null;
        Path errorPath = null;
        Path tempPath = null;
        Path complete = null;

        try {
            String fileRecvPath = context.getFileRecvPath();
            String fileErrorPath = context.getFileErrorPath();
            String fileTempPath = context.getFileTempPath();
            String fileCompletePath = context.getFileCompletePath();

            recvPath = Paths.get(fileRecvPath);
            errorPath = Paths.get(fileErrorPath);
            tempPath = Paths.get(fileTempPath);
            complete = Paths.get(fileCompletePath);
            log.info("[{}] RECV_PATH ==> {}", txid, recvPath);
            checkAndCreateDirectories(errorPath);
            checkAndCreateDirectories(tempPath);
            checkAndCreateDirectories(complete);

        } catch (Exception e) {
            String errorMsg = "[" + txid + "] FILE " + context.getInterfaceId() + " DIRECTORIES CHECK ERROR";
            log.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }

        int detectCount = 0;

        List<Path> pollList = Lists.newArrayList();

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(recvPath, context.getFileGlobPattern())) {

            Stream<Path> targetStream = StreamSupport
                    .stream(Spliterators.spliteratorUnknownSize(directoryStream.iterator(), Spliterator.ORDERED), false);

            log.debug("[{}] RECV PATH SORT START", txid);

            targetStream = targetStream.sorted(PathComparatorUtil.createLastModifiedTime());

            log.debug("[{}] RECV PATH SORT END", txid);

            Iterator<Path> iterator = targetStream.iterator();

            while (iterator.hasNext()) {
                if (detectCount >= context.getFileSendCount()) {
                    break;
                }

                Path path = iterator.next();

                if (Files.isRegularFile(path)) {
                    pollList.add(path);
                    detectCount++;
                    log.debug("[{}] POLLING FILE ADD : {}", txid, path.toString());
                }
            }

        } catch (IOException e) {
            throw new IOException(e);
        } finally {
            log.info("[{}] POLLING DETECT COUNT : {}", txid, detectCount);
            return pollList;
        }

    }

    public static void checkAndCreateDirectories(Path path) throws IOException {
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                return;
            } else {
                throw new IOException("REQUIRED DIRECTORY IS A FILE ==> " + path);
            }
        } else {
            Files.createDirectories(path);
        }
    }
}
