package mb.fw.atb.cxf.interceptor;

import com.indigo.esb.xml.XPathUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.config.sub.IFContext;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;
import java.util.Base64;

@Slf4j

public class Base64EncInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
    @Setter
    IFContext ifContext;

    Base64.Encoder encoder = Base64.getEncoder();

    public Base64EncInterceptor() {
        super(Phase.PRE_PROTOCOL_ENDING);
    }

    public void handleMessage(SoapMessage message) throws SoapFault {
        SOAPMessage doc = message.getContent(SOAPMessage.class);
        log.info("==== Base64EncInterceptor handleMessage() ====");
        SOAPBody soapbody = null;
        try {
            soapbody = doc.getSOAPBody();

            Node operation = getOperationNode(soapbody);

            String content = XPathUtils.nodeToXml(operation);
            log.debug("==== content: " + content);

            int begin = content.indexOf(">") + 1;
            int end = content.lastIndexOf("<");
            String prefix = content.substring(0, begin);
            String decoded = content.substring(begin, end).trim();
            String suffix = content.substring(end);
            log.info("==== decoded : " + new String(decoded));
            String encoded = encoder.encodeToString(decoded.getBytes());
            log.info("==== encoded : " + encoded);


            //암호화된 메시지로 다시 설정
            addDocument(soapbody, operation, prefix, suffix, encoded);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            doResults(doc, message);
        } catch (SOAPException e) {
            throw new RuntimeException(e);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

    }

    public void doResults(SOAPMessage doc, SoapMessage msg)
            throws SOAPException, XMLStreamException {
        SOAPBody body = doc.getSOAPBody();

        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new DOMSource(body));
        // advance just past body
        int evt = reader.next();
        int i = 0;
        while (reader.hasNext() && i < 1
                && (evt != XMLStreamConstants.END_ELEMENT || evt != XMLStreamConstants.START_ELEMENT)) {
            reader.next();
            i++;
        }
        msg.setContent(XMLStreamReader.class, reader);
    }

    /**
     * 암복호화된 문을 soapbody에 추가하는 함수
     *
     * @param soapbody
     * @param operation 기존에 있는 node
     * @param prefix    startTag
     * @param suffix    endTag
     * @param body      암복호화된 문
     * @throws Exception
     */
    private void addDocument(SOAPBody soapbody, Node operation, String prefix,
                               String suffix, String body) throws Exception {
        String newOperationXml = prefix + body + suffix;
        log.info("==== import Body : " + newOperationXml);
        Document document = XPathUtils.convertTextToXmlDoc(newOperationXml);
        soapbody.removeChild(operation);
        soapbody.addDocument(document);
    }

    /**
     * soapbody에서 operation node를 가지고 오는 함수
     *
     * @param soapbody
     * @return
     */
    private Node getOperationNode(SOAPBody soapbody) {
        Node operation = soapbody.getFirstChild();

        String localName = operation.getLocalName();
        if (localName == null) {
            operation = operation.getNextSibling();
        }
        return operation;
    }
}