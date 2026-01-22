package mb.fw.atb.strategy.pubsub;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.config.Specifications;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.enums.TResult;
import mb.fw.atb.job.file.*;
import mb.fw.atb.model.OnSignalInfo;
import mb.fw.atb.model.file.FileInfo;
import mb.fw.atb.model.file.FileMessage;
import mb.fw.atb.schema.pubsub.StandardFTFStrategySchema;
import mb.fw.atb.strategy.ATBStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component(value = "StandardFTFStrategy")
@Slf4j
public class StandardFTFStrategy extends ATBStrategy {

    @Autowired
    DirectoryPolling directoryPolling;

    @Autowired
    IFTPJob iftpJob;

    @Autowired
    TempMove tempMove;

    @Autowired
    FileRecvJob fileRecvJob;

    @Autowired
    FileOnMessageResult fileOnMessageResult;

    @Override
    public Specifications specifications() {
        return StandardFTFStrategySchema.specifications();
    }

    /**
     * example :
     *
     * @param context
     * @param txid
     * @param msgCreDt
     * @throws Exception
     */
    @Override
    @TimeTrace
    public OnSignalInfo onSignal(IFContext context, String txid, String msgCreDt) throws Exception {
        List<Path> pollList = directoryPolling.fileSendPoll(txid, context);

        if (pollList.size() == 0) {
            log.info("SCHEDULE STOPPED ");
            return null;
        }

        List<Path> tempList = null;

        tempList = tempMove.tempMove(pollList, context, txid);

        List<String> fileNameList = null;
        log.info("IFTPClient {}", client);
        fileNameList = iftpJob.putNRollback(client, tempList, context, txid);

        FileMessage fileMessage = new FileMessage();

        generateFileMessage(context, txid, fileNameList, fileMessage);

        return OnSignalInfo.builder().count(fileMessage.getCount()).sendObject(fileMessage).build();
    }

    @Override
    @TimeTrace
    public OnSignalInfo onSignalRetry(IFContext context, String txid, String eventDt, Map<String, String> propMap) throws Exception {
        log.info("ONSIGNAL_RETRY STARTED ==> {}", txid);


        //temp , complete , error 디렉토리에에 있는 파일을 찾아서 전송해야한다.
        String fileErrorPath = context.getFileErrorPath();
        String fileTempPath = context.getFileTempPath();
        String fileCompletePath = context.getFileCompletePath();
        String fileSuccessPath = context.getFileSuccessPath();

        Path retryTempPath = Paths.get(fileTempPath + IFTPJob.separator + txid);
        Path retryCompletePath = Paths.get(fileCompletePath + IFTPJob.separator + txid);
        Path retrySuccessPath = Paths.get(fileSuccessPath + IFTPJob.separator + txid);

        boolean isFindFile = false;

        Path retryErrorPath = Paths.get(fileErrorPath + IFTPJob.separator + txid);
        //에러에 있다면 안꼬이게 절차대로 temp로 옮기자
        List<Path> pollList = new ArrayList<Path>();
        List<Path> finalTempList = new ArrayList<Path>();

        if (Files.exists(retryErrorPath)) {
            log.info("재전송할 파일을 찾음 ==> {}", retryErrorPath);
            Files.list(retryErrorPath).forEach(path -> pollList.add(path));
            List<Path> tempList = tempMove.tempMove(pollList, context, txid);
            Files.delete(retryErrorPath);
            isFindFile = true;
            finalTempList.addAll(tempList);
        }

        if (!isFindFile && Files.exists(retryCompletePath)) {
            log.info("재전송할 파일을 찾음 ==> {}", retryCompletePath);
            Files.list(retryCompletePath).forEach(path -> pollList.add(path));
            List<Path> tempList = tempMove.tempMove(pollList, context, txid);
            Files.delete(retryCompletePath);
            isFindFile = true;
            finalTempList.addAll(tempList);
        }

        if (!isFindFile && Files.exists(retrySuccessPath)) {
            log.info("재전송할 파일을 찾음 ==> {}", retrySuccessPath);
            Files.list(retrySuccessPath).forEach(path -> pollList.add(path));
            List<Path> tempList = tempMove.tempMove(pollList, context, txid);
            Files.delete(retrySuccessPath);
            isFindFile = true;
            finalTempList.addAll(tempList);
        }

        if (!isFindFile && Files.exists(retryTempPath)) {
            log.info("재전송할 파일을 찾음 ==> {}", retryTempPath);
            List<Path> tempList = new ArrayList<Path>();
            Files.list(retryTempPath).forEach(path -> tempList.add(path));
            isFindFile = true;
            finalTempList.addAll(tempList);
        }

        if (!isFindFile) {
            log.info("재전송할 파일을 찾지 못함 ==> {}", txid);
            return null;
        }

        List<String> fileNameList = null;
        log.info("IFTPClient {}", client);
        fileNameList = iftpJob.putNRollback(client, finalTempList, context, txid);

        FileMessage fileMessage = new FileMessage();

        generateFileMessage(context, txid, fileNameList, fileMessage);

        return OnSignalInfo.builder().count(fileMessage.getCount()).sendObject(fileMessage).build();
    }

    @Override
    @TimeTrace
    public Object onMessageData(IFContext context, String txid, String eventDt, Object obj, Map<String, String> propMap) throws Exception {

        List<Path> recvTempPathList;
        FileMessage fileMessage = objectMapper.readValue((String) obj, FileMessage.class);

        try {
            recvTempPathList = iftpJob.getFiles(client, fileMessage.getFileInfoList(), context, txid);
        } catch (Exception e) {
            throw e;
        }

        List<Path> recvPathList;

        try {
            recvPathList = fileRecvJob.saveMove(recvTempPathList, context, txid);
        } catch (Exception e) {
            throw e;
        }

        return fileMessage;
    }

    @Override
    @TimeTrace
    public void onMessageResult(IFContext context, String txid, String eventDt, String resultCode, String resultMessage, String dataStr, Map<String, String> propMap) throws Exception {
        FileMessage fileMessage = objectMapper.readValue(dataStr, FileMessage.class);

        try {
            fileOnMessageResult.resultProcessFileInfo(txid, resultCode, resultMessage, context);

            //hub에 있는 파일을 삭제함
            if (context.isHubFileDelete() && TResult.SUCCESS.value().equals(resultCode)) {
                for (FileInfo fileInfo : fileMessage.getFileInfoList()) {
                    iftpJob.remoteFileDelete(client, fileInfo.getFileName(), txid, context.getFileRetryCount());
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private static void generateFileMessage(IFContext context, String txid, List<String> fileNameList, FileMessage fileMessage) {

        List<FileInfo> fileInfoList = Lists.newArrayList();

        for (String fileName : fileNameList) {
            Path sendCompletePath = Paths.get(context.getFileCompletePath() + IFTPJob.separator + txid + IFTPJob.separator + fileName);
            long size = 0;
            try {
                size = Files.size(sendCompletePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName(fileName);
            fileInfo.setSize(size);
            fileInfoList.add(fileInfo);
        }

        fileMessage.setFileInfoList(fileInfoList);
        fileMessage.setCount(fileInfoList.size());
    }

}
