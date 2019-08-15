(ns juxt.warp.request
  (:require
   [muuntaja.middleware :as mw]))

(defn wrap-catch-negotiate-error [h]
  (fn [req respond raise]
    (try
      (h req respond raise)
      (catch clojure.lang.ExceptionInfo e
        (if (#{:muuntaja/response-format-negotiation} (:type (ex-data e)))
          (respond {:status 406
                    :body "Not Acceptable"})
          (raise e))))))

(defn wrap-oas-path [h api]
  (fn [req respond raise]
    (h (merge req {:oas/api api}) respond raise)))

(defn path-map [req api]
  (let [url (format "%s://%s%s" (name (:scheme req)) (:server-name req) (:uri req))
        servers (->> (get-in api ["servers"]) (map #(get % "url")))
        path (some #(when (.startsWith url %) (subs url (count %))) servers)
        path-item (get-in api ["paths" path])]
    {:oas/url url
     :oas/servers servers
     :oas/path path
     :oas/path-item path-item}))

(defn wrap-check-404 [h api]
  (fn [req respond raise]
    (let [{:keys [:oas/path :oas/path-item] :as path-map} (path-map req api)]
      (cond
        ;; Not served by server in the 'servers' section
        ;; Or not found in the 'paths' section
        (or (nil? path) (nil? path-item))
        (respond {:status 404 :body "Not Found"})
        :else (h (merge req path-map) respond raise)))))

(defn wrap-check-405 [h]
  (fn [req respond raise]
    (let [path-item (:oas/path-item req)
          methods (set (keys path-item))]
      (cond
        (not (contains? methods (name (:request-method req))))
        (respond {:status 405 :body "Method Not Allowed"})
        :else (h (merge req {:oas/methods methods}) respond raise)))))

(defn wrap-properties [h options]
  (fn [req respond raise]
    (if-let [f (:properties-fn options)]
      (f (fn [result]
                 (h (assoc req ::value result) respond raise)))
      (h req respond raise))))

;; The rule for composing 3-arity Ring middleware is as follows:
;; If you want to call the delegate, simply pass the respond and raise functions along.
;; If you want to return a response map, call the respond function with your response map.
;; If you want to throw an exception, call the raise function with your exception.

(defn handler [api options]
  (->
   (fn [req respond raise]
     (respond {:status 200
               :body (if-let [v (::value req)]
                       {:message (format "OK, value is '%s'" (::value req))}
                       {:message "OK"})}))

   ;; Having determined the status code, we can now do pro-active
   ;; content negotiation since the available content types are a
   ;; function of the status code (in OpenAPI).
   (mw/wrap-format (dissoc muuntaja.core/default-options :default-format))
   wrap-catch-negotiate-error

   ;; Get the resource's properties
   (wrap-properties options)

   wrap-check-405
   (wrap-check-404 api)
   (wrap-oas-path api)))


#_{["findPets" "200"]
 (fn [content-type]
   "Hello World!"
   )}


;; Create a general handler that can be put in Ring, manifold or Pedestal
#_(make-handler [open-api-desc attachments]

              )
