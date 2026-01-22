package mb.fw.atb.job.file;

import com.indigo.esb.nifs.IFTPClient;
import com.indigo.esb.nifs.exception.IFTPChannelCloseException;
import com.indigo.esb.nifs.exception.IFTPConnectionFailException;
import com.indigo.esb.nifs.exception.IFTPFileNotFoundException;
import com.indigo.esb.nifs.exception.IFTPRequestTimeOutException;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.model.file.FileInfo;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class IFTPJob {

    public static String separator = FileSystems.getDefault().getSeparator();

    /**
     * iftp 파일 송신후 오류가 발생하였다면 파일 원상 복구처리(송신 파일을 send로 돌려놓음)
     *
     * @param tempFiles
     * @param client
     * @param context
     * @param txid
     * @return 정상처리 될경우 List<파일명> 으로 리턴
     */
    @TimeTrace
    public List<String> putNRollback(IFTPClient client, List<Path> tempFiles, IFContext context, String txid) throws Exception {
        List<String> fileList = new ArrayList<String>();

        boolean sendResult = false;

        for (Path path : tempFiles) {
            log.debug("TRANSFER DIR : {} , FILE NAME : {}", path.getParent(), path.getFileName());
            sendResult = putFile(client, path.getParent().toString(), path.getFileName().toString(), txid,
                    context.getFileRetryCount());
            if (!sendResult) {
                break;
            } else {
                fileList.add(path.getFileName().toString());
            }
        }

        if (sendResult) {
            log.info("SEND FILES SUCCESS PROCES , COMPLETE MOVE=> {} ", context.getFileCompletePath());
            Path tempTxidDir = Paths.get(context.getFileTempPath() + separator + txid);
            Path completeTxidDir = Paths.get(context.getFileCompletePath() + separator + txid);
            //complete 폴더내 파일 삭제
            directorySubFilesDelete(completeTxidDir);
            Files.move(tempTxidDir, completeTxidDir);

        } else {
            log.error("SEND FILES ROLLBACK PROCES , POLL MOVE => {} ", context.getFileSendPath());
            for (Path sourcePath : tempFiles) {
                Path targetPath = Paths.get(context.getFileSendPath() + separator + sourcePath.getFileName().toString());
                Files.move(sourcePath, targetPath);
            }


            Path tempTxidDir = Paths.get(context.getFileTempPath() + separator + txid);
            Files.delete(tempTxidDir);
            throw new Exception("[" + txid + "] SEND FILE IFTP TRANSFER FAILURE");
        }

        return fileList;

    }


    public void directorySubFilesDelete(Path path) throws IOException {
        if (Files.exists(path)) {
            log.info("ALREADY EXIST FILE, DELETE FILES IN DIRECTORY : {}", path);
            Files.walk(path).filter(Files::isRegularFile).map(Path::toFile).forEach(file -> {
                if (!file.delete()) {
                    log.warn("Failed to delete file: {}", file.getAbsolutePath());
                }
            });
        }
    }

    /**
     * IFTP SA로 파일을 송신한다
     *
     * @param sendDir        송신파일경로
     * @param targetfileName 송신파일명
     * @param txId           트랜잭션 ID
     * @return 파일 송신 여부
     */
    private boolean putFile(IFTPClient client, String sendDir, String targetfileName, String txId, int tryCnt) {

        log.info("PUT EVENT============");
        int count = 1;

        while (tryCnt > 0) {
            try {
                log.info("TRY COUNT : {}", count++);
                client.put(txId, sendDir, targetfileName);
                break;
            } catch (IFTPConnectionFailException e) {
                log.warn("IFTPConnectionFailException : ", e);
                tryCnt--;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            } catch (IFTPFileNotFoundException e) {
                log.warn("IFTPFileNotFoundException : ", e);
                return false;
            } catch (IFTPRequestTimeOutException e) {
                log.warn("IFTPRequestTimeOutException : ", e);
                tryCnt--;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            } catch (IFTPChannelCloseException e) {
                log.warn("IFTPRequestTimeOutException : ", e);
                tryCnt--;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }

        if (tryCnt > 0) {
            return true;
        } else {
            return false;
        }

    }


    /**
     * 파일 수신 처리를  진행
     */
    @TimeTrace
    public List<Path> getFiles(IFTPClient iftpClient, List<FileInfo> fileNameList, IFContext context, String txid) throws Exception {

        List<Path> recvFileList = new ArrayList<Path>();

        boolean recvResult = false;

        String tempDir = context.getFileTempPath() + separator + txid;
        try {
            for (FileInfo fileInfo : fileNameList) {
                log.info("RECV TEMP DIR : {} , FILE NAME : {}", tempDir, fileInfo.getFileName());
                recvResult = getFile(iftpClient, fileInfo.getFileName(), txid, tempDir, context.getFileRetryCount());

                Path recvFile = Paths.get(tempDir + separator + fileInfo.getFileName());
                if (Files.exists(recvFile)) {
                    recvFileList.add(recvFile);
                } else {
                    log.error("RECEIVED FILE NOT EXIST ==> {} ", fileInfo.getFileName());
                    recvResult = false;
                }

                if (!recvResult) {
                    break;
                }
            }

            if (recvResult) {
                log.info("RECV FILES SUCCESS PROCESS => {} ", tempDir);

            } else {
                log.error("RECV FILES ROLLBACK PROCESS , TEMP FILE REMOVE", tempDir);
                throw new Exception("RECV FILE IFTP GET PROCESS FAILURE");
            }
        } catch (Exception e) {
            //수신 실패했다면 temp/txid 폴더 삭제
            Path tempPath = Paths.get(context.getFileTempPath() + separator + txid);
            Files.walk(tempPath).map(Path::toFile).forEach(File::delete);
            Files.delete(tempPath);
            throw e;
        }
        return recvFileList;
    }


    /**
     * IFTP SA 로부터 파일을 수신 받는다
     *
     * @param fileName
     * @param tx_id
     * @param tryCnt   재시도 횟수
     * @return 파일 수신 처리 여부
     */
    public boolean getFile(IFTPClient iftpClient, String fileName, String tx_id, String getPath, int tryCnt) {

        log.info("GET EVENT============");
        int count = 1;

        while (tryCnt > 0) {
            try {
                log.info("TRY COUNT : {}", count++);
                iftpClient.get(tx_id, getPath, fileName);
                break;
            } catch (IFTPConnectionFailException e) {
                log.warn("IFTPConnectionFailException : ", e);
                tryCnt--;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            } catch (IFTPFileNotFoundException e) {
                log.warn("IFTPFileNotFoundException : ", e);
                return false;
            } catch (IFTPRequestTimeOutException e) {
                log.warn("IFTPRequestTimeOutException : ", e);
                tryCnt--;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            } catch (IFTPChannelCloseException e) {
                log.warn("IFTPRequestTimeOutException : ", e);
                tryCnt--;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }

        if (tryCnt > 0) {
            return true;
        } else {
            return false;
        }

    }

    @TimeTrace
    public boolean remoteFileDelete(IFTPClient iftpClient, String fileName, String tx_id, int tryCnt) {

        log.info("DELETE EVENT============");
        int count = 1;

        while (tryCnt > 0) {
            try {
                log.info("TRY COUNT : {}", count++);
                iftpClient.remoteFileDelete(tx_id, fileName);
                break;
            } catch (IFTPConnectionFailException e) {
                log.warn("IFTPConnectionFailException : ", e);
                tryCnt--;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            } catch (IFTPRequestTimeOutException e) {
                log.warn("IFTPRequestTimeOutException : ", e);
                tryCnt--;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }

        if (tryCnt > 0) {
            return true;
        } else {
            return false;
        }

    }
}
