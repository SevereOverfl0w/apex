;; Copyright © 2020, JUXT LTD.

(ns juxt.apex.examples.cms.router
  (:require
   [juxt.apex.examples.cms.cache :as cache]
   [juxt.apex.examples.cms.flowables :as flowables]
   [juxt.apex.examples.cms.upload :as upload]
   [juxt.apex.examples.cms.rs :as rs]
   [juxt.apex.examples.cms.sse :as sse]
   [integrant.core :as ig]))

(defn make-router [cms-router]
  (let [flowables-example-handler (flowables/flow-example {})]
    (fn [req respond raise]
      (condp re-matches (:uri req)

        #"/upload-file"
        (upload/upload-file-example req respond raise)

        #"/flow"
        (flowables-example-handler req respond raise)

        #"/bp"
        (rs/backpressure-example req respond raise)

        #"/cache-example"
        (cache/cache-example req respond raise)

        ;; SSE
        #"/sse" (sse/sse-example req respond raise)

        #"/ticker" (flowables/ticker-example req respond raise)

        (cms-router req respond raise)))))

(defmethod ig/init-key ::router [_ {:keys [cms-router]}]
  (assert cms-router)
  (make-router cms-router))
