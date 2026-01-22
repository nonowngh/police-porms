package mb.fw.atb.service;

import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.model.data.DetailData;
import mb.fw.atb.model.data.DBMessage;
import mb.fw.atb.model.file.FileInfo;
import mb.fw.atb.model.file.FileMessage;
import net.sf.flatpack.DataSet;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface DBTransactionService {

    public void insertFlatpack(DataSet ds, String callName, IFContext context, Map<String, String> addProp, boolean errorSkip, String fileName) throws Exception;

    public void insertMDFlatpack(List<Path> recvTempPathList, IFContext context, Map<String, String> propMap, FileMessage retFileMessage, List<FileInfo> retFileInfoList) throws Exception;

    public void insertList(String callName, IFContext context, List<Map<String, Object>> dataList, Map<String, String> addProp);

    public void insertMDList(DBMessage dbMessage, IFContext context, Map<String, String> propMap);

    public void insertList(String callName, IFContext context, List<Map<String, Object>> dataList, List<Map<String, DetailData>> childList, Map<String, String> addProp);

    public void updateList(String callName, IFContext context, List<Map<String, Object>> dataList, Map<String, String> addProp);

    public void call(String callName, IFContext context, Map<String, String> addProp);

    public void insert(String callName, IFContext context, Map map, Map<String, String> addProp);

    public List<Map<String, Object>> select(String callName, IFContext context, Map propMap);

    public List<Map<String, Object>> selectNtime(String callName, IFContext context, Map propMap);

    public Path selectToFile(String callName, IFContext context, Map propMap, String extName, boolean append);

    public Path selectToFileNtime(String callName, IFContext context, Map propMap, String extName, boolean append);

    public Object selectOne(String callName, IFContext context, Map propMap);

    public int update(String callName, IFContext context, Map map, Map<String, String> addProp);

    public int delete(String callName, IFContext context, Map map, Map<String, String> addProp);
}
