package au.com.nbnco.foundation.jobs.impl.embedded.mapping;

import au.com.nbnco.foundation.core.utils.CaptureAnalytics;
import au.com.nbnco.foundation.core.utils.PerformanceMetric;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.io.StringWriter;

@Slf4j
public class XSLHelper {

    private static final ThreadLocal<DocumentBuilderFactory> DOCUMENT_BUILDER_FACTORY_LOCAL = new ThreadLocal<DocumentBuilderFactory>() {
        @Override
        protected DocumentBuilderFactory initialValue() {
            return DocumentBuilderFactory.newInstance();
        }
    };

    private static final ThreadLocal<TransformerFactory> TRANSFORMER_FACTORY_LOCAL = new ThreadLocal<TransformerFactory>() {
        @Override
        protected TransformerFactory initialValue() {
            return TransformerFactory.newInstance();
        }
    };

    public static DocumentBuilderFactory documentBuilderFactory() {
        return DOCUMENT_BUILDER_FACTORY_LOCAL.get();
    }

    public static TransformerFactory tranformerFactory() {
        return TRANSFORMER_FACTORY_LOCAL.get();
    }

    @CaptureAnalytics
    @PerformanceMetric
    public static String transformXMLToHtml(final Document xml, final String specName) {
        documentBuilderFactory().setNamespaceAware(true);
        try (final InputStream stream = XSLHelper.class.getResourceAsStream("/workorderspecification/transform/" + specName + ".xsl")) {
            if (stream == null) {
                log.info("Cannot find the xlst template for work order spec {}", specName);
                return null;
            }
            final DocumentBuilder db = documentBuilderFactory().newDocumentBuilder();
            final Document parse = db.parse(stream);

            final Source xsltSource = new DOMSource(parse);
            final Transformer transformer = tranformerFactory().newTransformer(xsltSource);

            final DOMResult result = new DOMResult();
            final Source xmlSource = new DOMSource(xml);
            transformer.transform(xmlSource, result);
            return documentToString((Document) result.getNode());
        }
        catch (Exception e) {
            log.error("Exception", e);
            return null;
        }
    }

    @CaptureAnalytics
    @PerformanceMetric
    public static String documentToString(Document doc) {
        try {
            final StringWriter sw = new StringWriter();
            final Transformer transformer = tranformerFactory().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "html");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception e) {
            log.error("Exception ", e);
        }
        return null;
    }
}
