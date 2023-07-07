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

public class SlidesPreProcessor extends Preprocessor {

    String statcounterProject;
    String statcounterSecurity;
    String statcounter;

    String docName;
    
    String info = "// 'Escape' or 'o' to see all sides, F11 for full screen, 's' for speaker notes";

    public SlidesPreProcessor(Map<String, Object> config) {
        super(config);
    }

    @Override
    public void process(Document document, PreprocessorReader reader) {

        System.out.println("in the slides preprocessor");

        statcounterProject = (String) document.getAttribute("statcounter-project");
        statcounterSecurity = (String) document.getAttribute("statcounter-project");
        statcounter = buildStatCounterString();

        docName = (String) document.getAttribute("docname") + ".adoc";
        System.out.println("doc name= " + docName);

        List<String> lines = reader.readLines();
        List<String> newLines = new ArrayList();

        // managing titles and slides beginnings
        boolean previousLineIsPic = false;
        boolean previousLineIsPageBreak = false;
        for (String line : lines) {
            
            if (line.trim().equals(info)){
                line = line.substring(2).trim();
            }

            // we put 4 level titles instead of three, as level three would trigger vertical scrolling of slides, which I don't want
            if (line.startsWith("=== ")) {
                line = "=" + line;
            }

            if (line.startsWith("==== ")) {
                if (!previousLineIsPageBreak) {
                    newLines.add("== !");
                }
            }

            // inserting slide breaks when the //+ annotation is present
            if (line.equals("//+") || line.equals("// +")) {
                newLines.add("");
                line = line.replace("//+", "== !").trim();
                line = line.replace("// +", "== !").trim();
            }

            // we don't need to add extra white lines after pics for slide output
            if (previousLineIsPic && line.startsWith("{nbsp} +")) {
                line = "";
            }

            //adding a "stretch" class to images and a page break before. See: https://github.com/asciidoctor/asciidoctor-reveal.js/#stretch-class-attribute
            if (line.startsWith("image::") | line.startsWith("video:")) {
                if (!previousLineIsPageBreak) {
                    newLines.add("== !");
                }
                newLines.add("[.stretch]");
                previousLineIsPic = true;
            } else {
                previousLineIsPic = false;
            }
            newLines.add(line);

            // add a page (slide) break after titles, images, videos... if there was not a page break at the previous line already
            if (!line.equals("====") & ((line.startsWith("=") | line.startsWith("image::") | line.startsWith("video:")) & !line.trim().equals("== !"))) {
                newLines.add("== !");
                previousLineIsPageBreak = true;
            } else if (!line.trim().isEmpty()) {
                previousLineIsPageBreak = false;
            }

            // signaling that titles have generated page breaks: so images immediately following titles won't need to be preceded by an additional page break
            if (!line.equals("====") &&(line.startsWith("=") | line.startsWith("image::") | line.startsWith("video:") | line.startsWith("'Escape' or 'o'"))) {
                previousLineIsPageBreak = true;
            }

        }

        newLines.add("pass:[" + statcounter + "]");

        reader.restoreLines(newLines);
    }

    private String buildStatCounterString() {
        statcounter = "    <!-- Start of StatCounter Code for Default Guide -->\n"
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
