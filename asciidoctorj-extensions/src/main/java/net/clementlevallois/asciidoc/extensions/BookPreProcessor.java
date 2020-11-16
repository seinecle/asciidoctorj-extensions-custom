/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.asciidoc.extensions;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Preprocessor;
import org.asciidoctor.extension.PreprocessorReader;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BookPreProcessor extends Preprocessor {

    String param;
    Path outputDir;
    Path folderBookChaptersPath;

    public BookPreProcessor(Map<String, Object> config) {
        super(config);
    }

    @Override
    public void process(Document document, PreprocessorReader reader) {
        try {
            System.out.println("in the book preprocessor");

            String docToProcess = (String) document.getAttribute("doc-to-process");
            String folderBooksChapters = (String) document.getAttribute("book-chapters-folder");
            String collatedAdocs = (String) document.getAttribute("collated-adocs");
            folderBookChaptersPath = Paths.get(folderBooksChapters);
            String docName = (String) document.getAttribute("docname") + ".adoc";
            outputDir = Paths.get((String) document.getAttribute("output-dir"));

            System.out.println("doc-to-process= " + docToProcess);
            System.out.println("doc name= " + docName);

            StringBuilder sb = new StringBuilder();

            List<String> lines = reader.readLines();

            boolean skipEndSection = false;
            boolean skipHeaderSection = true;
            boolean skipSlidesLines = false;
            boolean keepTitle = false;

            for (String line : lines) {

                skipSlidesLines = (line.startsWith("//ST")) ? true : false;
                keepTitle = (line.startsWith("= ")) ? true : false;

                // deleting the main part of the title
                if (keepTitle) {
                    if (line.contains(":")){
                        line = "= " + line.substring(line.indexOf(":") + 1, line.length()).trim();
                    }
                    line = line  + "\n";
                }

                if (line.toLowerCase().startsWith("== the end")) {
                    skipEndSection = true;
                }

                if (line.startsWith("==")) {
                    skipHeaderSection = false;
                }

                if ((!skipHeaderSection & !skipEndSection & !skipSlidesLines) | keepTitle) {
                    sb.append(line);
                    sb.append("\n");
                }
            }
            // this introduces a page break
            sb.append("<<<\n");

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir.toFile(), collatedAdocs), true), "UTF-8"));
            bw.write(sb.toString());
            bw.close();

        } catch (IOException ex) {
            Logger.getLogger(BookPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
