(ns eszterlanc.gui
  (:require [eszterlanc.core :refer [object->clj]])
  (:import [hu.u_szeged.gui GUI]
           [hu.u_szeged.magyarlanc Magyarlanc]
           [hu.u_szeged.cons.vis BerkeleyUIWrapper]
           [hu.u_szeged.dep.whatswrong WhatsWrongWrapper]
           [javax.imageio ImageIO]
           [java.io ByteArrayInputStream]
           [java.io File]))


(defn init
  "Start the GUI."
  []
  (GUI/init))


(defn- get-buffered-image
  "Parses the input sentence using the `Magyarlanc/parseSentence` method.
   Returns a visual representation of the parsed sentence as a `BufferedImage`
   using the `BerkeleyUIWrapper/getImage` method."
  [sentence]
  (let [parsed (Magyarlanc/parseSentence sentence)]
    (BerkeleyUIWrapper/getImage parsed)))


(defn- get-image-from-bytes
  "Parses the input sentence using the `Magyarlanc/parseSentence` method.
   Converts the parsed result into a byte array using the
   `WhatsWrongWrapper/exportToByteArray` method. Subsequently, the byte array is
   transformed into a `BufferedImage` and returned."
  [sentence]
  (let [parsed (Magyarlanc/parseSentence sentence)]
    (ImageIO/read (ByteArrayInputStream.
                    (WhatsWrongWrapper/exportToByteArray parsed)))))


(defn save-image-berkley
  "Retrieves a visual representation of the parsed sentence using the
   `get-buffered-image` function and saves it as a PNG image to the specified path."
  [sentence path]
  (let [image (get-buffered-image sentence)]
    (ImageIO/write image "png" (File. path))))


(defn save-image-whatswrong
  "Retrieves a visual representation of the parsed sentence using the
   `get-image-from-bytes` function and saves it as a PNG image to the specified path."
  [sentence path]
  (let [image (get-image-from-bytes sentence)]
    (ImageIO/write image "png" (File. path))))


(defn save-image-to-resources
  "Obtains two visual representations of the parsed sentence: one via
   `get-buffered-image` and another via `get-image-from-bytes`. Both images are
   saved as PNG files in the specified directory with distinct filenames
   (`image-berkley.png` and `image-whatswrong.png`)."
  [sentence path]
  (let [image-berkley (get-buffered-image sentence)
        image-whatswrong (get-image-from-bytes sentence)]
    (do (ImageIO/write image-berkley "png" (File. (str path "image-berkley.png")))
        (ImageIO/write image-whatswrong "png" (File. (str path "image-whatswrong.png"))))))



(comment

  (save-image-berkley "Ez egy teszt, amit nagyon jó lett." "temp.png")
  (save-image-whatswrong "Ez egy teszt, amit nagyon jó lett." "temp-w.png")
  (save-image-to-resources "Ez egy teszt, amit nagyon jó lehetett volna."
                           "resources/magyarlanc-exported-images/")

  (init)
  )