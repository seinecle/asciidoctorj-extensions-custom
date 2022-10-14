/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.asciidoc.extensions;

import java.io.IOException;
import java.nio.file.Files;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Preprocessor;
import org.asciidoctor.extension.PreprocessorReader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

public class SlidesPreProcessor extends Preprocessor {

    Path directorySourceFile;
    Path directoryAllSources;
    Path basedir;

    String statcounterProject;
    String statcounterSecurity;
    String statcounter;

    public SlidesPreProcessor(Map<String, Object> config) {
        super(config);
    }

    @Override
    public void process(Document document, PreprocessorReader reader) {

        try {
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
            Path pathTargetImageFolderForThisDoc = Path.of(basedir.toString(), "docs", "generated-slides", "images", docName);

            pathTargetImageFolderForThisDoc.toFile().mkdirs();

            FileUtils.copyDirectory(pathSourceImageFolderForThisDoc.toFile(), pathTargetImageFolderForThisDoc.toFile());

            //writing this modified document to a temp folder, to be used by the revealjs maven build (see POM)
//        final Path path = Paths.get(docBasedir.toString() + "/subdir");
//        path.toFile().mkdirs();
            List<String> lines = reader.readLines();
            List<String> newLines = new ArrayList();

// managing titles and slides beginnings
            boolean previousLineIsPic = false;
            boolean previousLineIsPageBreak = false;
            for (String line : lines) {

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
                if (line.startsWith("=") | line.startsWith("image::") | line.startsWith("video:")) {
                    previousLineIsPageBreak = true;
                }

            }

            newLines.add("pass:[" + statcounter + "]");

            reader.restoreLines(newLines);

//        try {
//            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(path.toFile(), (String) document.getAttr("docname") + "_temp_slides.md")), "UTF-8"));
//            bw.write(sb.toString());
//            bw.close();
//        } catch (IOException ex) {
//            Logger.getLogger(SlidesPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
//        }
        } catch (IOException ex) {
            Logger.getLogger(SlidesPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
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
