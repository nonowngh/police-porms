package mb.fw.atb.strategy.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.config.Specifications;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.model.OnSignalInfo;
import mb.fw.atb.model.file.FileInfo;
import mb.fw.atb.model.file.FileMessage;
import mb.fw.atb.strategy.ATBStrategy;
import mb.fw.atb.util.DateUtils;
import org.apache.log4j.net.JMSAppender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(value = "FileMoveStrategy")
@Slf4j
public class FileMoveStrategy extends ATBStrategy {

    String separator = FileSystems.getDefault().getSeparator();

    @Autowired(required = false)
    private JmsTemplate jmsTemplate;

    ObjectMapper om = new ObjectMapper();

    @Override
    public Specifications specifications() {
        return null;
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

        String sendDt = DateUtils.today17();

        log.info("sendDt : " + sendDt);

        Path sendPath = Paths.get(context.getFileSendPath());
        Path recvPath = Paths.get(context.getFileRecvPath());

        if (Files.notExists(sendPath)) {
            sendPath = Files.createDirectory(sendPath);
        }
        if (Files.notExists(recvPath)) {
            recvPath = Files.createDirectory(recvPath);
        }

        Stream<Path> list = Files.list(sendPath);
        List<Path> sendList = list.filter(path -> Files.isRegularFile(path)).limit(context.getFileSendCount()).collect(Collectors.toList());

        if (sendList == null || sendList.size() == 0) {
            log.info("File Not Found , onSignal Stopped");
            return OnSignalInfo.builder().count(0).processEnd(true).build();
        } else {
            startLogging(context, txid, msgCreDt, sendDt, sendList.size());
        }

        for (Path moveFile : sendList) {
            Path beforeMovePath = Paths.get(context.getFileRecvPath() + separator + moveFile.getFileName());
            Path movedPath = Files.move(moveFile, beforeMovePath, StandardCopyOption.REPLACE_EXISTING);
            log.info(movedPath.toAbsolutePath() + " moved ");
        }

        FileMessage fileMessage = new FileMessage();
        generateFileMessage(context, txid, sendList, fileMessage);
        String resultMessage = "### FILE_LIST \n" + om.writeValueAsString(fileMessage);

        endLogging(context, txid, 0, context.getReceiverIds()[0], "S", resultMessage);

        return OnSignalInfo.builder().count(fileMessage.getCount()).processEnd(true).build();
    }

    @Override
    public OnSignalInfo onSignalRetry(IFContext context, String txid, String eventDt, Map<String, String> propMap) throws Exception {
        return null;
    }


    @Override
    public Object onMessageData(IFContext context, String txid, String eventDt, Object obj
            , Map<String, String> propMap) throws Exception {
        return null;
    }

    @Override
    public void onMessageResult(IFContext context, String txid, String eventDt, String resultCode, String
            resultMessage, String jsonStr, Map<String, String> propMap) throws Exception {
    }

    private static void generateFileMessage(IFContext context, String txid, List<Path> fileNameList, FileMessage fileMessage) {

        List<FileInfo> fileInfoList = Lists.newArrayList();
        for (Path filePath : fileNameList) {
            long size = 0;
            try {
                size = Files.size(filePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName(filePath.getFileName().toString());
            fileInfo.setSize(size);
            fileInfoList.add(fileInfo);
        }

        fileMessage.setFileInfoList(fileInfoList);
        fileMessage.setCount(fileInfoList.size());
    }


}
