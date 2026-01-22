package mb.fw.transformation.engine;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import mb.fw.transformation.form.RecordContext;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteOrder;
import java.util.Map;

/**
 * JSON type Support
 *
 * @author clupine
 *
 */
public class JSONEngine {

	private static Logger logger = LoggerFactory.getLogger(JSONEngine.class);

	ByteOrder srcOrder = ByteOrder.BIG_ENDIAN;
	ByteOrder trgOrder = ByteOrder.BIG_ENDIAN;



	XStream xStream = new XStream(new DomDriver());

	static MapEngine mapSupportEngine = new MapEngine();

	public void setSrcOrder(ByteOrder srcOrder) {
		this.srcOrder = srcOrder;
	}

	public void setTrgOrder(ByteOrder trgOrder) {
		this.trgOrder = trgOrder;
	}

	/**
	 *
	 * JSON -> ASCII SUPPORT
	 *
	 * @param json
	 * @param srcContext
	 * @param trgContext
	 * @return
	 * @throws Exception
	 */
	public byte[] conversionToAsciiBytes(String json, RecordContext srcContext, RecordContext trgContext) throws Exception {
		JSONParser jsonParser = new JSONParser();
		JSONObject inMap;
		try {
			inMap = (JSONObject) jsonParser.parse(json);
		} catch (org.json.simple.parser.ParseException e) {
			throw new Exception(e);
		}

		return mapSupportEngine.conversionToAsciiBytes(inMap, srcContext, trgContext);
	}

	/**
	 * JSON -> MAP SUPPORT
	 *
	 * @param json
	 * @param srcContext
	 * @param trgContext
	 * @return
	 * @throws Exception
	 */
	public Map conversionToMap(String json, RecordContext srcContext, RecordContext trgContext) throws Exception {
		JSONParser jsonParser = new JSONParser();
		JSONObject inMap;

		try {
			inMap = (JSONObject) jsonParser.parse(json);
		} catch (org.json.simple.parser.ParseException e) {
			throw new Exception(e);
		}

		return mapSupportEngine.conversionToMap(inMap, srcContext, trgContext);
	}

	public String conversionToXML(String json, RecordContext srcContext, RecordContext trgContext) throws Exception {
		JSONParser jsonParser = new JSONParser();
		JSONObject inMap;

		try {
			inMap = (JSONObject) jsonParser.parse(json);
		} catch (org.json.simple.parser.ParseException e) {
			throw new Exception(e);
		}

		Map map = mapSupportEngine.conversionToMap(inMap, srcContext, trgContext);
		return xStream.toXML(map);
	}

	public String conversionToJson(String json, RecordContext srcContext, RecordContext trgContext) throws Exception {
		JSONParser jsonParser = new JSONParser();
		JSONObject inMap;

		try {
			inMap = (JSONObject) jsonParser.parse(json);
		} catch (org.json.simple.parser.ParseException e) {
			throw new Exception(e);
		}

		Map map = mapSupportEngine.conversionToMap(inMap, srcContext, trgContext);
		return  JSONObject.toJSONString(map);
	}

	/**
	 * JSON -> VMXML SUPPORT
	 *
	 * @param json
	 * @param inContext
	 * @param outContext
	 * @return
	 * @throws Exception
	 */
	public String conversionToVMXml(String json, RecordContext inContext, RecordContext outContext) throws Exception {
		JSONParser jsonParser = new JSONParser();
		JSONObject inMap;
		try {
			inMap = (JSONObject) jsonParser.parse(json);
		} catch (org.json.simple.parser.ParseException e) {
			throw new Exception(e);
		}
		return mapSupportEngine.conversionToVMXml(inMap, inContext, outContext);
	}


	/**
	 *  JSON -> CTYPE SUPPORT
	 *
	 * @param json
	 * @param srcContext
	 * @param trgContext
	 * @return
	 * @throws Exception
	 */
	public byte[] conversionToCType(String json, RecordContext srcContext, RecordContext trgContext) throws Exception {
		JSONParser jsonParser = new JSONParser();
		JSONObject inMap;
		try {
			inMap = (JSONObject) jsonParser.parse(json);
		} catch (org.json.simple.parser.ParseException e) {
			throw new Exception(e);
		}

		return mapSupportEngine.conversionToCType(inMap, srcContext, trgContext);
	}

}
