package mb.fw.transformation.tool;

import mb.fw.transformation.form.Record;
import mb.fw.transformation.form.RecordContext;
import mb.fw.transformation.loader.ExcelFileMessageFormBoxLoader;
import mb.fw.transformation.type.AsciiType;
import mb.fw.transformation.type.CType;
import mb.fw.transformation.type.MapType;
import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlEngine;
import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Map;

public class ExpressionEvaluator {

	private static Logger logger = LoggerFactory.getLogger(ExcelFileMessageFormBoxLoader.class);

	private static final JexlEngine jexl = new JexlEngine();

	static {
		jexl.setCache(512);
		jexl.setLenient(false);
		jexl.setSilent(false);
	}

	public static void evaluate(RecordContext srcContext, ChannelBuffer retBuff, Record trgRecord, String expression, ByteOrder order)
			throws UnsupportedEncodingException {
		Expression e = jexl.createExpression(expression);

		if (CType.contains(trgRecord.getType())) {
			switch (CType.valueOf(trgRecord.getType())) {
			case CHAR: {
				String result = (String) e.evaluate(srcContext);
				logger.info("evaluate CHAR");
				byte[] convertBytes = TypeConversion.asciiConvert(result.getBytes(Charset.forName("KSC5601").toString()), trgRecord.getType(),
						trgRecord.getLength());
				retBuff.writeBytes(convertBytes);
				trgRecord.setData(convertBytes);
				if (logger.isDebugEnabled())
					logger.debug(trgRecord.toStringSimple());
			}
				break;
			case DOUBLE: {
				logger.debug("evaluate 구현 테스트중 DOUBLE : {}", e.evaluate(srcContext));
			}
				break;
			case FLOAT: {
				logger.debug("evaluate 구현 테스트중 FLOAT : {}", e.evaluate(srcContext));
			}
				break;
			case INT: {
				logger.debug("evaluate 구현 테스트중 INT : {}", e.evaluate(srcContext));
			}
				break;
			case LONG: {
				logger.debug("evaluate 구현 테스트중 LONG : {}", e.evaluate(srcContext));
			}
				break;
			case SHORT: {
				logger.debug("evaluate 구현 테스트중 SHORT : {}", e.evaluate(srcContext));
			}
				break;
			case UCHAR: {
				logger.debug("evaluate 구현 테스트중 UCHAR : {}", e.evaluate(srcContext));
			}
				break;
			case UINT: {
				logger.debug("evaluate 구현 테스트중 UINT : {}", e.evaluate(srcContext));
			}
				break;
			// case UINT24:
			// {
			// logger.debug("evaluate 구현 테스트중 UINT24 : {}",
			// e.evaluate(srcContext));
			// }
			// break;
			// case UINT48:
			// {
			// logger.debug("evaluate 구현 테스트중 UINT48 : {}",
			// e.evaluate(srcContext));
			// }
			// break;
			case ULONG: {
				logger.debug("evaluate 구현 테스트중 ULONG : {}", e.evaluate(srcContext));
			}
				break;
			case USHORT: {
				logger.debug("evaluate 구현 테스트중 USHORT : {}", e.evaluate(srcContext));
			}
				break;

			default:
				break;
			}
		} else if (AsciiType.contains(trgRecord.getType())) {
			switch (AsciiType.valueOf(trgRecord.getType())) {
			case A: {
				String result = (String) e.evaluate(srcContext);
//				logger.info("evaluate A");
				byte[] convertBytes = TypeConversion.asciiConvert(result.getBytes(Charset.forName("KSC5601").toString()), trgRecord.getType(),
						trgRecord.getLength());
				retBuff.writeBytes(convertBytes);
				trgRecord.setData(convertBytes);
				if (logger.isDebugEnabled())
					logger.debug(trgRecord.toStringSimple());
			}
				break;
			case H: {
				String result = (String) e.evaluate(srcContext);
//				logger.info("evaluate H");
				byte[] convertBytes = TypeConversion.asciiConvert(result.getBytes(Charset.forName("KSC5601").toString()), trgRecord.getType(),
						trgRecord.getLength());
				retBuff.writeBytes(convertBytes);
				trgRecord.setData(convertBytes);
				if (logger.isDebugEnabled())
					logger.debug(trgRecord.toStringSimple());
			}
				break;
			case N: {
				Object evaResult = e.evaluate(srcContext);
				String result = "";
				if(evaResult instanceof Integer){
					result = String.valueOf(evaResult);
				}else{
					result =(String) evaResult;
				}
				byte[] convertBytes = TypeConversion.asciiConvert(result.getBytes(Charset.forName("KSC5601").toString()), trgRecord.getType(),
						trgRecord.getLength());
				retBuff.writeBytes(convertBytes);
				trgRecord.setData(convertBytes);
				if (logger.isDebugEnabled())
					logger.debug(trgRecord.toStringSimple());
			}
				break;

			default:
				break;
			}

		} else if (MapType.contains(trgRecord.getType())) {
			switch (MapType.valueOf(trgRecord.getType())) {
			case BITSTR: {
				String result = (String) e.evaluate(srcContext);
				byte[] convertBytes = TypeConversion.asciiConvert(result.getBytes(Charset.forName("KSC5601").toString()), trgRecord.getType(),
						trgRecord.getLength());
				retBuff.writeBytes(convertBytes);
				trgRecord.setData(convertBytes);
				if (logger.isDebugEnabled())
					logger.debug(trgRecord.toStringSimple());
			}
				break;
			case BYTE: {
				String result = (String) e.evaluate(srcContext);
				byte[] convertBytes = TypeConversion.asciiConvert(result.getBytes(Charset.forName("KSC5601").toString()), trgRecord.getType(),
						trgRecord.getLength());
				retBuff.writeBytes(convertBytes);
				trgRecord.setData(convertBytes);
				if (logger.isDebugEnabled())
					logger.debug(trgRecord.toStringSimple());
			}
				break;
			case STRING: {
				String result = (String) e.evaluate(srcContext);
				byte[] convertBytes = TypeConversion.asciiConvert(result.getBytes(Charset.forName("KSC5601").toString()), trgRecord.getType(),
						trgRecord.getLength());
				retBuff.writeBytes(convertBytes);
				trgRecord.setData(convertBytes);
				if (logger.isDebugEnabled())
					logger.debug(trgRecord.toStringSimple());
			}
				break;
			default:
				break;
			}

		}

	}

	public static void evaluateMapToMap(RecordContext srcContext,Map map,  Record trgRecord, String expression, ByteOrder order)
			throws UnsupportedEncodingException {
		Expression e = jexl.createExpression(expression);

		if (MapType.contains(trgRecord.getType())) {
			switch (MapType.valueOf(trgRecord.getType())) {
			case BITSTR: {
				String result = (String) e.evaluate(srcContext);
				 map.put(trgRecord.getName(), result);
				trgRecord.setData(result.getBytes());
				if (logger.isDebugEnabled())
					logger.debug(trgRecord.toStringSimple());
			}
			break;
			case BYTE: {
				String result = (String) e.evaluate(srcContext);
				 map.put(trgRecord.getName(), result);
					trgRecord.setData(result.getBytes());
				if (logger.isDebugEnabled())
					logger.debug(trgRecord.toStringSimple());
			}
			break;
			case STRING: {
				String result = (String) e.evaluate(srcContext);
				 map.put(trgRecord.getName(), result);
					trgRecord.setData(result.getBytes());
				if (logger.isDebugEnabled())
					logger.debug(trgRecord.toStringSimple());
			}
			case DECIMAL :{
				String result = (String) e.evaluate(srcContext);
				 map.put(trgRecord.getName(), new Double(result));
					trgRecord.setData(result.getBytes());
				if (logger.isDebugEnabled())
					logger.debug(trgRecord.toStringSimple());
			}
			case INTEGER :{
				String result = (String) e.evaluate(srcContext);
				map.put(trgRecord.getName(), new Long(result));
				trgRecord.setData(result.getBytes());
				if (logger.isDebugEnabled())
					logger.debug(trgRecord.toStringSimple());
			}
			break;
			default:
				break;
			}

		}

	}


}
