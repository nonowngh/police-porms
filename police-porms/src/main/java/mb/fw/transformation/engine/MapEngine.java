package mb.fw.transformation.engine;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import mb.fw.transformation.exception.MCIException;
import mb.fw.transformation.form.Record;
import mb.fw.transformation.form.RecordContext;
import mb.fw.transformation.tool.ExpressionEvaluator;
import mb.fw.transformation.tool.TypeConversion;
import mb.fw.transformation.type.AsciiType;
import mb.fw.transformation.type.CType;
import mb.fw.transformation.type.MapType;
import org.apache.velocity.runtime.parser.ParseException;
import org.dom4j.DocumentException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jdom2.JDOMException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MAP type Support
 *
 * @author clupine
 *
 */
public class MapEngine {

	private static Logger logger = LoggerFactory.getLogger(MapEngine.class);

	ByteOrder srcOrder = ByteOrder.BIG_ENDIAN;
	ByteOrder trgOrder = ByteOrder.BIG_ENDIAN;

	XStream xStream = new XStream(new DomDriver());

	static VMXmlEngine vmXmlSupportEngine = new VMXmlEngine();

	public void setSrcOrder(ByteOrder srcOrder) {
		this.srcOrder = srcOrder;
	}

	public void setTrgOrder(ByteOrder trgOrder) {
		this.trgOrder = trgOrder;
	}

	/**
	 * Map -> ASCII SUPPORT
	 *
	 * @param srcMap
	 * @param srcContext
	 * @param trgContext
	 * @return
	 * @throws MCIException
	 */
	public byte[] conversionToAsciiBytes(Map srcMap, RecordContext srcContext, RecordContext trgContext)
			throws MCIException, UnsupportedEncodingException {
		srcContext.setOrder(srcOrder.toString());
		trgContext.setOrder(trgOrder.toString());
		ChannelBuffer retBuff = ChannelBuffers.dynamicBuffer();
		byte[] retBytes = null;

		if (logger.isDebugEnabled())
			logger.debug(
					"============================================================================================== [Before]");
		int trgCnt = trgContext.size();

		readOut(srcMap, srcContext, null, true);

		if (logger.isDebugEnabled()) {
			logger.debug(
					"[Before] ============================================================================================== \n");
			logger.debug(
					"[After ] ============================================================================================== ");
		}

		for (int i = 1; i <= trgCnt; i++) {

			Record trgRecord = trgContext.get(i);
			if (!(trgRecord.getFunction() == null || trgRecord.getFunction() == "")) {
				ExpressionEvaluator.evaluate(srcContext, retBuff, trgRecord, trgRecord.getFunction(), srcOrder);
			} else {
				if (trgRecord.getType().equals("LIST") || trgRecord.getType().equals("MAP")) {
					i = convertListorMapAscii(srcContext, trgContext, retBuff, i, trgRecord);
				} else {

					int rank = Integer.parseInt(trgRecord.getRank());
					Record srcRecord = srcContext.get(rank);
					// 가변 데이터
					if (trgRecord.getLength() == -1) {

						int countNo = trgRecord.getCountNo();
						Record varLenTrgRecord = trgContext.get(countNo);

						byte[] convertBytes = TypeConversion.convertMapToVarAscii(srcRecord, trgRecord,
								varLenTrgRecord);
						trgRecord.setData(convertBytes);
						retBuff.writeBytes(convertBytes);

					} else {
						if (!trgRecord.getType().equals("MAP")) {
							byte[] convertBytes = TypeConversion.asciiConvert(srcRecord.getData(), trgRecord.getType(),
									trgRecord.getLength());
							trgRecord.setData(convertBytes);
							retBuff.writeBytes(convertBytes);
						}
					}

					if (logger.isDebugEnabled())
						logger.debug(trgRecord.toStringSimple());
				}
			}
		}

		if (logger.isDebugEnabled())
			logger.debug(
					"============================================================================================== [After ]");

		retBytes = new byte[retBuff.readableBytes()];
		retBuff.readBytes(retBytes);

		return retBytes;
	}

	/**
	 * Map -> Map SUPPORT
	 *
	 * @param srcMap
	 * @param srcContext
	 * @param trgContext
	 * @return
	 * @throws MCIException
	 */
	public Map conversionToMap(Map srcMap, RecordContext srcContext, RecordContext trgContext)
			throws MCIException, UnsupportedEncodingException {
		srcContext.setOrder(srcOrder.toString());
		trgContext.setOrder(trgOrder.toString());

		if (logger.isDebugEnabled())
			logger.debug(
					"============================================================================================== [Before]");
		int trgCnt = trgContext.size();

		readOut(srcMap, srcContext, null, true);

		if (logger.isDebugEnabled()) {
			logger.debug(
					"[Before] ============================================================================================== \n");
			logger.debug(
					"[After ] ============================================================================================== ");
		}

		Map trgMap = new LinkedHashMap();

		for (int i = 1; i <= trgCnt; i++) {

			Record trgRecord = trgContext.get(i);
			if (!(trgRecord.getFunction() == null || trgRecord.getFunction() == "")) {
				ExpressionEvaluator.evaluateMapToMap(srcContext, trgMap, trgRecord, trgRecord.getFunction(), srcOrder);
			} else {
				if (trgRecord.getType().equals("LIST") || trgRecord.getType().equals("MAP")) {
					i = convertListorMap(srcContext, trgContext, srcMap, trgMap, i, trgRecord);
				} else {
					int rank = Integer.parseInt(trgRecord.getRank());
					Record srcRecord = srcContext.get(rank);
					Object val = TypeConversion.convertMapToMap(srcRecord.getData(), srcRecord, trgRecord, srcOrder,
							trgOrder);
					trgRecord.setData(val.toString().getBytes());
					trgMap.put(trgRecord.getName(), val);

					if (logger.isDebugEnabled())
						logger.debug(trgRecord.toStringSimple());
				}
			}
		}

		if (logger.isDebugEnabled())
			logger.debug(
					"============================================================================================== [After ]");

		return trgMap;
	}

	public String conversionToVMXml(Map inMap, RecordContext inContext, RecordContext outContext)
			throws MCIException, ParseException, IOException, JDOMException, DocumentException {
		Map outMap = conversionToMap(inMap, inContext, outContext);
		return vmXmlSupportEngine.convertVMXml(outMap, outContext);
	}

	public String conversionToXML(Map inMap, RecordContext inContext, RecordContext outContext)
			throws UnsupportedEncodingException, MCIException {
		Map retMap = conversionToMap(inMap, inContext, outContext);
		return xStream.toXML(retMap);
	}

	public String conversionToJson(Map inMap, RecordContext inContext, RecordContext outContext)
			throws UnsupportedEncodingException, MCIException {
		Map retMap = conversionToMap(inMap, inContext, outContext);
		return new JSONObject(retMap).toJSONString();
	}

	/**
	 * MAP -> CTYPE SUPPORT
	 *
	 * @param srcMap
	 * @param srcContext
	 * @param trgContext
	 * @return
	 * @throws MCIException
	 * @throws UnsupportedEncodingException
	 */
	public byte[] conversionToCType(Map srcMap, RecordContext srcContext, RecordContext trgContext)
			throws MCIException, UnsupportedEncodingException {

		srcContext.setOrder(srcOrder.toString());
		trgContext.setOrder(trgOrder.toString());
		ChannelBuffer retBuff = ChannelBuffers.dynamicBuffer();
		byte[] retBytes = null;

		if (logger.isDebugEnabled())
			logger.debug(
					"============================================================================================== [Before]");
		int trgCnt = trgContext.size();

		readOut(srcMap, srcContext, null, true);

		if (logger.isDebugEnabled()) {
			logger.debug(
					"[Before] ============================================================================================== \n");
			logger.debug(
					"[After ] ============================================================================================== ");
		}

		for (int i = 1; i <= trgCnt; i++) {

			Record trgRecord = trgContext.get(i);
			if (!(trgRecord.getFunction() == null || trgRecord.getFunction() == "")) {
				ExpressionEvaluator.evaluate(srcContext, retBuff, trgRecord, trgRecord.getFunction(), srcOrder);
			} else {
				if (trgRecord.getType().equals("LIST")) {
					i = convertListCType(srcContext, trgContext, retBuff, i, trgRecord);
				} else {
					int rank = Integer.parseInt(trgRecord.getRank());
					Record srcRecord = srcContext.get(rank);
					// 가변 데이터
					if (trgRecord.getLength() == -1) {

						int countNo = trgRecord.getCountNo();
						Record varLenTrgRecord = trgContext.get(countNo);
						byte[] convertBytes = TypeConversion.convertMapToVarChar(srcRecord.getData(), varLenTrgRecord,
								srcOrder, trgOrder);
						trgRecord.setData(convertBytes);
						retBuff.writeBytes(convertBytes);

					} else {
						byte[] convertBytes = TypeConversion.convertMapToCType(srcRecord.getData(), srcRecord,
								trgRecord, srcOrder, trgOrder);
						trgRecord.setData(convertBytes);
						retBuff.writeBytes(convertBytes);
					}

					if (logger.isDebugEnabled())
						logger.debug(trgRecord.toStringSimple());
				}
			}
		}

		if (logger.isDebugEnabled())
			logger.debug(
					"============================================================================================== [After ]");

		retBytes = new byte[retBuff.readableBytes()];
		retBuff.readBytes(retBytes);

		return retBytes;
	}

	public void readOut(Map srcMap, RecordContext srcContext, List<Record> readOutList, boolean islogging)
			throws UnsupportedEncodingException {
		int srcCnt = srcContext.size();
		for (int i = 1; i <= srcCnt; i++) {
			Record srcRecord = srcContext.get(i);
			if (srcRecord.getType().equals("LIST") || srcRecord.getType().equals("MAP")) {
				i = readListorMap(srcContext, srcMap, i, srcRecord, islogging);
			} else {
				if (srcRecord.getType().equals(MapType.STRING.name())|| srcRecord.getType().equals(MapType.BITSTR.name())  || srcRecord.getType().equals(MapType.DECIMAL.name()) || srcRecord.getType().equals(MapType.INTEGER.name())) {

					Object data = srcMap.get(srcRecord.getName());

					if (data == null) {
						String defaultValue = srcRecord.getDefaultValue();
						if (defaultValue == null || defaultValue.equals("")) {
							srcRecord.setData("".getBytes());
						} else {
							srcRecord.setData(defaultValue.getBytes());
						}
					} else {
						srcRecord.setData(String.valueOf(data).getBytes());
					}
				} else if (srcRecord.getType().equals(MapType.BYTE.name())) {
					Object bb = srcMap.get(srcRecord.getName());
					if (bb instanceof Byte) {
						byte[] databyte = { (Byte) srcMap.get(srcRecord.getName()) };
						srcRecord.setData(databyte);
					} else {
						byte[] data = (byte[]) srcMap.get(srcRecord.getName());
						srcRecord.setData(data);
					}
				}

				if (readOutList != null)
					readOutList.add(srcRecord);
				if (islogging) {
					if (logger.isDebugEnabled()) {
						logger.debug(srcRecord.toStringSimple());
					}
				}
			}
		}
	}

	/**
	 * Map To RecordContext Data setter
	 *
	 * @param srcContext
	 * @param srcMap
	 * @param nowindex
	 * @param srcRecord
	 * @param islogging
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private int readListorMap(RecordContext srcContext, Map srcMap, int nowindex, Record srcRecord, boolean islogging)
			throws UnsupportedEncodingException {
		int childCnt = srcRecord.getChildCount();

		Object obj = srcMap.get(srcRecord.getName());

		if (obj instanceof Map) {
			Map childMap = (Map) obj;

			if (islogging) {
				if (logger.isDebugEnabled()) {
					Record nowRecord = srcContext.get(nowindex);
					nowRecord.setLength(1);
					logger.debug(nowRecord.toStringSimple());
				}
			}
			for (int j2 = (nowindex + 1); j2 <= (nowindex + childCnt); j2++) {

				Record childRecord = srcContext.get(j2);
				if (childRecord.getType().equals("STRING") || childRecord.getType().equals("BITSTR") || childRecord.getType().equals(MapType.DECIMAL.name()) || srcRecord.getType().equals(MapType.INTEGER.name())) {
					Object childData =  childMap.get(childRecord.getName());

					if (childData == null) {
						String defaultValue = srcRecord.getDefaultValue();
						if (defaultValue == null || defaultValue.equals("")) {
							childRecord.setData("".getBytes());
						} else {
							childRecord.setData(defaultValue.getBytes());
						}
					} else {
						childRecord.setData(String.valueOf(childData).getBytes());
					}

					if (islogging) {
						if (logger.isDebugEnabled()) {
							if (childData != null) {
								logger.debug(childRecord.toStringSimple(String.valueOf(childData).getBytes()));
							}else{
								logger.debug(childRecord.toStringSimple("".getBytes()));
							}
						}
					}
				} else if (childRecord.getType().equals("BYTE")) {
					byte[] childBytes;
					Object temp = childMap.get(childRecord.getName());
					if (temp instanceof Byte) {
						childBytes = new byte[] { (Byte) temp };
						childRecord.setData(childBytes);
					} else {
						childBytes = (byte[]) temp;
						childRecord.setData(childBytes);
					}
					if (islogging) {
						if (logger.isDebugEnabled())
							logger.debug(childRecord.toStringSimple(childBytes));
					}
				}
			}

			nowindex += childCnt;
			return nowindex;
		} else {
			List dataList = (List) obj;

			if (dataList != null) {
				int validCnt = dataList.size();
				int listIdx = 0;

				if (islogging) {
					if (logger.isDebugEnabled()) {
						Record nowRecord = srcContext.get(nowindex);
						nowRecord.setLength(validCnt);
						logger.debug(nowRecord.toStringSimple());
					}
				}

				for (int vaildIdx = 1; vaildIdx <= validCnt; vaildIdx++) {
					Map childMap = (Map) dataList.get(listIdx);
					listIdx++;

					for (int j2 = (nowindex + 1); j2 <= (nowindex + childCnt); j2++) {

						Record childRecord = srcContext.get(j2);
						if (childRecord.getType().equals("STRING") || childRecord.getType().equals("BITSTR") || childRecord.getType().equals(MapType.DECIMAL.name()) || srcRecord.getType().equals(MapType.INTEGER.name()))  {

							Object childData =  childMap.get(childRecord.getName());

							if (childData == null) {
								String defaultValue = childRecord.getDefaultValue();
								if (defaultValue == null || defaultValue.equals("")) {
									childRecord.putData(vaildIdx , "".getBytes());
								} else {
									childRecord.putData(vaildIdx ,defaultValue.getBytes());
								}
							} else {
								childRecord.putData(vaildIdx ,String.valueOf(childData).getBytes());
							}

							if (islogging) {
								if (logger.isDebugEnabled()) {
									if (childData != null) {
										logger.debug(childRecord.toStringSimple(String.valueOf(childData).getBytes()));
									}else{
										logger.debug(childRecord.toStringSimple("".getBytes()));
									}
								}
							}

						} else if (childRecord.getType().equals("BYTE")) {
							byte[] childBytes;
							Object temp = childMap.get(childRecord.getName());
							if (temp instanceof Byte) {
								childBytes = new byte[] { (Byte) temp };
								childRecord.putData(vaildIdx, childBytes);
							} else {
								childBytes = (byte[]) temp;
								childRecord.putData(vaildIdx, childBytes);
							}
							if (islogging) {
								if (logger.isDebugEnabled())
									logger.debug(childRecord.toStringSimple(childBytes));
							}
						}
					}
				}

			}
			nowindex += childCnt;
			return nowindex;
		}

	}

	/**
	 * RecordContext to CTYPE Message
	 *
	 * @param srcContext
	 * @param trgContext
	 * @param retBuff
	 * @param nowIndex
	 * @param trgRecord
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private int convertListCType(RecordContext srcContext, RecordContext trgContext, ChannelBuffer retBuff,
			int nowIndex, Record trgRecord) throws UnsupportedEncodingException {

		int childCnt = trgRecord.getChildCount();
		int cntIdx = trgRecord.getCountNo();

		Record countIdxRecord = trgContext.get(cntIdx);
		byte[] data = countIdxRecord.getData();
		ChannelBuffer buff = ChannelBuffers.copiedBuffer(data);

		long validCnt = 0;

		switch (CType.valueOf(countIdxRecord.getType())) {

		case UCHAR:
			validCnt = buff.readUnsignedByte();
			break;
		case USHORT:
			validCnt = buff.readUnsignedShort();
			break;
		case UINT:
			validCnt = buff.readUnsignedInt();
			break;

		default:
			break;
		}

		for (int vaildIdx = 1; vaildIdx <= validCnt; vaildIdx++) {

			for (int j2 = (nowIndex + 1); j2 <= (nowIndex + childCnt); j2++) {

				Record childRecord = trgContext.get(j2);
				int rank = Integer.parseInt(childRecord.getRank());
				Record srcRecord = srcContext.get(rank);
				byte[] childData = srcRecord.getData(vaildIdx);

				if (childData == null) {
					childData = srcRecord.getData();

				}
				byte[] convertBytes = null;
				if (childRecord.getLength() == -1) {
					int countNo = childRecord.getCountNo();
					Record varTagRecord = trgContext.get(countNo);

					// CHAR 로 간주함
					convertBytes = TypeConversion.convertMapToVarChar4List(srcRecord.getData(), varTagRecord, vaildIdx,
							srcOrder, trgOrder);
					retBuff.writeBytes(convertBytes);
				} else {
					convertBytes = TypeConversion.convertMapToCType(childData, srcRecord, childRecord, srcOrder,
							trgOrder);
					trgRecord.putData(vaildIdx, convertBytes);
					retBuff.writeBytes(convertBytes);
				}

				if (logger.isDebugEnabled())
					logger.debug(childRecord.toStringSimple(convertBytes));

			}
		}

		nowIndex += childCnt;
		return nowIndex;
	}

	private int convertListorMapAscii(RecordContext srcContext, RecordContext trgContext, ChannelBuffer retBuff,
			int nowIndex, Record trgRecord) throws UnsupportedEncodingException {

		int childCnt = trgRecord.getChildCount();
		int cntIdx = trgRecord.getCountNo();
		long validCnt = 0;

		if (trgRecord.getType().equals("MAP")) {
			validCnt = 1;
		} else if (trgRecord.getType().equals("LIST")) {
			if (trgRecord.getLength() == 0) {
				try {
					Record countIdxRecord = trgContext.get(cntIdx);
					byte[] data = countIdxRecord.getData();
					ChannelBuffer buff = ChannelBuffers.copiedBuffer(data);

					int validLength = countIdxRecord.getLength();
					byte[] validData = new byte[validLength];
					switch (AsciiType.valueOf(countIdxRecord.getType())) {

					case N:
					case A:
						
						buff.readBytes(validData);
						validCnt = Integer.valueOf(new String(validData).trim());
						break;

					default:
						break;
					}
				} catch (Exception e) {
					validCnt = srcContext.get(nowIndex).getLength();
				}

			} else {
				validCnt = srcContext.get(nowIndex).getLength();
			}
		}
		for (int vaildIdx = 1; vaildIdx <= validCnt; vaildIdx++) {
			for (int j2 = (nowIndex + 1); j2 <= (nowIndex + childCnt); j2++) {

				Record childRecord = trgContext.get(j2);
				int rank = Integer.parseInt(childRecord.getRank());
				Record srcRecord = srcContext.get(rank);
				byte[] childData = srcRecord.getData(vaildIdx);

				if (childData == null) {
					childData = srcRecord.getData();

				}
				byte[] convertBytes = null;
				if (childRecord.getLength() == -1) {
					int countNo = childRecord.getCountNo();
					Record varTagRecord = trgContext.get(countNo);
					convertBytes = TypeConversion.convertMapToVarAscii4List(srcRecord, childRecord, varTagRecord,
							vaildIdx);
					retBuff.writeBytes(convertBytes);
				} else {
					convertBytes = TypeConversion.asciiConvert(childData, childRecord.getType(),
							childRecord.getLength());
					trgRecord.putData(vaildIdx, convertBytes);
					retBuff.writeBytes(convertBytes);
				}

				if (logger.isDebugEnabled())
					logger.debug(childRecord.toStringSimple(convertBytes));

			}
		}

		nowIndex += childCnt;
		return nowIndex;
	}

	private int convertListorMap(RecordContext srcContext, RecordContext trgContext, Map inDataMap, Map outDataMap,
			int nowIndex, Record trgRecord) throws UnsupportedEncodingException {

		int nowRank = Integer.parseInt(trgRecord.getRank());
		Record inNowRecord = srcContext.get(nowRank);
		String inKey = inNowRecord.getName();
		Object obj = inDataMap.get(inKey);
		if (logger.isDebugEnabled())
			logger.debug(trgContext.get(nowIndex).toStringSimple());

		if (obj instanceof Map) {
			Map inMap = (Map) obj;
			// 몇건 잇는지 값
			Map childMap = new LinkedHashMap();
			int childCnt = trgRecord.getChildCount();
			// list는 건너띠고
			// list 다음부터 계산
			for (int j2 = (nowIndex + 1); j2 <= (nowIndex + childCnt); j2++) {

				Record childRecord = trgContext.get(j2);
				int rank = Integer.parseInt(childRecord.getRank());
				Record srcRecord = srcContext.get(rank);
				byte[] childData = srcRecord.getData();
				int childLength = childRecord.getLength();

				if (childData == null) {
					childData = srcRecord.getData();
				}

				Object convertObj = TypeConversion.convertMapToMap(childData, srcRecord, childRecord, srcOrder,	trgOrder);
				childMap.put(childRecord.getName(), convertObj);

				if (logger.isDebugEnabled())
					logger.debug(childRecord.toStringSimple(convertObj));

			}

			outDataMap.put(trgRecord.getName(), childMap);
			nowIndex += childCnt;
			return nowIndex;

		} else {
			List<Map> inDataList = (List<Map>) obj;
			int childCnt = trgRecord.getChildCount();
			if (inDataList != null) {
				long validCnt = inDataList.size();
				// 몇건 잇는지 값

				List childList = new ArrayList();

				Map childMap;
				// list는 건너띠고
				for (int vaildIdx = 1; vaildIdx <= validCnt; vaildIdx++) {
					childMap = new LinkedHashMap();

					// list 다음부터 계산
					for (int j2 = (nowIndex + 1); j2 <= (nowIndex + childCnt); j2++) {

						Record childRecord = trgContext.get(j2);
						int rank = Integer.parseInt(childRecord.getRank());
						Record srcRecord = srcContext.get(rank);
						byte[] childData = srcRecord.getData(vaildIdx);
						int childLength = childRecord.getLength();
						// logger.info("차일드 데이터1 : " + new String(childData) );

						if (childData == null) {
							childData = srcRecord.getData();
						}

						// logger.info("차일드 데이터2 : " + new String(childData) );

						Object convertObj = TypeConversion.convertMapToMap(childData, srcRecord, childRecord, srcOrder,
								trgOrder);
						childMap.put(childRecord.getName(), convertObj);

						if (logger.isDebugEnabled())
							logger.debug(childRecord.toStringSimple(convertObj));

					}
					childList.add(childMap);
				}

				outDataMap.put(trgRecord.getName(), childList);
			}
			nowIndex += childCnt;
			return nowIndex;
		}

	}

	public static void main(String[] args) {
		System.out.println(new BigDecimal(1422144121211233213L));
	}
}
