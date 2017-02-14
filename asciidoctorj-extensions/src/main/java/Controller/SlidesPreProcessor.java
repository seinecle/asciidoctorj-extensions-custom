/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller;

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

public class SlidesPreProcessor extends Preprocessor {

    String param;
    Path docBasedir;

    public SlidesPreProcessor(Map<String, Object> config) {
        super(config);
    }

    @Override
    public PreprocessorReader process(Document document, PreprocessorReader reader) {

        System.out.println("in the slides preprocessor");
        StringBuilder sb = new StringBuilder();
        docBasedir = Paths.get((String) document.getAttr("docdir"));

        //writing this modified document to a temp folder, to be used by the revealjs maven build (see POM)
        final Path path = Paths.get(docBasedir.toString() + "/subdir");
        path.toFile().mkdirs();

        List<String> lines = reader.readLines();

        // managing titles and slides beginnings
        for (String line : lines) {
            if (line.startsWith("== ")) {
                continue;
            }
            if (line.startsWith("//ST:")) {
                line = line.replace("//ST:", "== ").trim();
                if (line.equals("==")){
                    line = line + " !";
                }
            }
            
            //adding a "stretch" class to images. See: https://github.com/asciidoctor/asciidoctor-reveal.js/#stretch-class-attribute
            if (line.startsWith("image::")) {
                sb.append("[.stretch]");
                sb.append("\n");
            }
            

            sb.append(line);
            sb.append("\n");
        }
        reader.push_include(sb.toString(), "", "", 1, document.getAttributes());

        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(path.toFile(), (String) document.getAttr("docname") + "_temp_slides.md")), "UTF-8"));
            bw.write(sb.toString());
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(CommonPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return reader;

    }

}
