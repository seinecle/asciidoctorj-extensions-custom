/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.extension.JavaExtensionRegistry;
import org.asciidoctor.extension.spi.ExtensionRegistry;

/**
 * @author LEVALLOIS
 */
public class ImagePreprocessorExtension implements ExtensionRegistry {

    @Override
    public void register(Asciidoctor asciidoctor) {

        System.out.println("Registering extension " + ImagePreprocessor.class.getSimpleName());
        JavaExtensionRegistry javaExtensionRegistry = asciidoctor.javaExtensionRegistry();
        javaExtensionRegistry.preprocessor(ImagePreprocessor.class);
    }

}
