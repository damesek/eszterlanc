(ns eszterlanc.core-test
  (:require [clojure.test :refer :all]
            [eszterlanc.core :refer :all]))



(def test-sentences
  "Kezdetben teremté Isten az eget és a földet. A föld pedig kietlen és puszta vala,
  és setétség vala a mélység színén, és az Isten Lelke lebeg vala a vizek felett.")


(deftest eszterlanc-core-tests
  (let [sa (sentence-array test-sentences)]
    (testing "Test sentence-array we get java object"
      (= (Class/forName "[[Ljava.lang.String;")
         (type sa)))
    (testing "With object->clj we get the right format"
      (= "Kezdetben"
         (->> sa object->clj ffirst)))))


(deftest parser-tests
  (let [sa (sentence-array test-sentences)]
    (testing "With morphological parser, array version"
      (= ["Kezdetben" "kezdet" "NOUN" "Case=Ine|Number=Sing"]
         (->> sa morph-array ffirst)))
    (testing "With Mate-tools Natural language analysis"
      (= ["1" "Kezdetben" "kezdet" "NOUN" "Case=Ine|Number=Sing" "2" "OBL"]
         (->> sa dependency ffirst)))
    (testing "With Berkley Tree Annotation parser"
      (= ["Kezdetben" "kezdet" "NOUN" "Case=Ine|Number=Sing" "(ROOT(CP(NP*)"]
         (->> sa constituency ffirst)))))

(deftest magyarlanc-tests
  (let [sa (sentence-array test-sentences)]
    (testing "With magyarlanc, could we get back all functionality?"
      (= (:morph :dep :kons)
         (->> sa magyarlanc first keys)))
    (testing "Does Magyarlanc give the same result as the morphological parser?"
      (= (->> sa magyarlanc first :morph)
         (->> sa morph-array first)))
    (testing "Does Magyarlanc give the same result as the Mate-tools parser?"
      (= (->> sa magyarlanc first :dep)
         (->> sa dependency first)))
    (testing "Does Magyarlanc give the same result as the Berkley Tree Annotation parser?"
      (= (->> sa magyarlanc first :kons)
         (->> sa constituency first)))))


(deftest lemmatization-tests
  (testing "With words the lemmatization"
    (= "bagoly" (lemma-for-word "baglyokat"))
    (= "teremt" (lemma-for-word "teremté"))))



