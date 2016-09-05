(ns spec-tools.perf-test
  (:require [clojure.spec :as spec]
            [schema.core :as schema]
            [schema.coerce :as coerce]
            [spec-tools.core :as st]
            [criterium.core :as cc]
            [clojure.spec :as s]))

;;
;; start repl with `lein perf repl`
;; perf measured with the following setup:
;;
;; Model Name:            MacBook Pro
;; Model Identifier:      MacBookPro11,3
;; Processor Name:        Intel Core i7
;; Processor Speed:       2,5 GHz
;; Number of Processors:  1
;; Total Number of Cores: 4
;; L2 Cache (per Core):   256 KB
;; L3 Cache:              6 MB
;; Memory:                16 GB
;;

(spec/def ::age (spec/and integer? #(> % 10)))
(spec/def ::x-age (spec/and st/integer? #(> % 10)))

(def age (schema/constrained schema/Int #(> % 10)))

(defn raw-title [color s]
  (println (str color (apply str (repeat (count s) "#")) "\u001B[0m"))
  (println (str color s "\u001B[0m"))
  (println (str color (apply str (repeat (count s) "#")) "\u001B[0m")))

(def title (partial raw-title "\u001B[35m"))
(def suite (partial raw-title "\u001B[32m"))

(schema/check age 12)

(defn valid-test []

  (suite "valid?")

  ; 1260ns
  (title "spec: integer?")
  (let [call #(spec/valid? ::age 12)]
    (assert (call))
    (cc/quick-bench
      (call)))

  ; 1480ns
  (title "spec: x-integer?")
  (let [call #(spec/valid? ::x-age 12)]
    (assert (call))
    (cc/quick-bench
      (call)))

  ; 430ns
  (title "schema: s/Int")
  (let [call #(schema/check age 12)]
    (assert (nil? (call)))
    (cc/quick-bench
      (call)))

  ; 31ns
  (title "schema: s/Int (compiled)")
  (let [checker (schema/checker age)
        call #(checker 12)]
    (assert (nil? (call)))
    (cc/quick-bench
      (call))))

(defn conform-test []

  (suite "no-op conform")

  ; 1315ns
  (title "spec: integer?")
  (let [call #(spec/conform ::age 12)]
    (assert (= (call) 12))
    (cc/quick-bench
      (call)))

  ; 1430ns
  (title "spec: x-integer?")
  (let [call #(st/conform ::x-age 12)]
    (assert (= (call) 12))
    (cc/quick-bench
      (call)))

  ; 452ns
  (title "schema: s/Int")
  (let [call #((coerce/coercer age (constantly nil)) 12)]
    (assert (= (call) 12))
    (cc/quick-bench
      (call)))

  ; 27ns
  (title "schema: s/Int (compiled)")
  (let [coercer (coerce/coercer age (constantly nil))
        call #(coercer 12)]
    (assert (= (call) 12))
    (cc/quick-bench
      (call))))

(defn conform-test2 []

  (suite "conforming set of keywords")

  (let [sizes-spec (spec/coll-of (spec/and st/keyword? #{:L :M :S}) :into #{})
        sizes-schema #{(schema/enum :L :M :S)}]

    ; 4300ns
    (title "spec: conform keyword enum - no-op")
    (let [call #(st/conform sizes-spec ["L" "M"] st/string-conformations)]
      (assert (= (call) #{:L :M}))
      (cc/quick-bench
        (call)))

    ; 3700ns
    (title "spec: conform keyword enum")
    (let [call #(st/conform sizes-spec #{:L :M} st/string-conformations)]
      (assert (= (call) #{:L :M}))
      (cc/quick-bench
        (call)))

    ; 890ns
    (title "schema: conform keyword enum - no-op")
    (let [coercer (coerce/coercer sizes-schema coerce/string-coercion-matcher)
          call #(coercer #{:L :M})]
      (assert (= (call) #{:L :M}))
      (cc/quick-bench
        (call)))

    ; 1100ns
    (title "schema: conform keyword enum")
    (let [coercer (coerce/coercer sizes-schema coerce/string-coercion-matcher)
          call #(coercer ["L" "M"])]
      (assert (= (call) #{:L :M}))
      (cc/quick-bench
        (call)))))

(comment
  (valid-test)
  (conform-test)
  (conform-test2))