package mb.fw.transformation.tool;

import mb.fw.transformation.form.MessageFormBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;


public class FormUtil {
	
	private static Logger logger = LoggerFactory.getLogger(FormUtil.class);

	public static void createfileMessageFormBox(String fileStr) throws IOException{
		
		MessageFormBox box = new MessageFormBox();
		File file = new File(fileStr);
		
		if(!file.exists()){
			file.createNewFile();
		}else{
			file.delete();
			file.createNewFile();
		}
		
		FileOutputStream fos = new FileOutputStream(file);
	    ObjectOutputStream oos = new ObjectOutputStream(fos);
	    oos.writeObject(box);
	    oos.close();
		fos.close();
	    
	}
	
	public static void save(MessageFormBox box , String fileStr) throws IOException{
		
		File file = new File(fileStr);
		
		if(!file.exists()){
			file.createNewFile();
		}else{
			file.delete();
			file.createNewFile();
		}
		
		FileOutputStream fos = new FileOutputStream(file);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(box);
		oos.close();
		fos.close();
		
	}
	
	public static void save(MessageFormBox box ,File file) throws IOException{
		
		if(!file.exists()){
			file.createNewFile();
		}else{
			file.delete();
			file.createNewFile();
		}
		
		FileOutputStream fos = new FileOutputStream(file);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(box);
		oos.close();
		fos.close();
		
	}
	
	
	public static MessageFormBox load(String fileStr) throws IOException, ClassNotFoundException {
		FileInputStream fin = new FileInputStream(fileStr);
	    ObjectInputStream ois = new ObjectInputStream(fin);
	    MessageFormBox box = (MessageFormBox) ois.readObject();
	    ois.close();
	    fin.close();
		return box;
	}
	
	public static void createfileMessageFormBox(File file) throws IOException{
		
		MessageFormBox box = new MessageFormBox();
		
		if(!file.exists()){
			file.createNewFile();
		}else{
			file.delete();
			file.createNewFile();
		}
		
		FileOutputStream fos = new FileOutputStream(file);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(box);
		oos.close();
		fos.close();
		
	}
	
	
	public static MessageFormBox load(File file) throws IOException, ClassNotFoundException {
		FileInputStream fin = new FileInputStream(file);
		ObjectInputStream ois = new ObjectInputStream(fin);
		MessageFormBox box = (MessageFormBox) ois.readObject();
		ois.close();
		fin.close();
		return box;
	}
	
	/**
	public static void fillData(byte[] msg, String srcType, RecordContext srcContext ,ByteOrder order )
			throws MCIException, UnsupportedEncodingException {
		
		srcContext.setOrder(order);
		
		// byteorder 맞춰서 Buffer 정의
		ChannelBuffer buff = ChannelBuffers.wrappedBuffer(msg);

		ChannelBuffer retBuff = ChannelBuffers.dynamicBuffer();

		byte[] retBytes = null;

		int srcCnt = srcContext.size();

		// 버퍼를 IN Record에 맞게 추출해서 data에 기록
		if (logger.isDebugEnabled())
			logger.debug("============================================================================================== [DATA]");

		for (int i = 1; i <= srcCnt; i++) {

			Record srcRecord = srcContext.get(i);
			byte[] readData = null;

			if (srcRecord.getType().equals("LIST")) {
				if (srcType.equals("C-STRUCTURE")) {
					i = readCStr(srcType, srcContext, buff, i, srcRecord , order);
				} else if (srcType.equals("BYTE[]")) {
					i = readBytes(srcType, srcContext, buff, i, srcRecord);
				}

			} else {

				readData = BufferedReadOut.reading(buff, srcRecord.getType(),srcRecord.getLength(), srcType);
				srcRecord.setData(readData);

				if (logger.isDebugEnabled()){
					logger.debug(srcRecord.toStringSimple());
				}
			}

		}

		if (logger.isDebugEnabled()) {
			logger.debug("[Before] ============================================================================================== \n");
			logger.debug("[After ] ============================================================================================== ");
		}

		retBytes = new byte[retBuff.readableBytes()];
		retBuff.readBytes(retBytes);
	}

	

	private static int readBytes(String srcType, RecordContext srcContext, ChannelBuffer buff, int nowindex, Record srcRecord) throws UnsupportedEncodingException {
		byte[] readData;
		int childCnt = srcRecord.getChildCount();
		int cntIdx = srcRecord.getCountNo();
		// logger.debug("cnt idx : "+inRecord);
		Record countIdxRecord = srcContext.get(cntIdx);
		byte[] data = countIdxRecord.getData();

		int validCnt = Integer.parseInt(new String(data));

		// list는 건너띠고
		for (int vaildIdx = 1; vaildIdx <= validCnt; vaildIdx++) {

			// list 다음부터 계산
			for (int j2 = (nowindex + 1); j2 <= (nowindex + childCnt); j2++) {

				Record childRecord = srcContext.get(j2);

				readData = BufferedReadOut.reading(buff, childRecord.getType(),childRecord.getLength(), srcType);

				childRecord.putData(vaildIdx, readData);

				if (logger.isDebugEnabled())
					logger.debug(childRecord.toStringSimple(readData));
			}
		}

		nowindex += childCnt;
		return nowindex;
	}

	private static int readCStr(String srcType,RecordContext  srcContext, ChannelBuffer buff, int i, Record srcRecord , ByteOrder order) throws UnsupportedEncodingException {
		byte[] readData;
		int totalCnt = srcRecord.getLength();
		int childCnt = srcRecord.getChildCount();
		int cntIdx = srcRecord.getCountNo();
		// logger.debug("cnt idx : "+inRecord);
		Record countIdxRecord = srcContext.get(cntIdx);
		byte[] data = countIdxRecord.getData();
		int validCnt = ChannelBuffers.wrappedBuffer(order, data).readInt();
		int garbageRocordslength = 0;
		int gabargeCnt = totalCnt - validCnt;

		boolean garbageCal = false;

		// list는 건너띠고
		for (int vaildIdx = 1; vaildIdx <= validCnt; vaildIdx++) {

			// list 다음부터 계산
			for (int j2 = (i + 1); j2 <= (i + childCnt); j2++) {

				Record childRecord = srcContext.get(j2);
				String childType = childRecord.getType();
				int childLength = childRecord.getLength();

				readData = BufferedReadOut.reading(buff, childRecord.getType(),childRecord.getLength(), srcType);

				childRecord.putData(vaildIdx, readData);

				if (!garbageCal) {
					if (childType.equals("CHAR")) {
						garbageRocordslength += (childLength + 1);
					} else {
						garbageRocordslength += (childLength);
					}
				}

				if (logger.isDebugEnabled())
					logger.debug(childRecord.toStringSimple(readData));
			}
			garbageCal = true;
		}

		int skipBytesLength = gabargeCnt * garbageRocordslength;
		buff.skipBytes(skipBytesLength);

		i += childCnt;
		return i;
	}
	**/
}
