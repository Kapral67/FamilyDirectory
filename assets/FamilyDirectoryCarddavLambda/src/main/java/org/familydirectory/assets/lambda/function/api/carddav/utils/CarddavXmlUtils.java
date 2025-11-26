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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
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
import org.jetbrains.annotations.Nullable;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNullElse;

@SuppressFBWarnings("EI_EXPOSE_REP")
public
enum CarddavXmlUtils {
    ;
    public static final String DAV_NS = "DAV:";
    public static final String CARDDAV_NS = "urn:ietf:params:xml:ns:carddav";
    public static final String CS_NS = "http://calendarserver.org/ns/";

    // INPUT //

    public record PropFindRequest(Kind kind, List<QName> properties) {
        public enum Kind {
            ALL, NAME, LIST
        }
    }

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

    public static
    PropFindRequest parsePropFind (Supplier<InputStream> inSupplier) throws BadRequestException {
        XMLFunction<PropFindRequest> behavior = reader -> {
            QName root = reader.getName();
            if (!DAV_NS.equals(root.getNamespaceURI()) || !"propfind".equals(root.getLocalPart())) {
                throw new NoSuchElementException();
            }

            while (reader.hasNext()) {
                int event = reader.next();
                if (event != XMLStreamConstants.START_ELEMENT) {
                    if (event == XMLStreamConstants.END_ELEMENT && "propfind".equals(reader.getLocalName()) && DAV_NS.equals(reader.getNamespaceURI())) {
                        return new PropFindRequest(PropFindRequest.Kind.ALL, List.of());
                    }
                    continue;
                }

                QName child = reader.getName();
                String ln = child.getLocalPart();
                String ns = child.getNamespaceURI();

                if (DAV_NS.equals(ns) && "allprop".equals(ln)) {
                    return new PropFindRequest(PropFindRequest.Kind.ALL, List.of());
                }
                if (DAV_NS.equals(ns) && "propname".equals(ln)) {
                    return new PropFindRequest(PropFindRequest.Kind.NAME, List.of());
                }
                if (DAV_NS.equals(ns) && "prop".equals(ln)) {
                    List<QName> props = new ArrayList<>();
                    int depth = 1;
                    while (reader.hasNext() && depth > 0) {
                        int ev = reader.next();
                        if (ev == XMLStreamConstants.START_ELEMENT) {
                            depth++;
                            QName propName = reader.getName();
                            props.add(propName);
                        } else if (ev == XMLStreamConstants.END_ELEMENT) {
                            depth--;
                        }
                    }
                    return new PropFindRequest(PropFindRequest.Kind.LIST, unmodifiableList(props));
                }
            }

            throw new RuntimeException();
        };

        return parseXml(inSupplier, behavior, "PROPFIND body does not contain DAV:propfind root");
    }

    public static List<QName> parseReportProps(Supplier<InputStream> inSupplier) throws BadRequestException {
        final XMLFunction<List<QName>> behavior = reader -> {
            final var props = new ArrayList<QName>();

            for (boolean first = true; first || reader.hasNext(); first = false) {
                if (!first) reader.next();
                if (!reader.isStartElement() || !DAV_NS.equals(reader.getNamespaceURI()) || !"prop".equals(reader.getLocalName())) {
                    continue;
                }

                int depth = 1;
                while (reader.hasNext() && depth > 0) {
                    int ev = reader.next();
                    if (ev == XMLStreamConstants.START_ELEMENT) {
                        if (depth == 1) {
                            props.add(reader.getName());
                        }
                        depth++;
                    } else if (ev == XMLStreamConstants.END_ELEMENT) {
                        depth--;
                    }
                }
                break;
            }

            return unmodifiableList(props);
        };

        return parseXml(inSupplier, behavior, "REPORT body could not be parsed for DAV:prop");
    }


    // OUTPUT //

    private static final class Renderer {
        private final StringWriter sw;
        private final XMLStreamWriter xw;
        private final Map<String, String> nsToPrefix = new HashMap<>();

        Renderer() throws XMLStreamException {
            this.sw = new StringWriter(1024);
            this.xw = XMLOutputFactory.newFactory().createXMLStreamWriter(sw);

            nsToPrefix.put(DAV_NS, "d");
            nsToPrefix.put(CARDDAV_NS, "C");
            nsToPrefix.put(CS_NS, "A");

            xw.writeStartDocument("UTF-8", "1.0");
        }

        String renderValidSyncTokenError() throws XMLStreamException {
            final var d = prefixFor(DAV_NS);
            xw.setPrefix(d, DAV_NS);

            writeError(new DavError(singletonList(dEmpty("valid-sync-token"))));

            xw.writeEndDocument();
            xw.flush();
            xw.close();
            return sw.toString();
        }

        String render(List<DavResponse> responses, URI syncToken) throws XMLStreamException {
            startMultistatus();
            for (DavResponse r : responses) {
                writeResponse(r);
            }

            if (syncToken != null) {
                String dPrefix = prefixFor(DAV_NS);
                xw.writeStartElement(dPrefix, "sync-token", DAV_NS);
                xw.writeCharacters(syncToken.toString());
                xw.writeEndElement();
            }

            xw.writeEndElement();   // </d:multistatus>
            xw.writeEndDocument();
            xw.flush();
            xw.close();
            return sw.toString();
        }

        private void startMultistatus() throws XMLStreamException {
            String dPrefix = prefixFor(DAV_NS);
            String cPrefix = prefixFor(CARDDAV_NS);

            xw.setPrefix(dPrefix, DAV_NS);
            xw.setPrefix(cPrefix, CARDDAV_NS);

            xw.writeStartElement(dPrefix, "multistatus", DAV_NS);
            xw.writeNamespace(dPrefix, DAV_NS);
            xw.writeNamespace(cPrefix, CARDDAV_NS);
        }

        private String prefixFor(String ns) throws XMLStreamException {
            String existing = nsToPrefix.get(ns);
            if (existing != null) return existing;

            int idx = 0;
            String candidate;
            do {
                candidate = "x" + idx++;
            } while (nsToPrefix.containsValue(candidate));

            nsToPrefix.put(ns, candidate);
            xw.setPrefix(candidate, ns);
            return candidate;
        }

        private void writeResponse(DavResponse r) throws XMLStreamException {
            String dPrefix = prefixFor(DAV_NS);

            xw.writeStartElement(dPrefix, "response", DAV_NS);

            xw.writeStartElement(dPrefix, "href", DAV_NS);
            xw.writeCharacters(r.href());
            xw.writeEndElement();

            for (DavPropStat ps : r.propStats()) {
                writePropStat(ps);
            }

            if (r.error() != null) {
                writeError(r.error());
            }

            xw.writeEndElement(); // </d:response>
        }

        private void writeError(DavError err) throws XMLStreamException {
            String dPrefix = prefixFor(DAV_NS);
            xw.writeStartElement(dPrefix, "error", DAV_NS);
            xw.writeNamespace(dPrefix, DAV_NS);
            for (final var child : Objects.<List<DavProperty>>requireNonNullElse(err.properties(), emptyList())) {
                writeProperty(child);
            }
            xw.writeEndElement();
        }

        private void writePropStat(DavPropStat ps) throws XMLStreamException {
            String dPrefix = prefixFor(DAV_NS);

            xw.writeStartElement(dPrefix, "propstat", DAV_NS);

            xw.writeStartElement(dPrefix, "prop", DAV_NS);
            for (DavProperty p : ps.properties()) {
                writeProperty(p);
            }
            xw.writeEndElement();

            xw.writeStartElement(dPrefix, "status", DAV_NS);
            xw.writeCharacters(ps.status().toString());
            xw.writeEndElement();

            xw.writeEndElement();
        }

        private void writeProperty(DavProperty p) throws XMLStreamException {
            QName name = p.qName();
            String ns    = name.getNamespaceURI();
            String local = name.getLocalPart();

            String prefix = prefixFor(ns);

            xw.writeStartElement(prefix, local, ns);

            xw.writeNamespace(prefix, ns);

            if (p.attributes() != null) {
                for (var e : p.attributes().entrySet()) {
                    xw.writeAttribute(e.getKey(), e.getValue());
                }
            }

            if (p.children() != null && !p.children().isEmpty()) {
                for (DavProperty child : p.children()) {
                    writeProperty(child);
                }
            } else if (p.textContent() != null && !p.textContent().isEmpty()) {
                xw.writeCharacters(p.textContent());
            }

            xw.writeEndElement();
        }
    }

    public static String renderMultistatus(List<DavResponse> responses) {
        return renderMultistatus(responses, null);
    }

    public static String renderMultistatus(List<DavResponse> responses, URI syncToken) {
        try {
            final var r = new Renderer();
            return r.render(responses, syncToken);
        } catch (XMLStreamException e) {
            throw new RuntimeException("Failed to build DAV multistatus XML", e);
        }
    }

    public static String renderValidSyncTokenError() {
        try {
            final var r = new Renderer();
            return r.renderValidSyncTokenError();
        } catch (XMLStreamException e) {
            throw new RuntimeException("Failed to build d:valid-sync-token error", e);
        }
    }

    /**
     * qName: prefix + localName
     * textContent: null => empty element (<d:x />)
     * attributes: may be empty
     */
    public record DavProperty(QName qName, String textContent, Map<String, String> attributes,
                              List<DavProperty> children) {}

    /**
     * Single <d:propstat> block.
     */
    public record DavPropStat(Response.Status status, List<DavProperty> properties) {}

    public record DavError(List<DavProperty> properties) {}

    /**
     * Single <d:response> (href + propstats ?+ error) .
     */
    public record DavResponse(String href, List<DavPropStat> propStats, @Nullable DavError error) {
        public DavResponse(String href, List<DavPropStat> propStats) {
            this(href, propStats, null);
        }
    }

    public static DavProperty dParent(String name, List<DavProperty> children) {
        return new DavProperty(new QName(DAV_NS, name), null, Map.of(), children);
    }

    public static DavProperty dProp(String name, String value) {
        return new DavProperty(new QName(DAV_NS, name), value, Map.of(), List.of());
    }

    public static DavProperty dEmpty(String name) {
        return new DavProperty(new QName(DAV_NS, name), null, Map.of(), List.of());
    }

    public static DavProperty cParent(String name, List<DavProperty> children) {
        return new DavProperty(new QName(CARDDAV_NS, name), null, Map.of(), children);
    }

    public static DavProperty cProp(String name, String value, Map<String, String> attrs) {
        return new DavProperty(new QName(CARDDAV_NS, name), value, attrs == null ? Map.of() : attrs, List.of());
    }

    public static DavProperty cEmpty(String name) {
        return new DavProperty(new QName(CARDDAV_NS, name), null, Map.of(), List.of());
    }

    public static DavPropStat okPropstat(List<DavProperty> props) {
        return new DavPropStat(Response.Status.SC_OK, props);
    }

    public static DavPropStat statusPropstat(Response.Status status, List<DavProperty> props) {
        return new DavPropStat(status, props);
    }
}
