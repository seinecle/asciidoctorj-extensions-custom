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
        
//        System.out.println("doc base dir in pdf pre processor: "+docBasedir.toString());

        //writing this modified document to a temp folder, to be used by the revealjs maven build (see POM)
        final Path path = Paths.get(docBasedir.toString() + "/subdir");
        path.toFile().mkdirs();

        StringBuilder sb = new StringBuilder();

        List<String> lines = reader.readLines();

        for (String line : lines) {
            //remove lines with raw html, because they would get written "as is" on th pdf.
            if (line.startsWith("pass:")){
                continue;
            }
            if (line.trim().startsWith("image:") && line.toLowerCase().contains(".gif")){
                line = "";
            }
            if (line.startsWith("//PDF:")){
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
