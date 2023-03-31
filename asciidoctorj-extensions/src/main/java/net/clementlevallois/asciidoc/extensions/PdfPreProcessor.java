/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.asciidoc.extensions;

import Utils.ImageAttributeExtractor;
import Utils.ImageFrame;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Preprocessor;
import org.asciidoctor.extension.PreprocessorReader;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class PdfPreProcessor extends Preprocessor {

    Path docBasedir;
    Path directorySourceFile;
    Path directoryAllSources;
    Path basedir;

    public PdfPreProcessor(Map<String, Object> config) {
        super(config);
    }

    @Override
    public void process(Document document, PreprocessorReader reader) {

        String docToProcess = (String) document.getAttribute("doc-to-process");
        System.out.println("doc-to-process= " + docToProcess);

        directoryAllSources = Paths.get((String) document.getAttribute("source-directory"));
        System.out.println("dir all sources = " + directoryAllSources);

        directorySourceFile = Path.of(directoryAllSources.toString(), "subdir"); // because the pre-processor extension delivered an intermediary source file in this subdir folder.
        System.out.println("source directory = " + directorySourceFile);

        basedir = directoryAllSources.getParent().getParent().getParent();
        System.out.println("base directory = " + basedir);
        String docName = (String) document.getAttribute("docname") + ".adoc";
        System.out.println("doc name= " + docName);
        Path pathSourceImageFolderForThisDoc = Path.of(directorySourceFile.toString(), "images");
        System.out.println("in the pdf preprocessor");
        docBasedir = Paths.get((String) document.getAttribute("docdir"));
        System.out.println("doc base dir: " + docBasedir);
        List<String> lines = reader.readLines();
        List<String> newLines = new ArrayList();
        for (String line : lines) {
//            System.out.println("line: "+  line);
            //remove lines with raw html, because they would get written "as is" on the pdf.
            if (line.startsWith("pass:")) {
                continue;
            }

            if (line.trim().startsWith("image:") && line.toLowerCase().contains(".gif")) {
                try {
                    String imagePrefix = (line.trim().startsWith("image::")) ? "image::" : "image:";
                    String title = ImageAttributeExtractor.extractTitle(line);
                    String source = ImageAttributeExtractor.extractSource(line);
                    String extension = ImageAttributeExtractor.extractExtension(line);

                    if (source.startsWith("http")) {

                        URL url = new URL(source);

                        File output = new File(pathSourceImageFolderForThisDoc.toFile(), title + ".png");

                        ImageIO.write(ImageIO.read(url), "png", output);
                        line = imagePrefix + title + ".png" + extension;
                    } else {
                        File input = new File(pathSourceImageFolderForThisDoc.toFile(), source);
                        File output = new File(pathSourceImageFolderForThisDoc.toFile(), title + ".png");

                        InputStream in = new FileInputStream(input);
                        ImageFrame[] readGif = ImageAttributeExtractor.readGif(in);

                        int frameNumber = Math.min(20, readGif.length);

                        if (frameNumber < 0 | frameNumber > readGif.length | readGif.length == 0) {
                            //do nothing the gif is corrupted it has no frame

                        } else {
                            ImageIO.write(readGif[frameNumber - 1].getImage(), "png", output);
                            line = imagePrefix + title + ".png" + extension;
                        }
                    }

                } catch (MalformedURLException ex) {
                    Logger.getLogger(PdfPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(PdfPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if (line.startsWith("//PDF:")) {
                line = line.replace("//PDF:", "").trim();
            }

            newLines.add(line);
        }
        reader.restoreLines(newLines);

    }

}
