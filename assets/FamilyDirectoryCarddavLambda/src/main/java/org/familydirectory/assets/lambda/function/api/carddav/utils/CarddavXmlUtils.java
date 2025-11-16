package org.familydirectory.assets.lambda.function.api.carddav.utils;

import io.milton.http.Response;

import java.net.URI;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

public
enum CarddavXmlUtils {
    ;
    private static final String DAV_NS = "DAV:";
    public static final String CARDDAV_NS = "urn:ietf:params:xml:ns:carddav";

    private static XMLStreamWriter newXmlWriter(StringWriter sw) throws XMLStreamException {
        XMLOutputFactory factory = XMLOutputFactory.newFactory();
        XMLStreamWriter xw = factory.createXMLStreamWriter(sw);
        xw.writeStartDocument("UTF-8", "1.0");
        return xw;
    }

    private static void startMultistatus(XMLStreamWriter xw) throws XMLStreamException {
        xw.setPrefix("d", DAV_NS);
        xw.setPrefix("C", CARDDAV_NS);
        xw.writeStartElement("d", "multistatus", DAV_NS);
        xw.writeNamespace("d", DAV_NS);
        xw.writeNamespace("C", CARDDAV_NS);
    }

    private static void endMultistatus(XMLStreamWriter xw) throws XMLStreamException {
        xw.writeEndElement();
        xw.writeEndDocument();
    }

    private static void writeResponse(XMLStreamWriter xw, DavResponse r) throws XMLStreamException {
        xw.writeStartElement("d", "response", DAV_NS);

        xw.writeStartElement("d", "href", DAV_NS);
        xw.writeCharacters(r.href());
        xw.writeEndElement();

        for (DavPropStat ps : r.propStats()) {
            writePropStat(xw, ps);
        }

        xw.writeEndElement(); // </d:response>
    }

    private static void writePropStat(XMLStreamWriter xw, DavPropStat ps) throws XMLStreamException {
        xw.writeStartElement("d", "propstat", DAV_NS);

        xw.writeStartElement("d", "prop", DAV_NS);
        for (DavProperty p : ps.properties()) {
            writeProperty(xw, p);
        }
        xw.writeEndElement();

        xw.writeStartElement("d", "status", DAV_NS);
        xw.writeCharacters(ps.status().toString());
        xw.writeEndElement();

        xw.writeEndElement();
    }

    private static void writeProperty(XMLStreamWriter xw, DavProperty p) throws XMLStreamException {
        String ns = "d".equals(p.prefix()) ? DAV_NS : CARDDAV_NS;

        xw.writeStartElement(p.prefix(), p.localName(), ns);

        if (p.attributes() != null) {
            for (var e : p.attributes().entrySet()) {
                xw.writeAttribute(e.getKey(), e.getValue());
            }
        }

        if (p.children() != null && !p.children().isEmpty()) {
            for (DavProperty child : p.children()) {
                writeProperty(xw, child);
            }
        } else if (p.textContent() != null && !p.textContent().isEmpty()) {
            xw.writeCharacters(p.textContent());
        }

        xw.writeEndElement();
    }

    public static String renderMultistatus(List<DavResponse> responses) {
        return renderMultistatus(responses, null);
    }

    public static String renderMultistatus(List<DavResponse> responses, URI syncToken) {
        try {
            StringWriter sw = new StringWriter(1024);
            XMLStreamWriter xw = newXmlWriter(sw);

            startMultistatus(xw);

            for (DavResponse r : responses) {
                writeResponse(xw, r);
            }

            if (syncToken != null) {
                xw.writeStartElement("d", "sync-token", DAV_NS);
                xw.writeCharacters(syncToken.toString());
                xw.writeEndElement();
            }

            endMultistatus(xw);
            xw.flush();
            xw.close();
            return sw.toString();
        } catch (XMLStreamException e) {
            throw new RuntimeException("Failed to build DAV multistatus XML", e);
        }
    }

    /**
     * d:prop values.
     * prefix: "d" or "C"
     * localName: "getetag", "address-data", ...
     * textContent: null => empty element (<d:x />)
     * attributes: may be empty
     */
    public record DavProperty(String prefix, String localName, String textContent, Map<String, String> attributes,
                              List<DavProperty> children) {}

    /**
     * Single <d:propstat> block.
     */
    public record DavPropStat(Response.Status status, List<DavProperty> properties) {}

    /**
     * Single <d:response> (href + propstats).
     */
    public record DavResponse(String href, List<DavPropStat> propStats) {}

    public static DavProperty dParent(String name, List<DavProperty> children) {
        return new DavProperty("d", name, null, Map.of(), children);
    }

    public static DavProperty dProp(String name, String value) {
        return new DavProperty("d", name, value, Map.of(), List.of());
    }

    public static DavProperty dEmpty(String name) {
        return new DavProperty("d", name, null, Map.of(), List.of());
    }

    public static DavProperty cParent(String name, List<DavProperty> children) {
        return new DavProperty("C", name, null, Map.of(), children);
    }

    public static DavProperty cProp(String name, String value, Map<String, String> attrs) {
        return new DavProperty("C", name, value, attrs == null ? Map.of() : attrs, List.of());
    }

    public static DavProperty cEmpty(String name) {
        return new DavProperty("C", name, null, Map.of(), List.of());
    }

    public static DavPropStat okPropstat(List<DavProperty> props) {
        return new DavPropStat(Response.Status.SC_OK, props);
    }

    public static DavPropStat statusPropstat(Response.Status status, List<DavProperty> props) {
        return new DavPropStat(status, props);
    }
}
