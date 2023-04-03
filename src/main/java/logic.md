SOURCES:
# all docs are in asciidoc folder

# all images are in images folder

# all transformed adocs are in the subdir folder, in a subfolder of the same name

# all transformed images are in the subdir folder, within a subfolder of the same name of the doc they refer to, in a subdolder called "images"

docs/
    asciidoc/
            random-doc.adoc
            images/
            subdir/
                  random-doc.adoc/  
                                 random-doc-transformed.adoc 
                                 images/
                          


RESULTS:

docs/
    generated-pdf/
    generated-slides/
                    random-doc.adoc
                    images/
                           random-doc.adoc/

    generated-html/
                    random-doc.adoc
                    images/
                           random-doc.adoc/

