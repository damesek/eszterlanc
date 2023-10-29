(ns eszterlanc.core
  (:import (hu.u_szeged.cons.parser MyBerkeleyParser)
           (hu.u_szeged.dep.parser MyMateParser)
           (hu.u_szeged.magyarlanc Magyarlanc)
           (hu.u_szeged.pos.purepos MyPurePos)
           (splitter MySplitter)
           (java.util List)))


(def test-text
  "Két folyó találkozásánál fekszik a város, ami a várakozások ellenére nem egy oroszlánról kapta a nevét. Megmutatjuk a legjobb kocsmákat, és hogy hol lehet a legnagyobb a magyarok öröme.")



(defn sentence-array
  "Split sentences to arrays."
  [sentences]
  (.splitToArray (MySplitter/getInstance) sentences))


(defn object->clj
  [obj]
  (->> obj
       (java.util.Arrays/asList)
       (mapv vec)))


(defn sentences-list
  "Split sentences to lists."
  [sentences]
  (.split (MySplitter/getInstance) sentences))


(defn morph-array
  "The morphological parser is a code based on the
   finite state automata written by György Gyepesi,
   which was built on the resource morphdb.hu."
  [sentences-array]
  (for [sentence sentences-array]
    (object->clj (.morphParseSentence (MyPurePos/getInstance) sentence))))



(defn morph-list
  "The morphological parser list version"
  [sentences-list]
  (doseq [sentence sentences-list]
    (.morphParseSentence (MyPurePos/getInstance) sentence)))


; New Deep-Syntactic Parser available at:
; https://code.google.com/p/deepsyntacticparsing/

(defn dependency
  "https://code.google.com/archive/p/mate-tools/
   Tools for Natural Language Analysis
   adapted to Hungarian
   Input: array"
  [sentences-array]
  (for [sentence sentences-array]
    (object->clj (.parseSentence (MyMateParser/getInstance) sentence))))


(defn constituency
  "Based on https://github.com/slavpetrov/berkeleyparser
   Learning Accurate, Compact, and Interpretable Tree Annotation
   adapted to Hungarian
   Input: array"
  [sentences-array]
  (for [sentence sentences-array]
    (object->clj (.parseSentence (MyBerkeleyParser/getInstance) sentence))))


(defn magyarlanc [sentences-array]
  "Magyarlanc Clojure verzio.
   Mindent visszad: morph, dependencia, konstituens."
  (map (fn [sentence]
         (let [morph (object->clj (Magyarlanc/morphParseSentence sentence))
               dep   (object->clj (Magyarlanc/depParseSentence sentence))
               kons  (object->clj (Magyarlanc/constParseSentence sentence))]
           {:morph morph
            :dep   dep
            :kons  kons}))
       sentences-array))


(defn lemma-for-word
  "Ha nem valos szot adunk meg, akkor is visszaadja az eredetit.
   Ez megteveszto lehet, tovabbi ellenorzest igenyelhet.
   Pl. spell checking"
  [word]
  (->> (.morphParseSentence (MyPurePos/getInstance) [word])
       (java.util.Arrays/asList)
       first second))

