package mb.fw.transformation.engine;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import mb.fw.transformation.exception.MCIException;
import mb.fw.transformation.form.RecordContext;
import mb.fw.transformation.util.XmlUtil4Dom4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.velocity.tools.generic.EscapeTool;
import org.dom4j.DocumentException;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

/**
 * Xml type Support
 *
 * @author clupine
 */
public class VMXmlEngine {

    private static Logger logger = LoggerFactory.getLogger(VMXmlEngine.class);

    ByteOrder inOrder = ByteOrder.BIG_ENDIAN;
    ByteOrder outOrder = ByteOrder.BIG_ENDIAN;

    static XStream xStream = new XStream(new DomDriver());

    SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");

    static MapEngine mapSupportEngine = new MapEngine();

    public void setSrcOrder(ByteOrder srcOrder) {
        this.inOrder = srcOrder;
    }

    public void setTrgOrder(ByteOrder trgOrder) {
        this.outOrder = trgOrder;
    }

    /**
     * XML -> ASCII SUPPORT
     *
     * @return byte[]
     * @throws MCIException
     * @throws IOException
     * @throws JDOMException
     * @throws ParseException
     * @throws DocumentException
     */

    public byte[] conversionToAsciiBytes(String xml, RecordContext inContext, RecordContext outContext) throws MCIException, JDOMException, IOException, ParseException, DocumentException {
        Map map = conversionToMap(xml, inContext, outContext);
        return mapSupportEngine.conversionToAsciiBytes(map, inContext, outContext);
    }

    /**
     * Xml -> CTYPE SUPPORT
     *
     * @return byte[]
     * @throws MCIException
     * @throws DocumentException
     * @throws JDOMException
     * @throws IOException
     * @throws ParseException
     */
    public byte[] conversionToCType(String xml, RecordContext inContext, RecordContext outContext) throws MCIException, ParseException, IOException, JDOMException, DocumentException {
        Map map = conversionToMap(xml, inContext, outContext);
        return mapSupportEngine.conversionToCType(map, inContext, outContext);
    }

    /**
     * Xml -> Map SUPPORT
     *
     * @param xml
     * @param inContext
     * @param outContext
     * @return Map
     * @throws MCIException
     * @throws ParseException
     * @throws IOException
     * @throws JDOMException
     * @throws DocumentException
     */
    public Map conversionToMap(String xml, RecordContext inContext, RecordContext outContext) throws MCIException, ParseException, IOException, JDOMException, DocumentException {
        /**
         * IN Processing
         */
        logger.info("VM-XML -> XML Start...... ==> \n" + xml);
        String retXml = convertVMXml(xml, inContext);
        logger.info("VM-XML -> XML End........ ==> \n" + retXml);
        logger.info("XML -> MAP Start......");
        Map inMap = (Map) xStream.fromXML(retXml);
//		mapSupportEngine.readOut(inMap, inContext, null,false);
        logger.info("XML -> MAP End....... ==> " + inMap);
        Map outMap = mapSupportEngine.conversionToMap(inMap, inContext, outContext);
//
        return outMap;
    }


    /**
     * VM-XML -> XML
     *
     * @param xml
     * @param inContext
     * @param outContext
     * @return
     * @throws MCIException
     * @throws ParseException
     * @throws IOException
     * @throws JDOMException
     * @throws DocumentException
     */
    public String conversionToXML(String xml, RecordContext inContext, RecordContext outContext) throws MCIException, ParseException, IOException, JDOMException, DocumentException {
        /**
         * IN Processing
         */
        logger.info("VM-XML -> XML Start...... ==> \n" + xml);
        String retXml = convertVMXml(xml, inContext);
        logger.info("VM-XML -> XML End........ ==> \n" + retXml);
        logger.info("XML -> MAP Start......");
        Map inMap = (Map) xStream.fromXML(retXml);
        logger.info("XML -> MAP End....... ==> " + inMap);
        Map outMap = mapSupportEngine.conversionToMap(inMap, inContext, outContext);
        return xStream.toXML(outMap);
    }


    /**
     * VM-XML ->JSON
     *
     * @param xml
     * @param inContext
     * @param outContext
     * @return
     * @throws MCIException
     * @throws ParseException
     * @throws IOException
     * @throws JDOMException
     * @throws DocumentException
     */
    public String conversionToJSON(String xml, RecordContext inContext, RecordContext outContext) throws MCIException, ParseException, IOException, JDOMException, DocumentException {
        Map map = conversionToMap(xml, inContext, outContext);
        return JSONObject.toJSONString(map);
    }

    /**
     * VM-XML -> VM-XML
     *
     * @param xml
     * @param inContext
     * @param outContext
     * @return
     * @throws MCIException
     * @throws ParseException
     * @throws IOException
     * @throws JDOMException
     * @throws DocumentException
     */
    public String conversionToVMXml(String xml, RecordContext inContext, RecordContext outContext) throws MCIException, ParseException, IOException, JDOMException, DocumentException {
        Map outMap = conversionToMap(xml, inContext, outContext);
        return convertVMXml(outMap, outContext);
    }


    /**
     * VM-XML -> XML로 컨버전
     *
     * @param xml
     * @param context
     * @return
     * @throws MCIException
     * @throws ParseException
     * @throws IOException
     * @throws JDOMException
     * @throws DocumentException
     */
    public String convertVMXml(String xml, RecordContext context) throws MCIException, ParseException, IOException, JDOMException, DocumentException {

        org.dom4j.Document doc4j = null;
        try {
            doc4j = XmlUtil4Dom4j.parseDoc(xml);
        } catch (DocumentException e) {
            throw e;
        }

        RuntimeServices rs = RuntimeSingleton.getRuntimeServices();
        // logger.info("record : " + inContext);
        String vm = context.getTmpData();
        // logger.info("vm : " + vm);
        StringReader sr = new StringReader(vm);
        SimpleNode sn = rs.parse(sr, "conversionToMap");

        Template template = new Template();
        template.setRuntimeServices(rs);
        template.setData(sn);
        template.initDocument();

        VelocityContext vc = new VelocityContext();

        StringReader reader = new StringReader(xml);
        // builder.setFactory(new AnakiaJDOMFactory());
        Document doc = builder.build(reader);
        List<Namespace> list = doc.getRootElement().getAdditionalNamespaces();
//		logger.info("namespace list : " + list);
        for (Namespace ns : list) {
            // logger.info("prefix : " + ns.getPrefix());
            vc.put(ns.getPrefix(), ns);
        }
        vc.put("jdom", doc);
        vc.put("dom4j", doc4j);
        vc.put("esc", new EscapeTool());
        StringWriter sw = new StringWriter();
        template.merge(vc, sw);

//		logger.info("convert XML : " + sw.toString());
        return sw.toString();
    }

    /**
     * out Map -> XML로 컨버전
     *
     * @param xml
     * @param context
     * @return
     * @throws MCIException
     * @throws ParseException
     * @throws IOException
     * @throws JDOMException
     * @throws DocumentException
     */
    public String convertVMXml(Map map, RecordContext context) throws MCIException, ParseException, IOException, JDOMException, DocumentException {


        RuntimeServices rs = RuntimeSingleton.getRuntimeServices();
        String vm = context.getTmpData();
        StringReader sr = new StringReader(vm);
        SimpleNode sn = rs.parse(sr, "conversionToXML");

        Template template = new Template();
        template.setRuntimeServices(rs);
        template.setData(sn);
        template.initDocument();

        VelocityContext vc = new VelocityContext();
        vc.put("map", map);
        vc.put("esc", new EscapeTool());
        StringWriter sw = new StringWriter();
        template.merge(vc, sw);

        return sw.toString();
    }


    // public String conversionToVMXml(String xml, RecordContext inContext,
    // RecordContext outContext) throws MCIException, ParseException,
    // IOException, JDOMException, DocumentException {
    // Map map = conversionToMap(xml, inContext);
    //
    // }
}
