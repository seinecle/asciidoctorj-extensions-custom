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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonPreProcessor extends Preprocessor {

    /**
     * this pre processor does these things: 1. downloading the image urls and
     * saving them to png
     *
     * 2. then replacing image:: http://... by image: download.png in the
     * aasciidoc document
     *
     * 3. writing this new version of the asciidoc in a temp folder callded
     * "subdir". The maven build will use the doc from this temp folder.
     *
     *
     *
     */
    private static final Pattern urlPattern = Pattern.compile(
            "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
            + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
            + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    String docName;
    String docToProcess;
    static Path baseDir;
    Path allSourcesImagesDocname;
    Path allSourcesImages;
    boolean refreshPics;
    String magickPath;

    Path tempSources;
    Path allSources;
    Path tempSourcesImages;
    Path tempSourcesOneFolderForOneDoc;

    public CommonPreProcessor(Map<String, Object> config) {
        super(config);
    }

    @Override
    public void process(Document document, PreprocessorReader reader) {

        System.out.println("in the common preprocessor");

        docName = (String) document.getAttribute("docname");
        docName = (String) document.getAttribute("docname") + ".adoc";
        System.out.println("doc name= " + docName);

        String refreshPicsString = ((String) document.getAttribute("refresh-pics"));
        refreshPics = !(refreshPicsString == null || !refreshPicsString.toLowerCase().equals("yes"));

        magickPath = (String) document.getAttribute("magick-path");
        System.out.println("magic path: " + magickPath);

        allSources = Paths.get((String) document.getAttribute("source-directory"));
        System.out.println("dir all sources = " + allSources);

        allSourcesImages = Path.of(allSources.toString(), "images");

        allSourcesImagesDocname = Path.of(allSources.toString(), "images", docName);
        allSourcesImagesDocname.toFile().mkdirs();

        System.out.println("all sources / images / docname: " + this.allSourcesImagesDocname);

        docToProcess = (String) document.getAttribute("doc-to-process");
        System.out.println("doc-to-process= " + docToProcess);

        tempSources = Path.of(allSources.toString(), "subdir"); // because the pre-processor extension delivered an intermediary source file in this subdir folder.
        System.out.println("source directory TRANSFORMED = " + tempSources);

        tempSourcesImages = Path.of(tempSources.toString(), "images");
        tempSourcesImages.toFile().mkdirs();

        tempSourcesOneFolderForOneDoc = Paths.get(tempSources.toString() + "/" + docName);
        System.out.println("preprocessed intermediary file will be saved as " + tempSourcesOneFolderForOneDoc.toString());

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
                        Path allSourcesImagesOneImage = Path.of(allSourcesImages.toString(), titlePic + ".png");
                        String allSourcesImagesDocnameOnePicture = allSourcesImagesDocname + "/" + titlePic + ".png";
                        String tempFolderImagesOnePicture = tempSourcesImages + "/" + titlePic + ".png";

                        // for the HTML and SLIDES builds
                        if (!Path.of(allSourcesImagesDocnameOnePicture).toFile().exists()) {
                            // copy the image to the folder in images dedicated to this document
                            Files.copy(allSourcesImagesOneImage, Path.of(allSourcesImagesDocnameOnePicture));
                        }
                        // for the PDF build
                        if (!Path.of(tempFolderImagesOnePicture).toFile().exists()) {
                            // copy the image to the folder in subdir / images
                            Files.copy(allSourcesImagesOneImage, Path.of(tempFolderImagesOnePicture));
                        }
                    } catch (IOException | DocumentException ex) {
                        Logger.getLogger(CommonPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

            if (line.startsWith("image:")) {
//                if (line.contains("milano.jpg")){
//                    System.out.println("stop");
//                }
//                extension = ImageAttributeExtractor.extractExtension(line);
                line = line.replace(extension, "");
                String fileType = line.substring(line.lastIndexOf("."), line.length());
                String fileName = line.replace("image::", "");
                fileName = fileName.replace("image:", "");
                String pathImageInImageDirWithFileName = allSourcesImagesDocname + "/" + fileName;
                String tempFolderImagesOnePicture = tempSourcesImages + "/" + fileName;
                Path imageInGeneral = Path.of(allSourcesImages.toString(), fileName);
                if (!Path.of(pathImageInImageDirWithFileName).toFile().exists()) {
                    Path.of(pathImageInImageDirWithFileName).toFile().mkdirs();
                    try {
                        // for the HTML and SLIDES builds
                        if (imageInGeneral.toFile().exists() & !Path.of(pathImageInImageDirWithFileName).toFile().exists()) {
                            Files.copy(imageInGeneral, Path.of(pathImageInImageDirWithFileName));
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(CommonPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                if (!Path.of(tempFolderImagesOnePicture).toFile().exists()) {
                    try {
                        // for the PDF build
                        // copy the image to the folder in subdir / images
                        Files.copy(imageInGeneral, Path.of(tempFolderImagesOnePicture));
                    } catch (IOException ex) {
                        Logger.getLogger(CommonPreProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                if (extension.toLowerCase().contains("\"landscape")) {
                    String pathRotatedPic = pathImageInImageDirWithFileName.replace(fileType, "_panorama" + fileType);
                    String pathPicInTextDirectory = pathRotatedPic;
                    if (!new File(pathPicInTextDirectory).exists()) {
                        try {
                            rotateImageByDegreesWithJMagick(Paths.get(pathImageInImageDirWithFileName).toFile(), 90, fileType);
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
            BufferedWriter bw = Files.newBufferedWriter(tempSourcesOneFolderForOneDoc, Charset.forName("UTF-8"));
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

            File imageFile = new File(allSourcesImagesDocname.toString() + "/", title + ".png");
            File imageFileThumbnail = new File(allSourcesImagesDocname.toString() + "/", title + "_thumbnail.png");

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
