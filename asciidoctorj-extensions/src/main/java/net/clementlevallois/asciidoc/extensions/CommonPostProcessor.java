/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.asciidoc.extensions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Preprocessor;
import org.asciidoctor.extension.PreprocessorReader;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommonPostProcessor extends Preprocessor {

    String docName;
    Path asciidocSourcesRootFolder;
    Path subdirImageFolderForThisDocument;

    public CommonPostProcessor(Map<String, Object> config) {
        super(config);
    }

    @Override
    public void process(Document document, PreprocessorReader reader) {
        try {
            System.out.println("copying resources to destination folder, and images in particular");

            String targetDocsRootDirectory = ((String) document.getAttribute("target-root-directory"));
            docName = (String) document.getAttribute("docname") + ".adoc";
            System.out.println("doc name= " + docName);

            String refreshPicsString = ((String) document.getAttribute("refresh-pics"));
            boolean refreshPics = !(refreshPicsString == null || !refreshPicsString.toLowerCase().equals("yes"));

            asciidocSourcesRootFolder = Paths.get((String) document.getAttribute("source-directory"));

            subdirImageFolderForThisDocument = Path.of(asciidocSourcesRootFolder.toString(), "subdir", docName, "images");

            Path targetSlidesImagesForThisDoc = Path.of(targetDocsRootDirectory, "generated-slides", docName, "images");
            Path targetHtmlImagesForThisDoc = Path.of(targetDocsRootDirectory, "generated-html", docName, "images");
            Files.createDirectories(targetSlidesImagesForThisDoc);
            Files.createDirectories(targetHtmlImagesForThisDoc);

            Files.walk(subdirImageFolderForThisDocument).filter(Files::isRegularFile).forEach(path -> {
                try {
                    Path targetPathSlides = targetSlidesImagesForThisDoc.resolve(subdirImageFolderForThisDocument.relativize(path));
                    Path targetPathHtml = targetHtmlImagesForThisDoc.resolve(subdirImageFolderForThisDocument.relativize(path));
                    if (Files.exists(targetPathSlides)) {
                        if (refreshPics) {
                            Files.copy(path, targetPathSlides, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } else {
                        Files.copy(path, targetPathSlides);
                    }
                    if (Files.exists(targetPathHtml)) {
                        if (refreshPics) {
                            Files.copy(path, targetPathHtml, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } else {
                        Files.copy(path, targetPathHtml);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(CommonPostProcessor.class.getName()).log(Level.SEVERE, null, ex);
                }
            });

        } catch (IOException ex) {
            Logger.getLogger(CommonPostProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
