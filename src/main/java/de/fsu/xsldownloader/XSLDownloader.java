package de.fsu.xsldownloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.StringTokenizer;
import java.util.concurrent.LinkedBlockingQueue;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

public class XSLDownloader {

    private static final Namespace XSL_NAMESPACE = Namespace.getNamespace("http://www.w3.org/1999/XSL/Transform");
    private static final Namespace XSI_NAMESPACE = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    private static final String URN_OASIS_NAMES_TC_ENTITY_XMLNS_XML_CATALOG = "urn:oasis:names:tc:entity:xmlns:xml:catalog";

    private XSLDownloaderOptions options;
    private Map<URL, String> urlLocalFileMap;
    private Queue<Map.Entry<URL, String>> filesToDownload;
    private HashSet<URL> xsdToDownload;

    private List<String> filterNamespaces = Arrays.asList(new String[]{"http://www.w3.org/1999/XSL/Transform",
            "http://www.w3.org/XML/1998/namespace", "http://www.w3.org/2001/XMLSchema-instance"});

    XSLDownloader(XSLDownloaderOptions options) throws MalformedURLException {
        this.options = options;
        this.urlLocalFileMap = new Hashtable<>();
        this.filesToDownload = new LinkedBlockingQueue<>();
        this.xsdToDownload = new HashSet<>();
        final String xslLocation = options.getXslLocation();
        final String destination = options.getDestination();
        final URL url = getUrl(xslLocation);
        filesToDownload.add(new AbstractMap.SimpleImmutableEntry<URL, String>(url, destination));
    }

    private void downloadToFile(URL url, Path destinationFile) {
        printIfVerbose("Download " + url.toString() + " to " + destinationFile.toAbsolutePath().toString());
        try (InputStream is = url.openStream()) {
            Files.copy(is, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return;
        }

    }

    private static URL getUrl(String xslLocation) throws MalformedURLException {
        return new URL(xslLocation);
    }

    public void download() throws IOException, JDOMException {
        Map.Entry<URL, String> nextFile;
        while ((nextFile = filesToDownload.poll()) != null) {
            downloadXSL(nextFile.getKey(), nextFile.getValue());
        }

        final Iterator<URL> xsdIterator = this.xsdToDownload.iterator();
        while(xsdIterator.hasNext()){
            URL xsd = xsdIterator.next();
            final String schemaFolder = getSchemaFolder();
            Path destinationPath = FileSystems.getDefault().getPath(schemaFolder + getFileName(xsd));
            downloadToFile(xsd,destinationPath);
        }
    }

    private String getSchemaFolder() {
        return options.getSchemaFolder().endsWith("/") ? options.getSchemaFolder(): options.getSchemaFolder()+"/";
    }

    private void downloadXSL(URL xslUrl, String destination) throws IOException, JDOMException {
        printIfVerbose("Download " + xslUrl.toString() + "...");
        try {
            Path destinationPath = FileSystems.getDefault().getPath(destination);

            if (Files.isDirectory(destinationPath)) {
                printIfVerbose("Destination is a Folder!", "Try to get filename from url..");
                final String file = getFileName(xslUrl);
                printIfVerbose("The file name is: " + file);
                destinationPath = destinationPath.resolve(file);
            }

            printIfVerbose("The file will be saved to " + destinationPath.toString());
            downloadToFile(xslUrl, destinationPath);

            if (!options.isSkipIncludes() || options.getCatalog() != null) {
                printIfVerbose("Open file and check for includes and Schema!");
                final Document xslDocument;
                try (final InputStream inputStream = Files.newInputStream(destinationPath)) {
                    final SAXBuilder saxBuilder = new SAXBuilder();
                    xslDocument = saxBuilder.build(inputStream);
                }

                if (options.getCatalog() != null) {
                    remapXSD(xslDocument);
                }

                if (!options.isSkipIncludes()) {
                    remapIncludes(destinationPath, xslDocument);
                }


            } else {
                System.out.println("Don't Open file because skipIncludes and no catlog!");
            }
        } catch (MalformedURLException e) {
            System.out.println(e.getMessage());
            return;
        }
    }

    private void remapXSD(Document xslDocument) throws IOException, JDOMException {
        String catalogPathParameter = options.getCatalog();
        final Path catalogPath = FileSystems.getDefault().getPath(catalogPathParameter);
        Document catalogDocument;

        if (Files.exists(catalogPath)) {
            catalogDocument = readCatalog(catalogPath);
        } else {
            final Element catalogElement = new Element("catalog", URN_OASIS_NAMES_TC_ENTITY_XMLNS_XML_CATALOG);
            catalogDocument = new Document(catalogElement);
        }

        printIfVerbose("Search schemas...");
        final XPathFactory instance = XPathFactory.instance();
        final XPathExpression<Element> pathExpression = instance.compile("//*[@xsi:schemaLocation]", Filters.element(), null, XSI_NAMESPACE);
        final List<Element> evaluate = pathExpression.evaluate(xslDocument.getRootElement());

        evaluate.forEach(s -> {
            final String value = s.getAttribute("schemaLocation", XSI_NAMESPACE).getValue();
            StringTokenizer schemaLocationTokenizer = new StringTokenizer(value, " ");

            while (schemaLocationTokenizer.hasMoreTokens()) {
                String ns = schemaLocationTokenizer.nextToken();
                String loc = schemaLocationTokenizer.nextToken();

                printIfVerbose("Found namespace " + ns + " with xsd-location: " + loc);
                final XPathExpression<Boolean> catalogHasLocationExpression = instance.compile("count(./e:system[@systemId='" + loc + "'])>0", Filters.fboolean(), null, Namespace.getNamespace("e", URN_OASIS_NAMES_TC_ENTITY_XMLNS_XML_CATALOG));
                final Boolean catalogHasLocation = catalogHasLocationExpression.evaluateFirst(catalogDocument.getRootElement());
                if (catalogHasLocation) {
                    printIfVerbose("The catalog.xml already has the system entry for location: " + loc);
                } else {
                    printIfVerbose("Adding location to catalog.xml");
                    final Element newSystemEntry = new Element("system", URN_OASIS_NAMES_TC_ENTITY_XMLNS_XML_CATALOG);
                    newSystemEntry.setAttribute("systemId", loc);
                    try {
                        final URL url = new URL(loc);
                        String schemaPrefix = options.getSchemaPrefix();
                        if(schemaPrefix == null){
                            schemaPrefix = catalogPath.getParent().relativize(FileSystems.getDefault().getPath(getSchemaFolder())).toString()+"/";
                        }

                        newSystemEntry.setAttribute("uri", schemaPrefix + getFileName(url));

                        printIfVerbose("Queue XSD for download..");
                        this.xsdToDownload.add(url);
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                    catalogDocument.getRootElement().addContent(newSystemEntry);
                }
            }
        });

        printIfVerbose("Updating catalog..");
        final XMLOutputter catalogOutputter = new XMLOutputter();
        catalogOutputter.setFormat(Format.getPrettyFormat());
        try (OutputStream catalogOutputStream = Files.newOutputStream(catalogPath)) {
            catalogOutputter.output(catalogDocument, catalogOutputStream);
        }
    }

    private void remapIncludes(Path destinationPath, Document xslDocument) throws IOException {
        printIfVerbose("Search includes...");
        final List<Element> includeElements = xslDocument.getRootElement().getChildren("include", XSL_NAMESPACE);
        includeElements.stream().forEach(include -> {
            String href = include.getAttributeValue("href");
            printIfVerbose("Found include: " + href);
            try {
                final URL includeUrl = getUrl(href);
                final String localFileName = getFileName(includeUrl);
                printIfVerbose("Map remote file " + includeUrl.toString() + " to " + localFileName);
                if (!urlLocalFileMap.containsKey(includeUrl)) {
                    final String dest = destinationPath.getParent().toString() + "/" + localFileName;
                    urlLocalFileMap.put(includeUrl, dest);
                    filesToDownload.add(new AbstractMap.SimpleEntry<>(includeUrl, dest));
                }

                include.setAttribute("href", localFileName);
            } catch (MalformedURLException e) {
                System.out.println(e.getMessage());
                throw new RuntimeException("XSL File contains invalid URL! " + href);
            }
        });

        printIfVerbose("Write new locations to XSL File...");
        try (final OutputStream outputStream = Files.newOutputStream(destinationPath)) {
            XMLOutputter xmlOutputter = new XMLOutputter();
            xmlOutputter.setFormat(Format.getPrettyFormat());
            xmlOutputter.output(xslDocument, outputStream);
        }
    }

    private Document readCatalog(Path catalogPath) throws IOException, JDOMException {
        try (InputStream catalogIS = Files.newInputStream(catalogPath)) {
            SAXBuilder catalogBuilder = new SAXBuilder();
            return catalogBuilder.build(catalogIS);
        }
    }

    private String getFileName(URL url) {
        final String[] urlParts = url.getFile().split("/");
        return urlParts[urlParts.length - 1];
    }

    private void printIfVerbose(String... x) {
        if (options.isVerbose()) {
            Arrays.asList(x).forEach(System.out::println);
        }
    }
}
