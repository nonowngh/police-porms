package mb.fw.atb.job.file;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.enums.TResult;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 송신 결과를 수신하여 송신햇던 파일을 결과 처리한다
 *
 * @author clupine-meta
 */
@Slf4j
@Component
public class FileOnMessageResult {

    static String separator = FileSystems.getDefault().getSeparator();

    /**
     * 결과 코드를 확인하여 (success/fail)/txid/      * 결과 코드를 확인하여 (success/fail)/txid/ 파일을 이동시킨다. 그리고
     * 오류 발생시 fail txid 상위에 에러내용이 담길 파일 생성
     */
    public void resultProcessFileInfo(String txid, String resultCode, String resultMessage, IFContext context
    ) throws IOException {

        log.info("RESULT CODE    : {}", resultCode);
        log.info("RESULT MESSAGE : {}", resultMessage);

        if (TResult.SUCCESS.value().equals(resultCode)) {
            String successDir = context.getFileSuccessPath() + separator + txid;
            resultToFileMove(txid, context, successDir);
        } else if (TResult.FAIL.value().equals(resultCode)) {
            String failDir = context.getFileErrorPath() + separator + txid;
            resultToFileMove(txid, context, failDir);
            Path path = Paths.get(failDir + separator + txid + ".txt");
            Files.write(path, resultMessage.getBytes());
        } else {
            log.info("UNEXPECTED RESULT CODE ==> {} ", resultCode);
        }
    }

    public void resultToFileMove(String txid, IFContext context,
                                 String targetDir) throws IOException {
        Path existPath = Paths.get(targetDir);

        if (Files.notExists(existPath.getParent())) {
            Files.createDirectory(existPath.getParent());
        }

        if (Files.notExists(existPath)) {
            existPath = Files.createDirectory(existPath);
        }

        Files.list(Paths.get(context.getFileCompletePath() + separator + txid))
                .forEach(path -> {
                    try {
                        //20241105 대상지에 이미 존재 할경우(재전송일때) 파일을 삭제하도록 추가
                        String targetFile = targetDir + separator + path.getFileName();
                        //파일 이동전 해당파일이 존재하면 삭제한다
                        if (Files.exists(Paths.get(targetFile))) {
                            Files.delete(Paths.get(targetFile));
                        }

                        Files.move(path, Paths.get(targetFile));
                    } catch (IOException e) {
                        log.error("FILE MOVE ERROR", e);
                    }
                });

        Files.delete(Paths.get(context.getFileCompletePath() + separator + txid));
    }

}
