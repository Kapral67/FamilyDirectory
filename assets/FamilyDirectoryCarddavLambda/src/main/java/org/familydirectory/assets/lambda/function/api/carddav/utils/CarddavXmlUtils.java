package org.familydirectory.assets.lambda.function.api.carddav.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.milton.http.Response;
import io.milton.http.exceptions.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNullElse;

@SuppressFBWarnings("EI_EXPOSE_REP")
public
enum CarddavXmlUtils {
    ;
    private static final String DAV_NS = "DAV:";
    public static final String CARDDAV_NS = "urn:ietf:params:xml:ns:carddav";

    // INPUT //

    @FunctionalInterface
    private interface XMLFunction<R> {
        R apply(XMLStreamReader r) throws XMLStreamException, IOException;
    }

    private static <T> T parseXml(Supplier<InputStream> inSupplier, XMLFunction<T> behavior, String exceptionMessage) throws BadRequestException {
        try (final var in = inSupplier.get()) {
            final var factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            final var reader = factory.createXMLStreamReader(in);
            RuntimeException badRequestCause = null;
            try {
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event != XMLStreamConstants.START_ELEMENT) {
                        continue;
                    }
                    return behavior.apply(reader);
                }
            } catch (RuntimeException e) {
                badRequestCause = e;
            } finally {
                reader.close();
            }
            throw new BadRequestException(exceptionMessage, badRequestCause);
        } catch (XMLStreamException e) {
            throw new BadRequestException("Invalid REPORT XML", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static QName parseReportRoot(Supplier<InputStream> inSupplier) throws BadRequestException {
        return parseXml(inSupplier, XMLStreamReader::getName, "REPORT body does not contain a root element");
    }

    public static Optional<String> parseSyncToken(Supplier<InputStream> inSupplier) throws BadRequestException {
        final XMLFunction<Optional<String>> behavior = reader -> {
            Optional<String> token = Optional.empty();
            for (boolean first = true; first || reader.hasNext(); first = false) {
                if (!first) reader.next();
                if (!reader.isStartElement() || !"sync-token".equals(reader.getLocalName())) {
                    continue;
                }
                String ns = reader.getNamespaceURI();
                if (!DAV_NS.equals(ns)) {
                    continue;
                }

                token = Optional.ofNullable(reader.getElementText())
                                .filter(Predicate.not(String::isBlank))
                                .map(String::trim);
                break;
            }
            return token;
        };
        return parseXml(inSupplier, behavior, "sync-collection report bad sync token");
    }

    public static List<String> parseMultigetHrefs(Supplier<InputStream> inSupplier) throws BadRequestException {
        final XMLFunction<List<String>> behavior = reader -> {
            final var hrefs = new ArrayList<String>();
            for (var firstIter = true; firstIter || reader.hasNext(); firstIter = false) {
                if (!firstIter) reader.next();
                if (!reader.isStartElement() || !"href".equals(reader.getLocalName())) {
                    continue;
                }
                final var href = requireNonNullElse(reader.getElementText(), "").trim();
                if (!href.isEmpty()) {
                    hrefs.add(href);
                }
            }
            if (hrefs.isEmpty()) {
                throw new NoSuchElementException();
            }
            return unmodifiableList(hrefs);
        };
        return parseXml(inSupplier, behavior, "addressbook-multiget REPORT missing DAV:href");
    }

    // OUTPUT //

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
            xw.writeEndElement();

            xw.writeEndDocument();
            xw.flush();
            xw.close();
            return sw.toString();
        } catch (XMLStreamException e) {
            throw new RuntimeException("Failed to build DAV multistatus XML", e);
        }
    }

    public static String renderValidSyncTokenError() {
        try {
            StringWriter sw = new StringWriter(1024);
            XMLStreamWriter xw = newXmlWriter(sw);

            xw.setPrefix("d", DAV_NS);
            xw.writeStartElement("d", "error", DAV_NS);
            xw.writeNamespace("d", DAV_NS);
            xw.writeEmptyElement("d", "valid-sync-token", DAV_NS);
            xw.writeEndElement();

            xw.writeEndDocument();
            xw.flush();
            xw.close();
            return sw.toString();
        } catch (XMLStreamException e) {
            throw new RuntimeException("Failed to build d:valid-sync-token error", e);
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
