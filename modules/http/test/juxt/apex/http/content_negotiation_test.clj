;; Copyright © 2020, JUXT LTD.

(ns juxt.apex.http.content-negotiation-test
  (:require
   [clojure.test :refer [deftest is are]]
   [juxt.apex.alpha.http.content-negotiation
    :refer [acceptable-media-type-qvalue select-most-acceptable-representation select-acceptable-representations]]
   [juxt.reap.alpha.api :as reap]
   [ring.mock.request :refer [request]]))

;; This test represents the table in RFC 7231 Section 5.3.2, where quality
;; values are determined from matching a variant's content-type according to
;; rules of precedence. These rules are specified in the RFC and are independent
;; of the actual content negotiation algorithm that is used.
(deftest acceptable-media-type-test

  (let [accepts
        (reap/accept "text/*;q=0.3, text/html;q=0.7, text/html;level=1, text/html;level=2;q=0.4, */*;q=0.5")]

    (are [content-type expected]
        (= expected
           (acceptable-media-type-qvalue
            {:apex.http/content-type content-type}
            accepts))

        "text/html;level=1" 1.0
        "text/html" 0.7
        "text/plain" 0.3
        "image/jpeg" 0.5
        "text/html;level=2" 0.4
        "text/html;level=3" 0.7)))

#_(select-most-acceptable-representation
 (-> (request :get "/hello")
     (update
      :headers conj
      ["accept" "text/html;q=0.8,text/plain"]))

 [{:id :html
   :apex.http/content "<h1>Hello World!</h1>"
   :apex.http/content-type "text/html;charset=utf-8"}

  {:id :html-level-2
   :apex.http/content "<h1>Hello World!</h1>"
   :apex.http/content-type "text/html;level=2;charset=utf-8"}

  {:id :plain-text
   :apex.http/content "Hello World!"
   :apex.http/content-type "text/plain;charset=utf-8"}])


(deftest select-most-acceptable-representation-test

  (are [accept-header expected-content]
      (= expected-content
         (:id
          (select-most-acceptable-representation
           (-> (request :get "/hello")
               (update
                :headers conj
                ["accept" accept-header]))

           [{:id :html
             :apex.http/content "<h1>Hello World!</h1>"
             :apex.http/content-type "text/html;charset=utf-8"}

            {:id :html-level-2
             :apex.http/content "<h1>Hello World!</h1>"
             :apex.http/content-type "text/html;level=2;charset=utf-8"}

            {:id :plain-text
             :apex.http/content "Hello World!"
             :apex.http/content-type "text/plain;charset=utf-8"}])))

    "text/html" :html
    "TEXT/HTML" :html

    "text/html;q=0.8,text/plain;q=0.7" :html
    "text/plain" :plain-text
    "text/html;q=0.8,text/plain" :plain-text

    "TEXT/HTML;level=2" :html-level-2
    ))

;; TODO: Test quality-of-source
