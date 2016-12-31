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

public class ImagePreprocessor extends Preprocessor {

    //this pre processor does  things:
    //1. downloading the image urls and saving them to png, then replacin image:: http://... byt image: download.png in the aasciidoc document
    //2. writing this new version of the asciidoc in a temp folder. The maven build for revealjs will use the doc from this temp folder.

    //later, I will add more tasks to this pre-processor, split in several pre preprocessors for maintainability.

    private static final Pattern urlPattern = Pattern.compile(
            "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
            + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
            + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    public ImagePreprocessor(Map<String, Object> config) {
        super(config);
    }

    @Override
    public PreprocessorReader process(Document document, PreprocessorReader reader) {
        //reading the doc, detecting image::http... and replacing it with the pic that is downloaded.
        StringBuilder sb = new StringBuilder();

        List<String> lines = reader.readLines();

        for (String line : lines) {
            if (line.startsWith("image::http")) {
                try {
                    String titlePic = downloadPicAndReturnTitle(line);
                    line = "image::" + titlePic + ".png[" + titlePic + "]";
                } catch (IOException ex) {
                    Logger.getLogger(ImagePreprocessor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            sb.append(line);
            sb.append("\n");
            System.out.println("line: " + line);
        }
        reader.push_include(sb.toString(), "", "", 1, document.getAttributes());

        //writing this modified document to a temp folder, to be used by the revealjs maven build (see POM)
        final Path path = Paths.get(Parameters.localDirectory + Parameters.endOfPath + "temp");
        path.toFile().mkdirs();

        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(path.toFile(),"temp.adoc")),"UTF-8"));
            bw.write(sb.toString());
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(ImagePreprocessor.class.getName()).log(Level.SEVERE, null, ex);
        }

        return reader;
    }

    private String downloadPicAndReturnTitle(String line) throws MalformedURLException, IOException {
        Matcher matcher = urlPattern.matcher(line);
        BufferedImage image = null;
        String title;
        if (line.contains("title=")) {
            String subline = line.substring(line.indexOf("title"));
            subline = subline.substring(subline.indexOf("=") + 1, subline.indexOf("]"));
            subline = subline.replaceAll("\"", "");
            title = subline;
        } else {
            title = "no-title";
        }
        System.out.println("title of pic: " + title);

        while (matcher.find()) {
            int matchStart = matcher.start(1);
            int matchEnd = matcher.end();
            String urlInLine = line.substring(matchStart, matchEnd);
            urlInLine = urlInLine.substring(0, urlInLine.indexOf("["));

            //applying default width:
            if (Parameters.applyDefaultPicHeight) {
                if (urlInLine.contains("?w=")) {
                    urlInLine = urlInLine.substring(0, urlInLine.indexOf("?w="));
                }
                urlInLine = urlInLine + "?h=" + Parameters.defaultPicHeight;
            }

            URL url = new URL(urlInLine);
            System.out.println("url: " + urlInLine);
            image = ImageIO.read(url);
            Path path = Paths.get(Parameters.localDirectory + "images\\" + title + ".png");
            path.toFile().mkdirs();
            Files.deleteIfExists(path);
            File imageFile = new File(Parameters.localDirectory + "images", title + ".png");
            System.out.println("Saving image to: " + imageFile.getAbsolutePath());
            ImageIO.write(image, "png", imageFile);
        }
        return title;
    }

}
