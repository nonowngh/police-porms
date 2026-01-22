package mb.fw.transformation.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * @author clupine
 *
 */
public class XmlUtil4Dom4j {

	private final static Log log = LogFactory.getLog("XmlUtil4Dom4j");

	private String fileStr = null;
	private HashMap xmlmap = null;

	public void setFileStr(String fileStr) {
		this.fileStr = fileStr;
	}

	public void setXmlmap(HashMap xmlmap) {
		this.xmlmap = xmlmap;
	}

    public static final String ENCODING = "euc-kr";

	public static Document parseDoc(String xmlString) throws DocumentException {

		SAXReader saxReader = null;
		StringReader reader = null;
		Document doc = null;

		try {
			reader = new StringReader(xmlString);
			saxReader = new SAXReader();
			doc = saxReader.read(reader);
			doc.setXMLEncoding(ENCODING);
		} catch(DocumentException e) {
			log.error("XML Parsing error : ", e);
			throw e;
		}
		return doc;
	}

	public static Document parseDoc(String xmlString , String encoding) throws DocumentException {

		SAXReader saxReader = null;
		StringReader reader = null;
		Document doc = null;
		try {
			reader = new StringReader(xmlString);
			saxReader = new SAXReader();
			doc = saxReader.read(reader);
			doc.setXMLEncoding(encoding);
		} catch(DocumentException e) {
			log.error("XML Parsing error : ", e);
			throw e;
		}
		return doc;
	}

	public static Document parseXml(File file) throws DocumentException {

		SAXReader saxReader = null;
		Document doc = null;
		try {
			saxReader = new SAXReader();
			doc = saxReader.read(file);
			doc.setXMLEncoding(ENCODING);
			return doc;
		} catch(DocumentException e) {
			log.error("XML Parsing error : ", e);
			throw e;
		}
	}
	public static String print(Document doc) throws Exception {
		StringWriter sw = new StringWriter();
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setEncoding(doc.getXMLEncoding());
		XMLWriter writer = new XMLWriter(sw, format);
		writer.write(doc);

		return sw.toString();
	}
	public static String print(Element el) throws Exception {
		StringWriter sw = new StringWriter();
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setEncoding(el.getDocument().getXMLEncoding());
		XMLWriter writer = new XMLWriter(sw, format);
		writer.write(el);

		return sw.toString();
	}
	public static String print(Node node) throws Exception {
		StringWriter sw = new StringWriter();
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setEncoding(node.getDocument().getXMLEncoding());
		XMLWriter writer = new XMLWriter(sw, format);
		writer.write(node);

		return sw.toString();
	}

	public static HashMap xml2Mapping(String xmlStr){
		HashMap map = null;
		try {
			Document doc = parseDoc(xmlStr);
			Element rootEl = doc.getRootElement();
			map = makeEl2Map(rootEl);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return map;
	}

	public static HashMap<String,Object> xml2Mapping(Document doc){
		HashMap<String,Object> map = new HashMap<String,Object>();
		Element rootEl = doc.getRootElement();
		map = makeEl2Map(rootEl);
		return map;
	}
	private static LinkedHashMap<String,Object> makeEl2Map(Element _el) {
//		if(log.isDebugEnabled())
//			log.debug("Element length : "  + _el.elements().size());
		LinkedHashMap _childMap = new LinkedHashMap();
		for (Iterator iterator2 = _el.elementIterator(); iterator2.hasNext();) {
			Element _elTmp = (Element) iterator2.next();
			if(_elTmp.elements().size() > 0){
				_childMap.put(_elTmp.getName(), makeEl2Map(_elTmp));
			}else{
				String type = "";
				try{
					type = _elTmp.attribute("type").getText();
				}catch (Exception e) {
					//type없으면 기본값으로 처리하도록함
				}

				switch (type) {
				case "STRING":
					_childMap.put(_elTmp.getName(), _elTmp.getStringValue());
					break;
				case "BIGDECIMAL":
					String value = _elTmp.getStringValue();
					_childMap.put(_elTmp.getName(), new BigDecimal(value));
					break;

				default:
					_childMap.put(_elTmp.getName(), _elTmp.getStringValue());
					break;
				}


			}
		}
		return _childMap;
	}

	public void xml2Mapping() throws DocumentException, Exception{
		File file = new File(".");
		log.info("xml2Mapping : "+file.getAbsoluteFile() + ", xml2Mapping : " + file.getPath());
		xmlmap.putAll(xml2Mapping(XmlUtil4Dom4j.parseXml(new File(fileStr))));
	}

	public static void main(String[] args) throws DocumentException {

		File file = new File("mapper.xml");
		Document doc = XmlUtil4Dom4j.parseXml(file);
		log.info("map : " + XmlUtil4Dom4j.makeEl2Map(doc.getRootElement()));

//		try {
//			HashMap map = XmlUtil4Dom4j.xml2Mapping(XmlUtil4Dom4j.print(XmlUtil4Dom4j.parseXml(file)));
//			log.info("map : " + map);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}


}
