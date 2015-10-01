package de.fsu.xsldownloader;

import java.io.IOException;

import org.jdom2.JDOMException;

public class CommandLineEntrypoint {

    public static void main(String[] args) {
        try {
            final CommandLineOptions commandLineOptions = new CommandLineOptions(args);
            new XSLDownloader(commandLineOptions).download();
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (JDOMException e) {
            System.out.println(e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }





}
