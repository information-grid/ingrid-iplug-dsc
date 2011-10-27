/**
 * 
 */
package de.ingrid.iplug.dsc.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import de.ingrid.utils.xpath.XPathUtils;

/**
 * Helper class encapsulating functionality for DOM processing. Must be
 * instantiated to be thread safe.
 * 
 * @author Martin
 */
public class DOMUtils {

    private static final Logger log = Logger.getLogger(DOMUtils.class);

    private Document myDoc = null;
    private XPathUtils xPathUtils = null;

    /** Maps namespace prefix to URI */
    private Map<String, String> myNSMap = new HashMap<String, String>();

    public DOMUtils(Document doc, XPathUtils xPathUtils) {
        this.myDoc = doc;
        this.xPathUtils = xPathUtils;
    }

    /** Add a namespace before creating an element of that NS ! */
    public void addNS(String prefix, String uri) {
        myNSMap.put(prefix, uri);
    }

    /**
     * Get the URI of a Namespace prefix.
     * 
     * @param prefix
     * @return null if not added yet
     */
    public String getNS(String prefix) {
        return myNSMap.get(prefix);
    }

    public class IdfElement {

        private Element e;

        public IdfElement(Element element) {
            this.e = element;
        }

        public IdfElement addElement(String qualifiedName) {
            String[] qNames = qualifiedName.split("/");
            Element parent = e;
            Element newElement = null;
            for (String qName : qNames) {
                newElement = domCreateElement(qName);
                parent.appendChild(newElement);
                parent = newElement;
            }
            return new IdfElement(newElement);
        }

        public IdfElement addElementAsFirst(String qualifiedName) {
            String[] qNames = qualifiedName.split("/");
            Element parent = e;
            Element newElement = null;
            for (String qName : qNames) {
                newElement = domCreateElement(qName);
                if (parent.hasChildNodes()) {
                    parent.insertBefore(newElement, parent.getFirstChild());
                } else {
                    parent.appendChild(newElement);
                }
                parent = newElement;
            }
            return new IdfElement(newElement);
        }

        /**
         * Add a new node (which can contain more sub nodes) as a sibling to the
         * dom node from where it has been called (member variable 'e'). From
         * the parent of 'e' the new node will be inserted before the sibling of
         * 'e'. If this sibling does not exist (because it was the last one),
         * the new node will added to the end.
         * 
         * @param qualifiedName
         *            , contains the path of nodes to create
         * @return
         */
        public IdfElement addElementAsSibling(String qualifiedName) {
            String[] qNames = qualifiedName.split("/");

            Node sibling = e.getNextSibling();
            Element parent = domCreateElement(qNames[0]);
            if (sibling != null) {
                e.getParentNode().insertBefore(parent, sibling);
            } else {
                e.getParentNode().appendChild(parent);
            }

            Element newElement = parent;
            for (int i = 1; i < qNames.length; i++) {
                String qName = qNames[i];
                newElement = domCreateElement(qName);
                NodeList siblings = xPathUtils.getNodeList(e, qName);
                if (siblings != null && siblings.getLength() > 0) {
                    Node refNode = siblings.item(siblings.getLength() - 1).getNextSibling();
                    if (refNode != null) {
                        parent.insertBefore(refNode, newElement);
                    } else {
                        parent.appendChild(newElement);
                    }
                } else {
                    parent.appendChild(newElement);
                }
                parent = newElement;
            }
            return new IdfElement(newElement);
        }

        public IdfElement addAttribute(String attrName, String attrValue) {
            DOMUtils.this.addAttribute(e, attrName, attrValue);
            return this;
        }

        public IdfElement addText(String text) {
            if (e.hasChildNodes()) {
                for (int i = 0; i < e.getChildNodes().getLength(); i++) {
                    if (e.getChildNodes().item(i) instanceof Text) {
                        ((Text) e.getChildNodes().item(i)).setNodeValue(escapeXmlText(text));
                        return this;
                    }
                }
            }
            DOMUtils.this.addText(e, text);
            return this;
        }

        public IdfElement addElement(IdfElement element) {
            e.appendChild(element.getElement());
            return element;
        }

        public Element getElement() {
            return e;
        }
    }

    public IdfElement convertToIdfElement(Element element) {
        return new IdfElement(element);
    }

    public IdfElement addElement(Element element, String qualifiedName) {
        return new IdfElement(element).addElement(qualifiedName);
    }

    public IdfElement addAttribute(Element element, String attrName, String attrValue) {
        element.setAttribute(attrName, escapeXmlText(attrValue));
        return new IdfElement(element);
    }

    public IdfElement addText(Element element, String text) {
        element.appendChild(domNewTextNode(text));
        return new IdfElement(element);
    }

    public IdfElement createElement(String qualifiedName) {
        return new IdfElement(domCreateElement(qualifiedName));
    }

    public IdfElement addElementFromXPath(IdfElement element, String xpath) {
        Element refelement = null;
        if (xpath.startsWith("/")) {
            refelement = element.getElement().getOwnerDocument().getDocumentElement();
        } else {
            refelement = element.getElement();
        }
        // refNode = node;
        String[] xpathElements = xpath.split("/");
        String tmpXpath = ".";
        Element result = refelement;
        for (int i = 0; i < xpathElements.length; i++) {
            if (xpathElements[i].length() > 0) {
                if (!xPathUtils.nodeExists(refelement, tmpXpath + "/" + xpathElements[i])) {
                    if (tmpXpath.length() == 0) {
                        throw new IllegalArgumentException(
                                "More than one root element is not allowed! The supplied absolute path MUST start with the existing root node!");
                    } else {
                        NodeList list = xPathUtils.getNodeList(refelement, tmpXpath);
                        result = (Element) list.item(0).appendChild(domCreateElement(xpathElements[i]));
                    }
                } else if (tmpXpath.length() > 0) {
                    result = (Element) xPathUtils.getNodeList(refelement, tmpXpath + "/" + xpathElements[i]).item(0);
                }
                tmpXpath = tmpXpath + "/" + xpathElements[i];
            } else {
                tmpXpath = "";
            }
        }
        return new IdfElement(result);
    }

    public IdfElement getElement(Object node, String xPath) {
        Element element;
        if (node instanceof Document) {
            element = ((Document) node).getDocumentElement();
        } else if (node instanceof Node) {
            element = (Element) node;
        } else if (node instanceof Element) {
            element = (Element) node;
        } else if (node instanceof IdfElement) {
            element = ((IdfElement) node).getElement();
        } else {
            throw new RuntimeException("Unsupported input argument type: " + node.getClass().getName());
        }
        Element e = (Element) xPathUtils.getNode(element, xPath);
        if (e != null) {
            return new IdfElement(e);
        } else {
            return null;
        }
    }

    private Element domCreateElement(String qualifiedName) {
        Element retValue = null;

        String[] prefixWithName = qualifiedName.split(":", 2);
        if (prefixWithName.length > 1) {
            // we can use the full qualified name when creating with NS !
            retValue = domNewElementNS(prefixWithName[0], qualifiedName);
        } else {
            retValue = domNewElement(qualifiedName);
        }

        return retValue;
    }

    /**
     * Create an Element WITHOUT NameSpace (NS) qualification !
     * 
     * @param nameWithoutNS
     *            if you pass name with NS prefix (*:*), the prefix isn't
     *            handles as namespace !
     * @return null if problems
     */
    private Element domNewElement(String nameWithoutNS) {
        Element retValue = null;
        try {
            retValue = myDoc.createElement(nameWithoutNS);
        } catch (Exception ex) {
            log.error(
                    "Problems creating DOM Element '" + nameWithoutNS + "' -> DOM Doc for creation = '" + myDoc + "'",
                    ex);
        }

        return retValue;
    }

    /**
     * Create an Element WITH NameSpace (NS) qualification ! Namespace URI must
     * be added with its prefix to internal map before calling this method !!!
     * 
     * @param nsKey
     *            key of NS, must be present in internal map for mapping to URI
     *            !
     * @param qualifiedName
     *            can be the full qualified name including namespace prefix !
     * @return null if problems
     */
    private Element domNewElementNS(String nsKey, String qualifiedName) {
        Element retValue = null;

        String nsUri = myNSMap.get(nsKey);
        try {
            retValue = myDoc.createElementNS(nsUri, qualifiedName);
        } catch (Exception ex) {
            log.error("Problems creating DOM Element '" + qualifiedName + "' -> nsKey = '" + nsKey
                    + "', found nsUri = '" + nsUri + "', DOM Doc for creation = '" + myDoc + "'", ex);
        }

        return retValue;
    }

    private Text domNewTextNode(String data) {
        return myDoc.createTextNode(escapeXmlText(data));
    }

    /** Escapes given text. NOTICE: returns empty string if passed text is null. */
    private String escapeXmlText(String text) {
        if (text == null) {
            text = "";
        }
        return StringEscapeUtils.escapeXml(text);
    }

}
