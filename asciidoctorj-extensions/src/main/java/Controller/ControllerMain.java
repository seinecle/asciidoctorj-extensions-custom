/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.extension.JavaExtensionRegistry;

/**
 *
 * @author LEVALLOIS
 */
public class ControllerMain {
    /**
     * Not needed in Maven approach
     */
    // tag::contains[]
    public static void main(String[] args) throws IOException {

        Asciidoctor asciidoctor = Asciidoctor.Factory.create();

        
        //converting to pdf
        Attributes attributes = new Attributes();
        attributes.setImagesDir("../../images");
        attributes.setAllowUriRead(true);

        Options options = new Options();
        options.setAttributes(attributes);
        options.setInPlace(true);
        options.setBackend("pdf");
        options.setSafe(SafeMode.UNSAFE);

        options.setInPlace(true);

        //deleting the previously converted pdf doc, if any
        Path path = Paths.get(Parameters.localDirectory + Parameters.endOfPath + Parameters.docName + ".pdf");
        Files.deleteIfExists(path);

        asciidoctor.convertFile(new File(Parameters.localDirectory + Parameters.endOfPath + Parameters.docName + ".adoc"), options);

        
        asciidoctor.shutdown();
    }
    // end::contains[]

}
