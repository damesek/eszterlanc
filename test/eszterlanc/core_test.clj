(ns eszterlanc.core-test
  (:require [clojure.test :refer :all]
            [eszterlanc.core :refer :all]))


;; todo: nil case, different type, single sentence

(def test-sentences
  "Kezdetben teremté Isten az eget és a földet. A föld pedig kietlen és puszta vala,
  és setétség vala a mélység színén, és az Isten Lelke lebeg vala a vizek felett.")


(deftest eszterlanc-core-tests
  (let [sa (sentence-array test-sentences)]
    (testing "Test sentence-array we get java object"
      (is (= (Class/forName "[[Ljava.lang.String;")
             (type sa))))
    (testing "With object->clj we get the right format"
      (is (= "Kezdetben"
             (->> sa object->clj ffirst))))))


(deftest parser-tests
  (let [sa (sentence-array test-sentences)]
    (testing "With morphological parser, array version"
      (is (= ["Kezdetben" "kezdet" "NOUN" "Case=Ine|Number=Sing"]
             (->> sa morph-array ffirst))))
    (testing "With Mate-tools Natural language analysis"
      (is (= ["1" "Kezdetben" "kezdet" "NOUN" "Case=Ine|Number=Sing" "2" "OBL"]
             (->> sa dependency ffirst))))
    (testing "With Berkley Tree Annotation parser"
      (is (= ["Kezdetben" "kezdet" "NOUN" "Case=Ine|Number=Sing" "(ROOT(CP(NP*)"]
             (->> sa constituency ffirst))))))

(deftest magyarlanc-tests
  (let [sa (sentence-array test-sentences)]
    (testing "With magyarlanc, could we get back all functionality?"
      (is (= '(:morph :dep :kons)
             (->> sa magyarlanc first keys))))
    (testing "Does Magyarlanc give the same result as the morphological parser?"
      (is (= (->> sa magyarlanc first :morph)
             (->> sa morph-array first))))
    (testing "Does Magyarlanc give the same result as the Mate-tools parser?"
      (is (= (->> sa magyarlanc first :dep)
             (->> sa dependency first))))
    (testing "Does Magyarlanc give the same result as the Berkley Tree Annotation parser?"
      (is (= (->> sa magyarlanc first :kons)
             (->> sa constituency first))))))



(deftest lemmatization-tests
  (testing "With words the lemmatization"
    (is (= "bagoly" (lemma-for-word "baglyokat")))
    (is (= "terem" (lemma-for-word "teremté")))))



