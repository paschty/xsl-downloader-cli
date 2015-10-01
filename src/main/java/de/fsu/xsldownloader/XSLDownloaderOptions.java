package de.fsu.xsldownloader;

public class XSLDownloaderOptions {

    private boolean verbose = false;
    private boolean skipIncludes = false;

    private String catalog = null;
    private String xslLocation = null;
    private String destination = null;
    private String schemaFolder = null;
    private String schemaPrefix = null;

    public String getCatalog() {
        return catalog;
    }

    public String getXslLocation() {
        return xslLocation;
    }

    public boolean isVerbose() {
        return verbose;
    }

    protected void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    protected void setXslLocation(String xslLocation) {
        this.xslLocation = xslLocation;
    }

    protected void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public String getDestination() {
        return destination;
    }

    protected void setDestination(String destination) {
        this.destination = destination;
    }

    public boolean isSkipIncludes() {
        return skipIncludes;
    }

    protected void setSkipIncludes(boolean skipIncludes) {
        this.skipIncludes = skipIncludes;
    }

    public String getSchemaFolder() {
        return schemaFolder;
    }

    protected void setSchemaFolder(String schemaFolder) {
        this.schemaFolder = schemaFolder;
    }

    public String getSchemaPrefix() {
        return schemaPrefix;
    }

    protected void setSchemaPrefix(String schemaPrefix) {
        this.schemaPrefix = schemaPrefix;
    }
}
