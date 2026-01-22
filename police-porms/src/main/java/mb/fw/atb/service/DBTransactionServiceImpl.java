package mb.fw.atb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.enums.THeader;
import mb.fw.atb.error.ATBException;
import mb.fw.atb.error.ErrorCode;
import mb.fw.atb.job.file.FlatPackJob;
import mb.fw.atb.model.data.DetailData;
import mb.fw.atb.model.data.DBMessage;
import mb.fw.atb.model.file.FileDataError;
import mb.fw.atb.model.file.FileInfo;
import mb.fw.atb.model.file.FileMessage;
import net.sf.flatpack.DataError;
import net.sf.flatpack.DataSet;
import net.sf.flatpack.Parser;
import org.apache.ibatis.session.*;
import org.jetbrains.annotations.NotNull;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service("DBTransactionService")
@Slf4j
public class DBTransactionServiceImpl implements DBTransactionService {

    @Autowired(required = false)
    SqlSessionTemplate sqlSessionTemplate;

    ObjectMapper om = new ObjectMapper();

    @Transactional
    @Override
    @TimeTrace
    public void insertFlatpack(DataSet ds, String callName, IFContext context, Map<String, String> addProp, boolean errorSkip, String fileName) throws Exception {


        int batchCount = 0;

        while (ds.next()) {
            Map record = Maps.newLinkedHashMap();
            for (String column : ds.getColumns()) {
                record.put(column, ds.getString(column));
            }
            record.putAll(addProp);
            //log.debug("Insert Data : " + record);
            sqlSessionTemplate.insert(callName, record);
            batchCount++;
            if (batchCount % 4000 == 0) {
                batchCount = 0;
                sqlSessionTemplate.flushStatements();
            }
        }

        if (batchCount < 4000) {
            sqlSessionTemplate.flushStatements();
        }

        //에러난 파일을 추출한다.
        if (ds.getErrors() != null && !ds.getErrors().isEmpty()) {
            log.info("FOUND ERRORS IN FILE....");
            StringBuilder lineSB = new StringBuilder();

            StringBuilder sb = new StringBuilder();

            for (DataError de : ds.getErrors()) {
                log.error("Error: " + de.getErrorDesc() + " Line: " + de.getLineNo());
                sb.append(de.getLineNo()).append(",");
            }

            sb.deleteCharAt(sb.length() - 1);
            if (!errorSkip) {
                throw new RuntimeException(fileName + " File Parsing Error Row Count : " + ds.getErrorCount() + " , ErrorLines [" + sb + "]");
            }
        }
    }


    /**
     * Master/Detail 파일을 묶어서 Transaction 처리
     *
     * @param recvTempPathList
     * @param context
     * @param propMap
     * @throws Exception
     */
    @Transactional
    @Override
    @TimeTrace
    public void insertMDFlatpack(List<Path> recvTempPathList, IFContext context, Map<String, String> propMap, FileMessage retFileMessage, List<FileInfo> retFileInfoList) throws Exception {
        //childData가 있을경우 별로로 구현해야할듯
        for (Path insertFile : recvTempPathList) {
            UUID uuid = UUID.randomUUID();
            String key = uuid.toString();
            Parser parser = FlatPackJob.getParser(key, context, insertFile);

            DataSet ds = parser.parse();

            try {
                //Master
                String fileName = insertFile.getFileName().toString();
                log.info("INSERT FILE NAME : {}", fileName);
                if (fileName.endsWith("data")) {
                    insertFlatpackNoneTx(ds, context.getInterfaceId() + ".INSERT", context, propMap, context.isFileErrorSkip(), fileName);
                } else {
                    //확장자명을 가져와서 INSERT.확장자명으로 호출한다.<규칙>
                    String ext = fileName.substring(fileName.lastIndexOf(".") + 1);
                    insertFlatpackNoneTx(ds, context.getInterfaceId() + ".INSERT_" + ext, context, propMap, context.isFileErrorSkip(), fileName);
                }
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
        }
    }

    /**
     * insertFlatpack의 Transaction을 사용하지 않는 버전
     * insertMDFlatpack에서 사용
     *
     * @param ds
     * @param callName
     * @param context
     * @param addProp
     * @param errorSkip
     * @param fileName
     * @throws Exception
     */
    private void insertFlatpackNoneTx(DataSet ds, String callName, IFContext
            context, Map<String, String> addProp, boolean errorSkip, String fileName) throws Exception {
        int batchCount = 0;

        while (ds.next()) {
            Map record = Maps.newLinkedHashMap();
            for (String column : ds.getColumns()) {
                record.put(column, ds.getString(column));
            }
            record.putAll(addProp);
            //log.debug("Insert Data : " + record);
            sqlSessionTemplate.insert(callName, record);
            batchCount++;
            if (batchCount % 4000 == 0) {
                batchCount = 0;
                sqlSessionTemplate.flushStatements();
            }
        }

        if (batchCount < 4000) {
            sqlSessionTemplate.flushStatements();
        }

        //에러난 파일을 추출한다.
        if (ds.getErrors() != null && !ds.getErrors().isEmpty()) {
            log.info("FOUND ERRORS IN FILE....");
            StringBuilder lineSB = new StringBuilder();

            StringBuilder sb = new StringBuilder();

            for (DataError de : ds.getErrors()) {
                log.error("Error: " + de.getErrorDesc() + " Line: " + de.getLineNo());
                sb.append(de.getLineNo()).append(",");
            }

            sb.deleteCharAt(sb.length() - 1);
            if (!errorSkip) {
                throw new RuntimeException(fileName + " File Parsing Error Row Count : " + ds.getErrorCount() + " , ErrorLines [" + sb + "]");
            }
        }
    }

    @Transactional
    @Override
    @TimeTrace
    public void insertList(String callName, IFContext
            context, List<Map<String, Object>> dataList, Map<String, String> addProp) {
        for (Map record : dataList) {
            LinkedHashMap data = Maps.newLinkedHashMap();
            data.putAll(record);
            data.putAll(addProp);
            sqlSessionTemplate.insert(callName, data);
        }
    }

    /**
     * Master/Detail data insert
     *
     * @param dbMessage
     * @param context
     * @param propMap
     */
    @Transactional
    @TimeTrace
    @Override
    public void insertMDList(DBMessage dbMessage, IFContext context, Map<String, String> propMap) {
        List<Map<String, Object>> dataList = dbMessage.getDataList();

        try {
            insertListNoneTx(context.getInterfaceId() + ".INSERT", context, dbMessage.getDataList(), propMap);
            log.info("MASTER TABLE INSERT COMPLETE {}", dbMessage.getCount());
        } catch (Exception e) {
            ErrorCode errorCode = ErrorCode.INSERT_FAILURE;
            ATBException atbException = new ATBException("MASTER TABLE INSERT FAILURE", errorCode, e.getCause());
            log.error(errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage() + ",{}", atbException.getCause().getMessage());
            throw atbException;
        }

        List<Map<String, DetailData>> childList = dbMessage.getDetailList();
        for (Map<String, DetailData> childDataMap : childList) {
            for (String childName : childDataMap.keySet()) {
                DetailData detailData = childDataMap.get(childName);
                try {
                    insertListNoneTx(context.getInterfaceId() + ".INSERT_" + childName, context, detailData.getDataList(), propMap);
                    log.info("DETAIL TABLE INSERT COMPLETE {}", detailData.getSize());
                } catch (Exception e) {
                    ErrorCode errorCode = ErrorCode.INSERT_FAILURE;
                    ATBException atbException = new ATBException("DETAIL TABLE INSERT FAILURE", errorCode, e.getCause());
                    log.error(errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage() + ",{}", atbException.getCause().getMessage());
                    throw atbException;
                }
            }
        }
    }

    /**
     * insertList의 Transaction을 사용하지 않는 버전
     *
     * @param callName
     * @param context
     * @param dataList
     * @param addProp
     */
    private void insertListNoneTx(String callName, IFContext
            context, List<Map<String, Object>> dataList, Map<String, String> addProp) {
        for (Map record : dataList) {
            LinkedHashMap data = Maps.newLinkedHashMap();
            data.putAll(record);
            data.putAll(addProp);
            sqlSessionTemplate.insert(callName, data);
        }
    }

    /**
     * Master/Detail data insert
     *
     * @param callName
     * @param context
     * @param dataList
     * @param childList
     * @param addProp
     */
    @Transactional
    @Override
    @TimeTrace
    public void insertList(String callName, IFContext
            context, List<Map<String, Object>> dataList, List<Map<String, DetailData>> childList, Map<String, String> addProp) {
        for (Map record : dataList) {
            LinkedHashMap data = Maps.newLinkedHashMap();
            data.putAll(record);
            data.putAll(addProp);
            sqlSessionTemplate.insert(callName, data);

            for (Map<String, DetailData> multipleDataMap : childList) {
                for (String key : multipleDataMap.keySet()) {
                    DetailData detailData = multipleDataMap.get(key);
                    List<Map<String, Object>> childListData = detailData.getDataList();
                    for (Map childRecord : childListData) {
                        LinkedHashMap childDataMap = Maps.newLinkedHashMap();
                        childDataMap.putAll(childRecord);
                        childDataMap.putAll(addProp);
                        sqlSessionTemplate.insert(callName + "_" + detailData.getName(), childDataMap);
                    }
                }
            }
        }
    }

    @Transactional
    @Override
    @TimeTrace
    public void updateList(String callName, IFContext
            context, List<Map<String, Object>> dataList, Map<String, String> addProp) {
        for (Map record : dataList) {
            LinkedHashMap data = Maps.newLinkedHashMap();
            data.putAll(record);
            data.putAll(addProp);
            sqlSessionTemplate.update(callName, data);
        }
    }

    @Override
    public void insert(String callName, IFContext context, Map dataMap, Map<String, String> addProp) {
        LinkedHashMap data = Maps.newLinkedHashMap();
        data.putAll(dataMap);
        data.putAll(addProp);
        sqlSessionTemplate.insert(callName, data);
    }

    @Override
    @Transactional
    @TimeTrace
    public int update(String callName, IFContext context, Map map, Map<String, String> addProp) {
        SqlSessionFactory sqlSessionFactory = sqlSessionTemplate.getSqlSessionFactory();
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE, false);
        map.putAll(addProp);
        return sqlSession.update(callName, map);
    }

    @Override
    public int delete(String callName, IFContext context, Map map, Map addProp) {
        map.putAll(addProp);
        return sqlSessionTemplate.delete(callName, map);
    }

    @Override
    public List<Map<String, Object>> selectNtime(String callName, IFContext context, Map propMap) {
        return sqlSessionTemplate.selectList(callName, propMap);
    }

    @Override
    @TimeTrace
    public List<Map<String, Object>> select(String callName, IFContext context, Map propMap) {
        return sqlSessionTemplate.selectList(callName, propMap);
    }

    @Override
    @TimeTrace
    public Path selectToFile(String callName, IFContext context, Map propMap, String extName, boolean append) {

        String txId = (String) propMap.get(THeader.TRANSACTION_ID.key());
        String sendFilePathStr = context.getFileSendPath() + "/" + txId + "." + extName;

        AtomicBoolean headerFirstRow = new AtomicBoolean(true);
        AtomicLong rowCount = new AtomicLong(0);

        Path sendFilePath = Paths.get(sendFilePathStr);

        if (append) {
            if (Files.notExists(sendFilePath)) {
                try {
                    Files.createFile(sendFilePath);
                } catch (IOException e) {
                    log.error("FILE CREATE ERROR", e);
                    throw new RuntimeException(e);
                }
            } else {
                headerFirstRow.set(false);
            }
        } else {
            //기존에 파일이 존재하면 삭제하고 다시 생성한다.
            try {
                Files.deleteIfExists(sendFilePath);
            } catch (IOException e) {
                log.error("FILE DELETE ERROR", e);
                throw new RuntimeException(e);
            }

            //sendPath create
            try {
                Files.createFile(sendFilePath);
            } catch (IOException e) {
                log.error("FILE CREATE ERROR", e);
                throw new RuntimeException(e);
            }
        }


        ResultHandler rh = getResultHandler(context, rowCount, headerFirstRow, sendFilePath);
        sqlSessionTemplate.select(callName, propMap, rh);

        propMap.put(THeader.SENDER_DATA_COUNT.key(), String.valueOf(rowCount.get()));
        log.debug("SELECT COUNT {}", rowCount.get());
        if (rowCount.get() == 0) {
            try {
                Files.deleteIfExists(sendFilePath);
                return null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        //sendFilePath 를 로그에 출력
        log.info("GENERATED FILE PATH {}", sendFilePath);
        return sendFilePath;
    }

    @Override
    public Path selectToFileNtime(String callName, IFContext context, Map propMap, String extName, boolean append) {

        String txId = (String) propMap.get(THeader.TRANSACTION_ID.key());
        String sendFilePathStr = context.getFileSendPath() + "/" + txId + "." + extName;

        AtomicBoolean headerFirstRow = new AtomicBoolean(true);
        AtomicLong rowCount = new AtomicLong(0);

        Path sendFilePath = Paths.get(sendFilePathStr);

        if (append) {
            if (Files.notExists(sendFilePath)) {
                try {
                    Files.createFile(sendFilePath);
                } catch (IOException e) {
                    log.error("FILE CREATE ERROR", e);
                    throw new RuntimeException(e);
                }
            } else {
                headerFirstRow.set(false);
            }
        } else {
            //기존에 파일이 존재하면 삭제하고 다시 생성한다.
            try {
                Files.deleteIfExists(sendFilePath);
            } catch (IOException e) {
                log.error("FILE DELETE ERROR", e);
                throw new RuntimeException(e);
            }

            //sendPath create
            try {
                Files.createFile(sendFilePath);
            } catch (IOException e) {
                log.error("FILE CREATE ERROR", e);
                throw new RuntimeException(e);
            }
        }


        ResultHandler rh = getResultHandler(context, rowCount, headerFirstRow, sendFilePath);
        sqlSessionTemplate.select(callName, propMap, rh);

        propMap.put(THeader.SENDER_DATA_COUNT.key(), String.valueOf(rowCount.get()));
        log.debug("SELECT COUNT {}", rowCount.get());
        if (rowCount.get() == 0) {
            try {
                Files.deleteIfExists(sendFilePath);
                return null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        //sendFilePath 를 로그에 출력
        log.info("GENERATED FILE PATH {}", sendFilePath);
        return sendFilePath;
    }

    @NotNull
    private ResultHandler getResultHandler(IFContext context, AtomicLong rowCount, AtomicBoolean firstRow, Path
            sendFilePath) {
        return new ResultHandler<Map>() {
            @Override
            public void handleResult(ResultContext<? extends Map> resultContext) {
                rowCount.incrementAndGet();
                Map rowMap = resultContext.getResultObject();
                char fileDelimiter = context.getFileDelimiter();
                char fileDelimiterQualifier = context.getFileDelimiterQualifier();
                StringBuilder keysSB = new StringBuilder();
                StringBuilder valueSB = new StringBuilder();

                Set<String> keySet = rowMap.keySet();

                if (firstRow.get()) {

                    for (String key : keySet) {
                        keysSB.append(key).append(fileDelimiter);
                    }
                    keysSB.deleteCharAt(keysSB.length() - 1);

                    try {
                        Files.write(sendFilePath, (keysSB + "\n").getBytes(), StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    firstRow.set(false);
                }
                //log.debug("SELECT ROW {}", rowMap);
                for (String key : keySet) {
                    Object value = rowMap.get(key);
                    //log.info("SELECT ROW {} {}", key, value);
                    if (value == null) {
                        valueSB.append(fileDelimiter);
                    } else {
                        if (String.valueOf(value).indexOf(fileDelimiter + "") > -1) {
                            valueSB.append(fileDelimiterQualifier).append(value).append(fileDelimiterQualifier).append(fileDelimiter);
                        } else {
                            valueSB.append(value).append(fileDelimiter);
                        }
                    }
                }

                valueSB.deleteCharAt(valueSB.length() - 1);

                try {
                    Files.write(sendFilePath, (valueSB + "\n").getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Override
    @TimeTrace
    public Object selectOne(String callName, IFContext context, Map propMap) {
        return sqlSessionTemplate.selectOne(callName, propMap);
    }

    @Transactional
    @Override
    @TimeTrace
    public void call(String callName, IFContext context, Map<String, String> addProp) {
        sqlSessionTemplate.update(callName, addProp);
    }
}
