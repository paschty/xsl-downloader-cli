package de.fsu.xsldownloader;

import java.io.InputStream;


public class CommandLineOptions extends XSLDownloaderOptions {

    public CommandLineOptions(String args[]) throws IllegalArgumentException {
        processArguments(args);

        if (getXslLocation() == null) {
            printCommandLineOptions();
            throw new IllegalArgumentException("No stylesheet specified!");
        }

        if(getDestination() == null){
            printCommandLineOptions();
            throw new IllegalArgumentException("No download destination specified!");
        }
    }

    public static final void printCommandLineOptions() {
        final InputStream optionTXTStream = CommandLineEntrypoint.class.getClassLoader().getResourceAsStream("options.txt");
        final String options = Utils.convertStreamToString(optionTXTStream);
        System.out.println(options);
    }

    private final void processArguments(String[] args) {
        for (int currentArgNr = 0; currentArgNr < args.length; currentArgNr++) {
            final String currentArg = args[currentArgNr];
            if (currentArgNr == 0) {
                setXslLocation(currentArg);
            } else if (currentArgNr == 1) {
                setDestination(currentArg);
            } else {
                final String[] keyValueArray = currentArg.split("=", 2);
                if (keyValueArray.length > 0 && keyValueArray[0].startsWith("-")) {
                    String key = keyValueArray[0].substring(1);
                    if (keyValueArray.length == 2) {
                        String value = keyValueArray[1];
                        processArgument(key, value);
                    } else {
                        processArgument(key);
                    }
                    continue;
                }
                printCommandLineOptions();
                System.out.println("Invalid argument: " + currentArg);
                throw new IllegalArgumentException(currentArg);
            }
        }
    }

    private final void processArgument(String key) {
        switch (key) {
            case "verbose":
                this.setVerbose(true);
                break;
            case "skipIncludes":
                this.setSkipIncludes(true);
                break;
            default:
                printCommandLineOptions();
                System.out.printf("Invalid argument: -%s%n", key);
                throw new IllegalArgumentException(key);
        }
    }

    private final void processArgument(String key, String value) {
        switch (key) {
            case "catalog":
                if (this.getCatalog() == null) {
                    this.setCatalog(value);
                } else {
                    final String msg = "Multiple catalogs parameters.";
                    System.out.println(msg);
                    throw new IllegalArgumentException(msg);
                }

                break;
            case "schemaFolder":
                if(this.getSchemaFolder() == null){
                   this.setSchemaFolder(value);
                } else {
                    final String msg = "Multiple schemaFolderParameters.";
                    System.out.println(msg);
                    throw new IllegalArgumentException(msg);
                }
                break;
            case "schemaPrefix":
                if(this.getSchemaPrefix() == null){
                    this.setSchemaPrefix(value);
                } else {
                    final String msg = "Multiple schemaPrefixParameters.";
                    System.out.println(msg);
                    throw new IllegalArgumentException(msg);
                }
                break;
            default:
                printCommandLineOptions();
                System.out.printf("Invalid argument: -%s=%s%n", key, value);
                throw new IllegalArgumentException(key + " " + value);
        }
    }

}
