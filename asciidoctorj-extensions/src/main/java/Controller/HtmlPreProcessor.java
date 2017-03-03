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

public class HtmlPreProcessor extends Preprocessor {

    String param;
    Path docBasedir;
    String statcounterProject;
    String statcounterSecurity;
    String statcounter;

    public HtmlPreProcessor(Map<String, Object> config) {
        super(config);
    }

    @Override
    public PreprocessorReader process(Document document, PreprocessorReader reader) {

        statcounterProject = (String) document.getAttr("statcounter-project");
        statcounterSecurity = (String) document.getAttr("statcounter-security");
        statcounter = buildStatCounterString();
        

        docBasedir = Paths.get((String) document.getAttr("docdir"));

        //writing this modified document to a temp folder, to be used by the revealjs maven build (see POM)
        final Path path = Paths.get(docBasedir.toString() + "/subdir");
        path.toFile().mkdirs();

        StringBuilder sb = new StringBuilder();

        List<String> lines = reader.readLines();

        for (String line : lines) {

            sb.append(line);
            sb.append("\n");
        }

        sb.append("pass:[" + statcounter + "]");
        sb.append("\n");

        reader.push_include(sb.toString(), "", "", 1, document.getAttributes());

        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(path.toFile(), (String) document.getAttr("docname") + "_temp_html.md")), "UTF-8"));
            bw.write(sb.toString());
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(CommonPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }

        return reader;
    }

    private String buildStatCounterString() {
            statcounter = "    <!-- Start of StatCounter Code for Default Guide -->\n"
            + "    <script type=\"text/javascript\">\n"
            + "        var sc_project = " + statcounterProject+";\n"
            + "        var sc_invisible = 1;\n"
            + "        var sc_security = \""+statcounterSecurity+"\";\n"
            + "        var scJsHost = ((\"https:\" == document.location.protocol) ?\n"
            + "            \"https://secure.\" : \"http://www.\");\n"
            + "        document.write(\"<sc\" + \"ript type='text/javascript' src='\" +\n"
            + "            scJsHost +\n"
            + "            \"statcounter.com/counter/counter.js'></\" + \"script>\");\n"
            + "    </script>\n"
            + "    <noscript><div class=\"statcounter\"><a title=\"site stats\"\n"
            + "    href=\"http://statcounter.com/\" target=\"_blank\"><img\n"
            + "    class=\"statcounter\"\n"
            + "    src=\"//c.statcounter.com/"+statcounterProject+"/0/"+statcounterSecurity+"/1/\" alt=\"site\n"
            + "    stats\"></a></div></noscript>\n"
            + "    <!-- End of StatCounter Code for Default Guide -->";
            
            return statcounter;

    }

}
