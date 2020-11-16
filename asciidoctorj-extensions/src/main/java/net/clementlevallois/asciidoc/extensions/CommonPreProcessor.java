/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.asciidoc.extensions;

import Utils.ImageAttributeExtractor;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfImage;
import com.itextpdf.text.pdf.PdfIndirectObject;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Preprocessor;
import org.asciidoctor.extension.PreprocessorReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
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

    static String asciiDocBasedir;
    String docName;
    String docToProcess;
    static Path docBasedir;
    static Path baseDir;
    Path imagesDir;
    boolean refreshPics;
    String lang;
    Map<String, Object> config;
    String magickPath;

    public CommonPreProcessor(Map<String, Object> config) {
        super(config);
        this.config = config;
    }

    @Override
    public void process(Document document, PreprocessorReader reader) {

        System.out.println("in the common preprocessor");

        asciiDocBasedir = System.getProperty("asciiDocMavenJavaSE.basedir");
        baseDir = Paths.get((String) document.getAttribute("path"));
        docBasedir = Paths.get((String) document.getAttribute("docdir"));
        docName = (String) document.getAttribute("docname");
        String refreshPicsString = ((String) document.getAttribute("refresh-pics"));
//        String langString = ((String) document.getAttr("lang"));
        imagesDir = Paths.get((String) document.getAttribute("images-dir"));
        refreshPics = !(refreshPicsString == null || !refreshPicsString.toLowerCase().equals("yes"));
//        lang = "-" + langString;
        magickPath = (String) document.getAttribute("magick-path");
        System.out.println("magic path: " + magickPath);

        if (docToProcess != null && !docToProcess.isEmpty()) {
            if (!(docName + ".adoc").equals(docToProcess)) {
            }
        }
//        if (lang.equals("-fr")) {
//            if (!docName.contains("-fr")) {
//            return null;
//            }
//        }

        System.out.println("doc name= " + docName);

        final Path pathSubdir = Paths.get(docBasedir.toString()+ "/subdir/" );
        pathSubdir.toFile().mkdirs();
        final Path pathTempFile = Paths.get(pathSubdir.toString() +"/"+  docName+ ".adoc");
        System.out.println("preprocessed intermediary file will be saved as " + pathTempFile.toString());

        List<String> lines = reader.readLines();
        List<String> newLines = new ArrayList();

        String previousLine = "";

        for (String line : lines) {
            String extension = "";

            // the following 2 if conditions fix iframes which were not surrounded by ++++
            if (line.startsWith("<iframe") && !previousLine.startsWith("++++")) {
                newLines.add("++++");
            }

            if (!line.startsWith("++++") && previousLine.startsWith("<iframe")) {
                newLines.add("++++");
            }
            if (line.startsWith("image:")) {
                extension = ImageAttributeExtractor.extractExtension(line);
            }

            String picFileName = "";

            //detecting image::http... and replacing it with the pic that is downloaded.
            if (line.startsWith("image::http") | line.startsWith("image:http")) {
                if (!line.split("\\[")[0].endsWith(".gif") & !line.split("\\[")[0].endsWith(".png") & !line.split("\\[")[0].endsWith(".jpg") & !line.split("\\[")[0].endsWith(".jpeg")) {
                    try {
                        String titlePic = downloadPicAndReturnTitle(line);
                        if (line.startsWith("image:") & !line.startsWith("image::")) {
                            line = "image:" + titlePic + ".png" + extension;
                        }
                        if (line.startsWith("image::")) {
                            line = "image::" + titlePic + ".png" + extension;
                        }
                        picFileName = imagesDir + "/" + titlePic + ".png";
                    } catch (IOException ex) {
                        Logger.getLogger(CommonPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (DocumentException ex) {
                        Logger.getLogger(CommonPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

            if (line.startsWith("image:")) {
                extension = ImageAttributeExtractor.extractExtension(line);
            }

            if (line.startsWith("image::")) {
                line = line.replace(extension, "");
                String fileType = line.substring(line.lastIndexOf("."), line.length());
                String fileName = line.replace("image::", "");
                String path = imagesDir + "/" + fileName;

                if (extension.toLowerCase().contains("\"landscape")) {
                    String pathRotatedPic = path.replace(fileType, "_panorama" + fileType);
                    picFileName = pathRotatedPic;
                    if (!new File(pathRotatedPic).exists()) {
                        try {
                            rotateImageByDegreesWithJMagick(Paths.get(path).toFile(), 90, fileType);
                            Thread.sleep(1000l);
                        } catch (IOException | InterruptedException ex) {
                            Logger.getLogger(CommonPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    line = "image::" + fileName.replace(fileType, "_panorama" + fileType);
                }
                line = line + extension;

            }

            newLines.add(line);

            //adding an empty line after pictures and their legend.
            if (line.startsWith("image::") | line.startsWith("video::")) {
                newLines.add("{nbsp}");
            }
            previousLine = line;
        }

        reader.restoreLines(newLines);

        try {
            BufferedWriter bw = Files.newBufferedWriter(pathTempFile, Charset.forName("UTF-8"), StandardOpenOption.CREATE);
            StringBuilder sb = new StringBuilder();
            newLines.iterator().forEachRemaining((line) -> sb.append(line).append("\n"));
            bw.write(sb.toString());
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(CommonPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private String downloadPicAndReturnTitle(String line) throws MalformedURLException, IOException, DocumentException {
        Matcher matcher = urlPattern.matcher(line);
        BufferedImage bufferedImage;
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

            File imageFile = new File(imagesDir.toString() + "/", title + ".png");
            File imageFileThumbnail = new File(imagesDir.toString() + "/", title + "_thumbnail.png");

            if (imageFile.exists() & !refreshPics) {
                continue;
            }

            URL url = new URL(urlInLine);
            System.out.println("url: " + urlInLine);
            bufferedImage = ImageIO.read(url);
            BufferedImage thumbnail = scale(bufferedImage, 0.3, 0.3);
            System.out.println("Saving image to: " + imageFile.getAbsolutePath());
            ImageIO.write(bufferedImage, "png", imageFile);
            ImageIO.write(thumbnail, "png", imageFileThumbnail);
        }
        return title;
    }

    public File rotateImageByDegreesWithJMagick(File imgFile, double degrees, String fileExtension) throws IOException {
        String pathRotatedImage = imgFile.getAbsolutePath().replace(fileExtension, "_panorama" + fileExtension);

        ProcessBuilder pb = new ProcessBuilder(magickPath, "convert", "-rotate", "90", imgFile.getAbsolutePath(), pathRotatedImage);
        pb.start();
        File rotatedImage = new File(pathRotatedImage);
        return rotatedImage;
    }

    public BufferedImage rotateImageByDegrees(BufferedImage img, double degrees) {
        double rads = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));
        int w = img.getWidth();
        int h = img.getHeight();
        int newWidth = (int) Math.floor(w * cos + h * sin);
        int newHeight = (int) Math.floor(h * cos + w * sin);

        BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotated.createGraphics();
        AffineTransform at = new AffineTransform();
        at.translate((newWidth - w) / 2, (newHeight - h) / 2);

        int x = w / 2;
        int y = h / 2;

        at.rotate(rads, x, y);
        g2d.setTransform(at);
        //not sure null is ok
        g2d.drawImage(img, 0, 0, null);
        g2d.setColor(Color.RED);
        g2d.drawRect(0, 0, newWidth - 1, newHeight - 1);
        g2d.dispose();

        return rotated;
    }

    public static BufferedImage scale(BufferedImage sbi, double fWidth, double fHeight) {
        BufferedImage dbi = null;
        int dWidth = (int) Math.round(sbi.getWidth() * fWidth);
        int dHeight = (int) Math.round(sbi.getHeight() * fHeight);
        dbi = new BufferedImage(dWidth, dHeight, sbi.getType());
        Graphics2D g = dbi.createGraphics();
        AffineTransform at = AffineTransform.getScaleInstance(fWidth, fHeight);
        g.drawRenderedImage(sbi, at);
        return dbi;
    }

    public static BufferedImage setBackgroundToBlack(BufferedImage sbi) {
        for (int i = 0; i < sbi.getWidth(); i++) {
            for (int j = 0; j < sbi.getHeight(); j++) {
                // get argb from pixel
                int coli = sbi.getRGB(i, j);
                int a = coli >> 24 & 0xFF;
                int r = coli >> 16 & 0xFF;
                int g = coli >> 8 & 0xFF;
                int b = coli & 0xFF;
                coli &= ~0xFFFFFFFF;
                // do what you want with a, r, g and b, in your case :
                a = 0xFF;
                // save argb
                coli |= a << 24;
                coli |= r << 16;
                coli |= g << 8;
                coli |= b << 0;
                sbi.setRGB(i, j, coli);
            }
        }
        return sbi;
    }

    public static void saveCanvasAsPdf(File imageFile, String imageTitle) throws IOException, BadElementException, DocumentException {

        Image image = Image.getInstance(imageFile.getAbsolutePath());

        // print to A4
        imageTitle = imageTitle.replace(".png", "");
        Path pathPdfImage = Paths.get(baseDir + "/docs/pdf/" + imageTitle + "_A4.pdf");

        PdfReader reader = new PdfReader(baseDir + "/docs/generated-book/blank-page.pdf");
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(pathPdfImage.toString()));
        PdfImage stream = new PdfImage(image, "", null);
        stream.put(new PdfName("ITXT_SpecialId"), new PdfName("123456789"));
        PdfIndirectObject ref = stamper.getWriter().addToBody(stream);
        image.setDirectReference(ref.getIndirectReference());
        image.setAbsolutePosition(1, 1);
        image.scaleToFit(PageSize.A4);

        PdfContentByte over = stamper.getOverContent(1);
        over.addImage(image);
        stamper.close();
        reader.close();

        // print to A3
        imageTitle = imageTitle.replace(".png", "");
        pathPdfImage = Paths.get(baseDir + "/docs/pdf/" + imageTitle + "_A3.pdf");

        reader = new PdfReader(baseDir + "/docs/generated-book/blank-page-A3.pdf");
        stamper = new PdfStamper(reader, new FileOutputStream(pathPdfImage.toString()));
        stream = new PdfImage(image, "", null);
        stream.put(new PdfName("ITXT_SpecialId"), new PdfName("123456789"));
        ref = stamper.getWriter().addToBody(stream);
        image.setDirectReference(ref.getIndirectReference());
        image.setAbsolutePosition(1, 1);
        image.scaleToFit(PageSize.A3);

        over = stamper.getOverContent(1);
        over.addImage(image);
        stamper.close();
        reader.close();

    }

}
