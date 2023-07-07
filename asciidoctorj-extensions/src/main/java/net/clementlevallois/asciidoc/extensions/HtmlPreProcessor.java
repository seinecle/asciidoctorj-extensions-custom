/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.asciidoc.extensions;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Preprocessor;
import org.asciidoctor.extension.PreprocessorReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HtmlPreProcessor extends Preprocessor {

    String statcounterProject;
    String statcounterSecurity;
    String statcounter;

    public HtmlPreProcessor(Map<String, Object> config) {
        super(config);
    }

    @Override
    public void process(Document document, PreprocessorReader reader) {

        System.out.println("in the html preprocessor");
        statcounterProject = (String) document.getAttribute("statcounter-project");
        statcounterSecurity = (String) document.getAttribute("statcounter-security");
        statcounter = buildStatCounterString();
        List<String> lines = reader.readLines();
        List<String> newLines = new ArrayList();
        for (String line : lines) {
            if (line.startsWith("//HTML:")) {
                line = line.replace("//HTML:", "").trim();
            }
            newLines.add(line);
        }
        newLines.add("\n");
        newLines.add("pass:[" + statcounter + "]");
        reader.restoreLines(newLines);
//        try {
//            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(path.toFile(), (String) document.getAttribute("docname") + "_temp_html.md")), "UTF-8"));
//            bw.write(sb.toString());
//            bw.close();
//        } catch (IOException ex) {
//            Logger.getLogger(CommonPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    private String buildStatCounterString() {
        statcounter = "\n    <!-- Start of StatCounter Code for Default Guide -->\n"
                + "    <script type=\"text/javascript\">\n"
                + "        var sc_project = " + statcounterProject + ";\n"
                + "        var sc_invisible = 1;\n"
                + "        var sc_security = \"" + statcounterSecurity + "\";\n"
                + "        var scJsHost = ((\"https:\" == document.location.protocol) ?\n"
                + "            \"https://secure.\" : \"http://www.\");\n"
                + "        document.write(\"<sc\" + \"ript type='text/javascript' src='\" +\n"
                + "            scJsHost +\n"
                + "            \"statcounter.com/counter/counter.js'></\" + \"script>\");\n"
                + "    </script>\n"
                + "    <noscript><div class=\"statcounter\"><a title=\"site stats\"\n"
                + "    href=\"http://statcounter.com/\" target=\"_blank\"><img\n"
                + "    class=\"statcounter\"\n"
                + "    src=\"//c.statcounter.com/" + statcounterProject + "/0/" + statcounterSecurity + "/1/\" alt=\"site\n"
                + "    stats\"></a></div></noscript>\n"
                + "    <!-- End of StatCounter Code for Default Guide -->";

        return statcounter;
    }
}
