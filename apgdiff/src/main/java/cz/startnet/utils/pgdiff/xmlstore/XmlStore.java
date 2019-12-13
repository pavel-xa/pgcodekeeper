package cz.startnet.utils.pgdiff.xmlstore;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ru.taximaxim.codekeeper.apgdiff.localizations.Messages;

public abstract class XmlStore<T> {

    protected final String fileName;
    protected final String rootTag;

    protected XmlStore(String fileName, String rootTag) {
        this.fileName = fileName;
        this.rootTag = rootTag;
    }

    protected Element createSubElement(Document xml, Element parent, String name, String value) {
        Element newElement = xml.createElement(name);
        newElement.setTextContent(value);
        parent.appendChild(newElement);
        return newElement;
    }

    protected abstract Path getXmlFile();

    public List<T> readObjects() throws IOException {
        try (Reader xmlReader = Files.newBufferedReader(getXmlFile(), StandardCharsets.UTF_8)) {
            return getObjects(readXml(xmlReader));
        } catch (NoSuchFileException ex) {
            return new ArrayList<>();
        } catch (IOException | SAXException ex) {
            throw new IOException(MessageFormat.format(
                    Messages.XmlStore_read_error, ex.getLocalizedMessage()), ex);
        }
    }

    protected List<T> getObjects(Document xml) {
        List<T> objects = new ArrayList<>();
        Element root = (Element) xml.getElementsByTagName(rootTag).item(0);
        NodeList nList = root.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node node = nList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                objects.add(parseElement(node));
            }
        }
        return objects;
    }

    protected abstract T parseElement(Node node);

    public void writeObjects(List<T> list) throws IOException {
        try {
            Path path = getXmlFile();
            Files.createDirectories(path.getParent());
            try (Writer xmlWriter = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                Element root = xml.createElement(rootTag);
                xml.appendChild(root);
                appendChildren(xml, root, list);
                serializeXml(xml, true, xmlWriter);
            }


        } catch (IOException | ParserConfigurationException | TransformerException ex) {
            throw new IOException(MessageFormat.format(
                    Messages.XmlStore_write_error, ex.getLocalizedMessage()), ex);
        }
    }

    protected abstract void appendChildren(Document xml, Element root, List<T> list);

    /**
     * Reads (well-formed) list XML and checks it for basic validity:
     * root node must be <code>&lt;rootTagName&gt;</code>
     */
    private Document readXml(Reader reader) throws IOException, SAXException {
        try {
            Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(reader));
            xml.normalize();

            if (!xml.getDocumentElement().getNodeName().equals(rootTag)) {
                throw new IOException(Messages.XmlStore_root_error);
            }

            return xml;
        } catch (ParserConfigurationException ex) {
            throw new IOException(ex);
        }
    }

    private void serializeXml(Document xml, boolean formatting,
            Writer writer) throws TransformerException {
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        if (formatting) {
            tf.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
            tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        tf.transform(new DOMSource(xml), new StreamResult(writer));
    }
}
