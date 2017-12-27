/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.asciidoc.extensions;

import Utils.ImageAttributeExtractor;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Preprocessor;
import org.asciidoctor.extension.PreprocessorReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonPreProcessor extends Preprocessor {

    //this pre processor does  things:
    //1. downloading the image urls and saving them to png, then replacing image:: http://... by image: download.png in the aasciidoc document
    //2. writing this new version of the asciidoc in a temp folder. The maven build for revealjs will use the doc from this temp folder.
    //later, I will add more tasks to this pre-processor, split in several pre preprocessors for maintainability.
    private static final Pattern urlPattern = Pattern.compile(
            "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
            + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
            + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    String asciiDocBasedir;
    String docName;
    String docToProcess;
    Path docBasedir;
    boolean refreshPics;

    public CommonPreProcessor(Map<String, Object> config) {
        super(config);
    }

    @Override
    public PreprocessorReader process(Document document, PreprocessorReader reader) {

        System.out.println("in the common preprocessor");

        asciiDocBasedir = System.getProperty("asciiDocMavenJavaSE.basedir");
        docBasedir = Paths.get((String) document.getAttr("docdir"));
        docToProcess = (String) document.getAttr("doc-to-process");
        docName = (String) document.getAttr("docname");
        refreshPics = Boolean.valueOf((String)document.getAttr("refresh-pics"));

        System.out.println("doc-to-process= " + docToProcess);
        System.out.println("doc name= " + docName);
        
        StringBuilder sb = new StringBuilder();

        List<String> lines = reader.readLines();

        for (String line : lines) {

            //detecting image::http... and replacing it with the pic that is downloaded.
            if (line.startsWith("image::http") | line.startsWith("image:http")) {
                String extension = ImageAttributeExtractor.extractExtension(line);
                if (!line.split("\\[")[0].endsWith(".gif") & !line.split("\\[")[0].endsWith(".png") & !line.split("\\[")[0].endsWith(".jpg") & !line.split("\\[")[0].endsWith(".jpeg")) {
                    try {
                        String titlePic = downloadPicAndReturnTitle(line);
                        line = "image::" + titlePic + ".png" + extension;
                    } catch (IOException ex) {
                        Logger.getLogger(CommonPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

            sb.append(line);
            sb.append("\n");

            //adding an empty line after pictures and their legend.
            if (line.startsWith("image::")) {
                sb.append("{nbsp} +\n");
            }
        }
        reader.push_include(sb.toString(), "", "", 1, document.getAttributes());

        //writing this modified document to a temp folder, to be used by the revealjs maven build (see POM)
        final Path path = Paths.get(docBasedir.toString() + "/subdir");
        path.toFile().mkdirs();

        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(path.toFile(), (String) document.getAttr("docname") + "_temp_common.md")), "UTF-8"));
            bw.write(sb.toString());
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(CommonPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return reader;
    }

    private String downloadPicAndReturnTitle(String line) throws MalformedURLException, IOException {
        Matcher matcher = urlPattern.matcher(line);
        BufferedImage image;
        String title = ImageAttributeExtractor.extractTitle(line);
        System.out.println("title of pic: " + title);

        while (matcher.find()) {
            int matchStart = matcher.start(1);
            int matchEnd = matcher.end();
            String urlInLine = line.substring(matchStart, matchEnd);
            urlInLine = urlInLine.substring(0, urlInLine.indexOf("["));

            //applying default width:
            if (CommonParameters.applyDefaultPicHeight) {
                if (urlInLine.contains("?w=")) {
                    urlInLine = urlInLine.substring(0, urlInLine.indexOf("?w="));
                }
                if (urlInLine.contains("docs.google.com/drawings")) {
                    urlInLine = urlInLine + "?h=" + CommonParameters.defaultPicHeight;
                }
            }

            File imageFile = new File(docBasedir.toString() + "/images/", title + ".png");

            if (imageFile.exists() && !refreshPics) {
                continue;
            }

            URL url = new URL(urlInLine);
            System.out.println("url: " + urlInLine);
            image = ImageIO.read(url);
//            Path path = Paths.get(docBasedir.toString()+ "/images/" + title + ".png");
//            path.toFile().mkdirs();
//            Files.deleteIfExists(path);
            System.out.println("Saving image to: " + imageFile.getAbsolutePath());
            ImageIO.write(image, "png", imageFile);
        }
        return title;
    }
}
