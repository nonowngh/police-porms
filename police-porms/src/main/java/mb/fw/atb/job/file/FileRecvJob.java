package mb.fw.atb.job.file;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.enums.TResult;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.model.file.FileInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 송신 결과를 수신하여 송신햇던 파일을 결과 처리한다
 *
 * @author clupine-meta
 */
@Slf4j
@Component
public class FileRecvJob {

    @Autowired
    IFTPJob iftpJob;

    static String separator = FileSystems.getDefault().getSeparator();

    /**
     * 결과 코드를 확인하여 (success/fail)/txid/ 파일을 이동시킨다. 그리고
     * 오류 발생시 fail txid 상위에 에러내용이 담길 파일 생성
     */
    public void resultProcessFileInfo(String txid, String resultCode, String resultMeesage, IFContext context,
                                      List<FileInfo> fileInfoList) throws IOException {

        log.info("RESULT CODE    : {}", resultCode);
        log.info("RESULT MESSAGE : {}", resultMeesage);

        if (TResult.SUCCESS.value().equals(resultCode)) {
            String successDir = context.getFileSuccessPath() + separator + txid;
            targetDirToResultPathFileInfo(txid, context, fileInfoList, successDir);
        } else if (TResult.FAIL.value().equals(resultCode)) {
            String failDir = context.getFileErrorPath() + separator + txid;
            targetDirToResultPathFileInfo(txid, context, fileInfoList, failDir);
            Path path = Paths.get(failDir + separator + txid + ".txt");
            Files.write(path, resultMeesage.getBytes());
        } else {
            log.info("UNEXPECTED RESULT CODE ==> {} ", resultCode);
        }
    }

    /**
     * 목적지 디렉토리에 파일을 이동시킨다
     *
     * @param txid
     * @param context
     * @param fileList
     * @param targetDir
     * @throws IOException
     */
    public void targetDirToResultPathFileInfo(String txid, IFContext context, List<FileInfo> fileList,
                                              String targetDir) throws IOException {
        Path existPath = Paths.get(targetDir);

        if (Files.notExists(existPath.getParent())) {
            Files.createDirectory(existPath.getParent());
        }

        //이미 존재하는 디렉토리가 있다면 삭제
        if (Files.exists(existPath)) {
            iftpJob.directorySubFilesDelete(existPath);
            Files.delete(existPath);
        }

        existPath = Files.createDirectory(existPath);

        for (FileInfo fileInfo : fileList) {
            String tempPath = context.getFileTempPath() + separator + txid + separator + fileInfo.getFileName();
            String targetPath = targetDir + separator + fileInfo.getFileName();
            Path path = Paths.get(tempPath);
            Path beforeMovePath = Paths.get(targetPath);
            Path movedPath = Files.move(path, beforeMovePath);
        }
        Files.delete(Paths.get(context.getFileTempPath() + separator + txid));
    }

    /**
     * 파일 삭제
     *
     * @param pathList
     * @throws IOException
     */
    public void remove(List<Path> pathList) throws IOException {
        for (Path path : pathList) {
            Files.deleteIfExists(path);
            log.info(path.toAbsolutePath() + " delete ");
        }
    }

    /**
     * TEMP에 있는 파일을 SAVE 위치로 옮긴다 TEMP/TXID 디렉토리는 삭제
     *
     * @param pollList
     * @param context
     * @param txid
     * @return List<Path> SAVE/TXID/ 파일 PATH
     * @throws IOException
     */
    public List<Path> saveMove(List<Path> pollList, IFContext context, String txid) throws Exception {

        List<Path> saveList = new ArrayList<Path>();

        try {
            Path savePath = Paths.get(context.getFileRecvPath());

            if (Files.notExists(savePath)) {
                savePath = Files.createDirectory(savePath);
            }

            for (Path path : pollList) {
                Path beforeMovePath = Paths.get(context.getFileRecvPath() + separator + path.getFileName());
                Path movedPath = Files.move(path, beforeMovePath, StandardCopyOption.REPLACE_EXISTING);
                log.info("MOVED SAVE FILE ==> " + movedPath);
                saveList.add(movedPath);
            }
        } catch (IOException e) {
            log.info("ERROR, FILE ROLLBACK");
            //오류나서 롤백하기위해 이미 수신디렉토리에 옮겨진 파일을 지운다.
            for (Path path : pollList) {
                Path recvPath = Paths.get(context.getFileRecvPath() + separator + path.getFileName());
                Files.deleteIfExists(recvPath);
                log.info(recvPath.toAbsolutePath() + " delete ");
            }
            throw e;
        } finally {
            Path tempPath = Paths.get(context.getFileTempPath() + separator + txid);
            Files.walk(tempPath).forEach(path -> {
                try {
                    if (Files.isRegularFile(path)) {
                        log.info(path.toAbsolutePath() + " delete ");
                        Files.deleteIfExists(path);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            Files.deleteIfExists(tempPath);
        }

        return saveList;
    }
}
