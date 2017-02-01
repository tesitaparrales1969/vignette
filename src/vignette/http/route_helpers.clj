(ns vignette.http.route-helpers
  (:require [vignette.util.external-hotlinking :refer [original-request->file]]
            [vignette.util.image-response :refer :all]
            [vignette.util.query-options :refer :all]
            [vignette.protocols :refer :all]
            [vignette.storage.core :refer :all]
            [vignette.storage.protocols :refer :all]
            [vignette.util.thumbnail :as u]))

(def blocked-placeholder-param "bp")

(defn handle-thumbnail
  [store image-params request]
  (if-let [thumb (u/get-or-generate-thumbnail store image-params)]
    (create-image-response thumb image-params)
    (error-response 404 image-params)))

(defn handle-original
  [store image-params request]
  (if-let [file (original-request->file request store image-params)]
    (create-image-response file image-params)
    (error-response 404 image-params)))

(defn handle-head
  [store image-params]
  (if (original-exists? store image-params)
    (create-head-response image-params)
    (error-response 404 image-params)))

(defn route-params->image-type
  [route-params]
  (if (clojure.string/blank? (:image-type route-params))
    "images"
    (clojure.string/replace (:image-type route-params)
                            #"^\/(.*)"
                            "$1")))

(defn route->requested-format
  "Extract information about the format being explicitely specified in the url"
  [request-map request]
  (assoc request-map :requested-format (:format (extract-query-opts request))))

(defn route->options
  "Extracts the query options and moves them to 'request-map'"
  [request-map request]
  (assoc request-map :options (autodetect-request-format request (extract-query-opts request))))

(defn route->image-type
  [request-map]
  (assoc request-map :image-type (route-params->image-type request-map)))

(defn route->blocked-placeholder
  [request-map request]
  (if-let [params (:query-params request)]
    (if-let [placeholder-id (params blocked-placeholder-param)]
      (assoc request-map :blocked-placeholder placeholder-id)
      request-map)
    request-map))

(defn route->original-map
  [request-map request]
  (-> request-map
      (assoc :request-type :original)
      (route->image-type)
      (route->requested-format request)
      (route->options request)
      (route->blocked-placeholder request)))

(defn route->thumbnail-map
  [request-map request &[options]]
  (-> request-map
      (assoc :request-type :thumbnail)
      (route->image-type)
      (route->requested-format request)
      (route->options request)
      (route->blocked-placeholder request)
      (cond->
        options (merge options))))

(defn route->thumbnail-auto-height-map
  [request-map request]
  (route->thumbnail-map request-map request {:height :auto}))

(defn route->thumbnail-auto-width-map
  [request-map request]
  (route->thumbnail-map request-map request {:width :auto}))
