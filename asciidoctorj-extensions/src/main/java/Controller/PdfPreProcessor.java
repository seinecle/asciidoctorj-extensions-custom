/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Preprocessor;
import org.asciidoctor.extension.PreprocessorReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfPreProcessor extends Preprocessor {

    String param;

    public PdfPreProcessor(Map<String, Object> config) {
        super(config);
    }

    @Override
    public PreprocessorReader process(Document document, PreprocessorReader reader) {
        

        System.out.println("in the pdf preprocessor");
        System.out.println("docdir: ");
        System.out.println(document.getAttr("docdir"));



        StringBuilder sb = new StringBuilder();

        List<String> lines = reader.readLines();

        for (String line : lines) {

            sb.append(line);
            sb.append("\n");
        }
        reader.push_include(sb.toString(), "", "", 1, document.getAttributes());

        //writing this modified document to a temp folder, to be used by the revealjs maven build (see POM)
        final Path path = Paths.get(param + "\\docs\\src\\main\\asciidoc\\subdir");
        path.toFile().mkdirs();

        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(path.toFile(), document.doctitle()+"_temp_pdf.adoc")), "UTF-8"));
            bw.write(sb.toString());
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(CommonPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return reader;

    }

}
