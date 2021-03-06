package fc.fp.util.xas;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/** This class provides conversion from a XAS EventSequence to
 * SAX events.
 */

// ctl: sorry for not writing tip-top code, this is because im in a slight hurry...

public class SaxBridge {

    private static Log log = LogFactory.getLog(SaxReader.class.getName());

    protected EventSequence es;

    public SaxBridge(EventSequence source) {
        es = source;
    }

    public static void output(EventSequence es, ContentHandler contentHandler) throws
        SAXException, IOException {
        (new SaxBridge(es)).output(contentHandler);
    }

    public void output(ContentHandler contentHandler) throws SAXException, IOException {
        if (es == null) {
            throw new SAXException("Must have an EventSequence to output");
        }
        Map prefixMapping = new HashMap();
        XmlReader r = new XmlReader(es);
        int[] textParams = new int[2];
        for (Event e = r.advance();
             e != null && e.getType() != Event.END_DOCUMENT; e = r.advance()) {
            int eventType = e.getType();
            switch (eventType) {
                case Event.START_DOCUMENT: {
                    if (log.isDebugEnabled()) {
                        log.debug("START_DOCUMENT");
                    }
                    if (contentHandler != null) {
                        contentHandler.startDocument();
                    }
                    break;
                }
                case Event.NAMESPACE_PREFIX: {
                    String prefix = e.getValue().toString();
                    String uri = e.getNamespace();
                    contentHandler.startPrefixMapping(prefix, uri);
                    prefixMapping.put(uri, prefix);
                    if (log.isDebugEnabled()) {
                        log.debug("PM(" + e.getNamespace() + ", " +
                                  e.getValue() + ")");
                    }
                    break;
                }
                case Event.START_ELEMENT: {
                    String namespace = e.getNamespace();
                    String localName = e.getName();
                    String prefix = (String) prefixMapping.get(namespace);
                    AttributesImpl atts = new AttributesImpl();
                    if (log.isDebugEnabled()) {
                        log.debug("START_TAG(" + namespace + ", " +
                                  localName
                                  + ")");
                    }
                    // ctl: Scan attributes; is there any other event that may
                    // occur inside attributes than COMMENT?
                    for (Event e2=null; (e2 = r.advance()) != null; ) {
                        if (e2.getType() == Event.ATTRIBUTE) {
                            atts.addAttribute(
                              makeNameSpace(prefixMapping,e2.getNamespace(),e2.getName()),
                              makeName(prefixMapping,e2.getNamespace(),e2.getName()),
                              makeQName(prefixMapping, e2.getNamespace(), e2.getName()),
                                              "CDATA",
                                              e2.getValue().toString());
                        } else if (e2.getType() == Event.COMMENT)
                            continue;
                        else
                            break;
                    }
                    r.backup(); // Since we read past last attribute
                    if (contentHandler != null) {
                      contentHandler.startElement(
                          makeNameSpace(prefixMapping, namespace, localName),
                          makeName(prefixMapping, namespace, localName),
                          makeQName(prefixMapping, namespace, localName),
                          atts);
                    }
                    break;
                }
                case Event.END_ELEMENT: {
                    String namespace = e.getNamespace();
                    String localName = e.getName();
                    if (log.isDebugEnabled()) {
                        log.debug("END_TAG(" + namespace + ", " + localName
                                  + ")");
                    }
                    if (contentHandler != null) {
                      contentHandler.endElement(
                          makeNameSpace(prefixMapping, namespace, localName),
                          makeName(prefixMapping, namespace, localName),
                          makeQName(prefixMapping, namespace, localName));
                    }
                    break;
                }
                case Event.CONTENT: {
                    char[] ch =
                        e.getValue().toString().toCharArray();
                    if (log.isDebugEnabled()) {
                        log.debug("TEXT(" + new String(ch, textParams[0],
                            textParams[1]) + ")");
                    }
                    if (contentHandler != null) {
                        contentHandler.characters(ch, 0, ch.length);
                    }
                    break;
                }
                default:
                    break;
            }
        }
        if (contentHandler != null) {
            contentHandler.endDocument();
        }
    }

    protected String makeNameSpace(Map prefixes, String nameSpace,
                                   String localName) {
      return nameSpace;
    }

    protected String makeName(Map prefixes, String nameSpace,
                                   String localName) {
      return localName;
    }

    protected String makeQName(Map prefixes, String nameSpace, String localName) {
        String prefix = (String) prefixes.get(nameSpace);
        if (prefix != null && prefix.length() > 0) {
            return prefix + ":" + localName;
        } else
            return localName;
    }
}
// arch-tag: 99076dd49e13f98eef6d42577c5d70f3 *-
