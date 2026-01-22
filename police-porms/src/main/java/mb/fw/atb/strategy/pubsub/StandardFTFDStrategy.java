package mb.fw.atb.strategy.pubsub;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.config.Specifications;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.enums.TResult;
import mb.fw.atb.job.file.FileRecvJob;
import mb.fw.atb.job.file.FlatPackJob;
import mb.fw.atb.job.file.IFTPJob;
import mb.fw.atb.model.file.FileDataError;
import mb.fw.atb.model.file.FileInfo;
import mb.fw.atb.model.file.FileMessage;
import mb.fw.atb.schema.pubsub.StandardFTFDStrategySchema;
import mb.fw.atb.service.DBTransactionService;
import net.sf.flatpack.DataError;
import net.sf.flatpack.DataSet;
import net.sf.flatpack.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component(value = "StandardFTFDStrategy")
@Slf4j
public class StandardFTFDStrategy extends StandardFTFStrategy {

    @Autowired(required = false)
    @Qualifier("DBTransactionService")
    DBTransactionService dbTransactionService;

    @Autowired
    IFTPJob iftpJob;

    @Autowired
    FileRecvJob fileRecvJob;

    @Override
    public Specifications specifications() {
        return StandardFTFDStrategySchema.specifications();
    }

    @Override
    @TimeTrace
    public Object onMessageData(IFContext context, String txid, String eventDt, Object obj, Map<String, String> propMap) throws Exception {

        List<Path> recvTempPathList;
        FileMessage recvFileMessage = objectMapper.readValue((String) obj, FileMessage.class);

        FileMessage retFileMessage = new FileMessage();
        List<FileInfo> retFileInfoList = Lists.newArrayList();

        try {
            recvTempPathList = iftpJob.getFiles(client, recvFileMessage.getFileInfoList(), context, txid);
        } catch (Exception e) {
            throw e;
        }

        try {
            //저장된걸 csv형태의 파일이라고 생각하고 DB에 입력한다.
            boolean isFirstHeader = context.isFileFirstHeader();

            if (recvFileMessage.isDetailData()) {
                //childData가 있을경우 별로로 구현해야할듯
                dbTransactionService.insertMDFlatpack(recvTempPathList, context, propMap, retFileMessage, retFileInfoList);
                if (context.isProcedureCall()) {
                    dbTransactionService.call(context.getInterfaceId() + ".CALL", context, propMap);
                }
            } else {
                for (Path insertFile : recvTempPathList) {
                    UUID uuid = UUID.randomUUID();
                    String key = uuid.toString();
                    Parser parser = FlatPackJob.getParser(key, context, insertFile);
                    DataSet ds = parser.parse();

                    try {
                        dbTransactionService.insertFlatpack(ds, context.getInterfaceId() + ".INSERT", context, propMap, context.isFileErrorSkip(), insertFile.getFileName().toString());
                    } finally {
                        FlatPackJob.close(key);
                        if (ds.getErrors() != null && !ds.getErrors().isEmpty()) {
                            retFileMessage.setErrorCount(1);
                        }
                        FileInfo fileInfo = new FileInfo();
                        fileInfo.setFileName(insertFile.getFileName().toString());
                        fileInfo.setSize(Files.size(insertFile));
                        fileInfo.setParseErrorCount(ds.getErrorCount());
                        List<FileDataError> errorList = Lists.newArrayList();
                        for (DataError de : ds.getErrors()) {
                            FileDataError dataError = new FileDataError();
                            dataError.parseDataError(de);
                            errorList.add(dataError);
                        }
                        fileInfo.setErrors(errorList);

                        retFileInfoList.add(fileInfo);
                    }

                    if (context.isProcedureCall()) {
                        dbTransactionService.call(context.getInterfaceId() + ".CALL", context, propMap);
                    }
                }
            }
            //정상일경우 success로 실패일경우 fail로 이동
            fileRecvJob.resultProcessFileInfo(txid, TResult.SUCCESS.value(), "OK", context, recvFileMessage.getFileInfoList());
        } catch (Exception e) {
            //정상일경우 success로 실패일경우 fail로 이동
            fileRecvJob.resultProcessFileInfo(txid, TResult.FAIL.value(), e.getMessage(), context, recvFileMessage.getFileInfoList());
            throw e;
        } finally {
            retFileMessage.setFileInfoList(retFileInfoList);
            retFileMessage.setCount(retFileInfoList.size());
        }

        return retFileMessage;
    }


    protected static void generateFileMessage(IFContext context, String txid, List<String> fileNameList, FileMessage fileMessage) {

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
