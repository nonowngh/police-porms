package mb.fw.transformation.engine;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import mb.fw.transformation.exception.MCIException;
import mb.fw.transformation.form.RecordContext;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Xml type Support
 *
 * @author clupine
 *
 */
public class XmlEngine {

	private static Logger logger = LoggerFactory.getLogger(XmlEngine.class);

	ByteOrder srcOrder = ByteOrder.BIG_ENDIAN;
	ByteOrder trgOrder = ByteOrder.BIG_ENDIAN;

	XStream xStream = new XStream(new DomDriver());

	MapEngine supportEngine = new MapEngine();

	public void setSrcOrder(ByteOrder srcOrder) {
		this.srcOrder = srcOrder;
	}

	public void setTrgOrder(ByteOrder trgOrder) {
		this.trgOrder = trgOrder;
	}

	/**
	 * XML -> ASCII SUPPORT
	 * @param srcContext
	 * @param trgContext
	 * @return byte[]
	 * @throws MCIException
	 */
	public byte[] conversionToAsciiBytes(String xml, RecordContext srcContext, RecordContext trgContext) throws MCIException,UnsupportedEncodingException {
		Map srcMap = (Map) xStream.fromXML(xml);
		return supportEngine.conversionToAsciiBytes(srcMap, srcContext, trgContext);
	}

	public String conversionToVMXML(String xml, RecordContext srcContext, RecordContext trgContext) throws Exception {
		Map srcMap = (Map) xStream.fromXML(xml);
		return supportEngine.conversionToVMXml(srcMap, srcContext, trgContext);
	}

	/**
	 * Xml -> CTYPE    SUPPORT
	 * @param srcContext
	 * @param trgContext
	 * @return byte[]
	 * @throws MCIException
	 * @throws UnsupportedEncodingException
	 */
	public byte[] conversionToCType(String xml, RecordContext srcContext, RecordContext trgContext)
			throws MCIException, UnsupportedEncodingException {
		Map srcMap = (Map) xStream.fromXML(xml);
		return supportEngine.conversionToCType(srcMap, srcContext, trgContext);
	}

	public String conversionToXml(String xml, RecordContext srcContext, RecordContext trgContext)
			throws MCIException, UnsupportedEncodingException {
		Map map =	supportEngine.conversionToMap((Map) xStream.fromXML(xml), srcContext, trgContext);
		return xStream.toXML(map);
	}

	/**
	 * Xml -> Map    SUPPORT
	 * @param xml
	 * @param srcContext
	 * @param trgContext
	 * @return Map
	 * @throws MCIException
	 * @throws UnsupportedEncodingException
	 */
	public Map conversionToMap(String xml, RecordContext srcContext, RecordContext trgContext)
			throws MCIException, UnsupportedEncodingException {
		return supportEngine.conversionToMap((Map) xStream.fromXML(xml), srcContext, trgContext);
	}

	public String conversionToJson(String xml, RecordContext srcContext, RecordContext trgContext)
			throws MCIException, UnsupportedEncodingException {
		Map map = supportEngine.conversionToMap((Map) xStream.fromXML(xml), srcContext, trgContext);
		return JSONObject.toJSONString(map);
	}

	public static void main(String[] args) {
		XStream xStream = new XStream(new DomDriver());
		Map map = new LinkedHashMap();
		Map jdomstyle = new LinkedHashMap();
		jdomstyle.put("sender", "aaaa");
		jdomstyle.put("version", "bbbb");
		jdomstyle.put("ManagementNum", new BigDecimal("12345"));
		jdomstyle.put("DateTime", "dddd");
		jdomstyle.put("StatusCode", "eeee");
		jdomstyle.put("GCode", "ffff");
		jdomstyle.put("FromReleaseDt", "gggg");
		jdomstyle.put("ToReleaseDt", "hhhh");
		jdomstyle.put("OrderNumber", new BigDecimal("12345"));

		Map dom4jstyle = new LinkedHashMap();
		dom4jstyle.put("sender", "jjjj");
		dom4jstyle.put("version", "kkkk");
		dom4jstyle.put("ManagementNum", new BigDecimal("12345"));
		dom4jstyle.put("DateTime", "mmmm");
		dom4jstyle.put("StatusCode", "nnnn");
		dom4jstyle.put("GCode", "oooo");
		dom4jstyle.put("FromReleaseDt", "pppp");
		dom4jstyle.put("ToReleaseDt", "qqqq");
		dom4jstyle.put("OrderNumber", new BigDecimal("12345"));

		map.put("jdom-style", jdomstyle);
		map.put("dom4j-style", dom4jstyle);


		Map food = new LinkedHashMap();
		food.put("name", "Belgian Waffles");
		food.put("price", new BigDecimal(5000));
		food.put("description", "Two of our famous Belgian Waffles with plenty of real maple syrup");
		food.put("calories", new BigDecimal(650));

		Map food2 = new LinkedHashMap();
		food2.put("name", "Strawberry Belgian Waffles");
		food2.put("price", new BigDecimal(6000));
		food2.put("description", "Light Belgian waffles covered with strawberries and whipped cream");
		food2.put("calories", new BigDecimal(900));

		List breakfast = new ArrayList();
		breakfast.add(food);
		breakfast.add(food2);

		map.put("breakfast",breakfast );

		String xml = (String)xStream.toXML(map);

		System.out.println(xml);

	}


}
