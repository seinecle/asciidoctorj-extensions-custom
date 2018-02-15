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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

public class PdfPreProcessor extends Preprocessor {

    String param;
    Path docBasedir;

    public PdfPreProcessor(Map<String, Object> config) {
        super(config);
    }

    @Override
    public PreprocessorReader process(Document document, PreprocessorReader reader) {

        System.out.println("in the pdf preprocessor");

        docBasedir = Paths.get((String) document.getAttr("docdir"));

        String docToProcess = (String) document.getAttr("doc-to-process");

        String docName = (String) document.getAttr("docname");

        System.out.println("doc-to-process= " + docToProcess);
        System.out.println("doc name= " + docName);

//        System.out.println("doc base dir in pdf pre processor: "+docBasedir.toString());
        //writing this modified document to a temp folder, to be used by the revealjs maven build (see POM)
        final Path path = Paths.get(docBasedir.toString() + "/subdir");
        path.toFile().mkdirs();

        StringBuilder sb = new StringBuilder();

        List<String> lines = reader.readLines();

        for (String line : lines) {
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

                        File output = new File(docBasedir.toString() + "/images/", title + ".png");

                        ImageIO.write(ImageIO.read(url), "png", output);
                        line = imagePrefix + title + ".png" + extension;
                    } else {
                        File input = new File(docBasedir.toString() + "/images/", source);
                        File output = new File(docBasedir.toString() + "/images/", title + ".png");

                        InputStream in = new FileInputStream(input);
                        ImageFrame[] readGif = ImageAttributeExtractor.readGif(in);

                        int frameNumber = Math.min(20, readGif.length);

                        if (frameNumber < 0 | frameNumber > readGif.length | readGif.length == 0) {
                            //do nothing the gif is corrupted it has no frame

                        } else {

                            ImageIO.write(readGif[frameNumber -1].getImage(), "png", output);
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

            sb.append(line);
            sb.append("\n");
        }
        reader.push_include(sb.toString(), "", "", 1, document.getAttributes());

        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(path.toFile(), (String) document.getAttr("docname") + "_temp_pdf.md")), "UTF-8"));
            bw.write(sb.toString());
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(CommonPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }

        return reader;

    }

}
