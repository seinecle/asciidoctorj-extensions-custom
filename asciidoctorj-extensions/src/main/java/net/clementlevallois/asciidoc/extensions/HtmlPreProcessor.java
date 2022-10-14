/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.asciidoc.extensions;

import java.io.IOException;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Preprocessor;
import org.asciidoctor.extension.PreprocessorReader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

public class HtmlPreProcessor extends Preprocessor {

    Path directorySourceFile;
    Path directoryAllSources;
    Path basedir;

    String statcounterProject;
    String statcounterSecurity;
    String statcounter;

    public HtmlPreProcessor(Map<String, Object> config) {
        super(config);
    }

    @Override
    public void process(Document document, PreprocessorReader reader) {

        try {
            System.out.println("in the html preprocessor");

            statcounterProject = (String) document.getAttribute("statcounter-project");
            statcounterSecurity = (String) document.getAttribute("statcounter-security");
            statcounter = buildStatCounterString();

            System.out.println("in the slides preprocessor");

            statcounterProject = (String) document.getAttribute("statcounter-project");
            statcounterSecurity = (String) document.getAttribute("statcounter-project");
            statcounter = buildStatCounterString();

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

            Path pathSourceImageFolderForThisDoc = Path.of(directoryAllSources.toString(), "images", docName);
            Path pathTargetImageFolderForThisDoc = Path.of(basedir.toString(), "docs", "generated-html", "images", docName);

            pathTargetImageFolderForThisDoc.toFile().mkdirs();

            FileUtils.copyDirectory(pathSourceImageFolderForThisDoc.toFile(), pathTargetImageFolderForThisDoc.toFile());

            List<String> lines = reader.readLines();
            List<String> newLines = new ArrayList();
            for (String line : lines) {
                newLines.add(line);
            }
            newLines.add("pass:[" + statcounter + "]");

//        reader.push_include(sb.toString(), "", "", 1, document.getAttributes());
            reader.restoreLines(newLines);
//        try {
//            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(path.toFile(), (String) document.getAttribute("docname") + "_temp_html.md")), "UTF-8"));
//            bw.write(sb.toString());
//            bw.close();
//        } catch (IOException ex) {
//            Logger.getLogger(CommonPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
//        }
        } catch (IOException ex) {
            Logger.getLogger(HtmlPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
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
