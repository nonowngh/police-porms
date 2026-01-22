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
public class Base64DecInterceptor extends AbstractPhaseInterceptor<SoapMessage> {

    @Setter
    IFContext ifContext;

    Base64.Decoder decoder = Base64.getDecoder();

    public Base64DecInterceptor() {
        super(Phase.UNMARSHAL);
    }

    public void handleMessage(SoapMessage message) throws SoapFault {
        SOAPMessage doc = message.getContent(SOAPMessage.class);
        log.info("==== Base64DecInterceptor handleMessage() ====");
        SOAPBody soapbody = null;
        try {
            soapbody = doc.getSOAPBody();
            Node operation = getOperationNode(soapbody);
            String content = XPathUtils.nodeToXml(operation);
            log.debug("==== content: " + content);
            String prefix, encoded, suffix;

            int begin = content.indexOf(">") + 1;
            int end = content.lastIndexOf("<");
            prefix = content.substring(0, begin);
            encoded = content.substring(begin, end).trim();
            suffix = content.substring(end);
            log.info("==== encoded : " + encoded);
            byte[] decoded = decoder.decode(encoded);
            String decrypted = new String(decoded);
            log.info("==== decoded : " + new String(decoded));
            //암호화된 메시지로 다시 설정
            String body = addDocument(soapbody, operation, prefix, suffix, new String(decrypted));

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
    private String addDocument(SOAPBody soapbody, Node operation, String prefix,
                               String suffix, String body) throws Exception {
        String newOperationXml = prefix + body + suffix;
        log.info("==== import Body : " + newOperationXml);
        Document document = XPathUtils.convertTextToXmlDoc(newOperationXml);
        soapbody.removeChild(operation);
        soapbody.addDocument(document);
        return newOperationXml;
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