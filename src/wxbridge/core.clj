(ns wxbridge.core
  (:require
   #_[ring.adapter.jetty :as ra]
   #_[ring.middleware.file :as rmf :refer [wrap-file]]
   #_[ring.middleware.resource :as rmr :refer [wrap-resource]]
   [clojure.core.async :as async :refer [go >! <! chan]]
   [ring.util.response :as resp]
   [compojure.core :refer [defroutes GET]]
   [compojure.route :as route]
   [org.httpkit.client :as http]
   [org.httpkit.server :as svr]
   [wxbridge.conf :as conf :refer [appid]])
  (:gen-class))


(def wxurl "https://api.openweathermap.org/data/2.5/forecast")

(def options {:timeout 1500
              :query-params {"units" "imperial"
                             "appid" appid}})

(def categories {:test ["Boston" "Paris,FR", "Venice,IT"]})

(def svrdb (atom {}))

(def outch (chan))


(defn make-options [city]
  (assoc-in options [:query-params "q"] city))

(defn get-city-data [city channel]
  (http/get wxurl (make-options city)
            (fn [{:keys [status headers body error]}] ;; asynchronous response handling
              (prn "gcd" status headers)
              (if error
                (go (>! channel {city {:success false
                               :error error}}))
                (go (>! channel {city {:success true
                               :wxdata body}}))))))

#_(defn get-city-data [city db]
  (http/get wxurl (make-options city)
            (fn [{:keys [status headers body error]}] ;; asynchronous response handling
              (prn "gcd" headers)
              (if error
                (swap! db assoc city {:success false
                                      :error error})
                (swap! db assoc city {:success true
                                      :wxdata body})))))

(defn get-category-data [category db]
  (let [cities (get categories category)]
    (reset! svrdb {})
    (doseq [city cities]
      (get-city-data city db))))

#_(defn handle [city]
    (let [{:keys [status body]} (get-city-data city)]
    (prn "handle" status body)
    (if (= status 200)
      {:status 200
       :headers {"Content-Type" "application/json"
                 "Access-Control-Allow-Origin" "*"}
       :body body}
      {:status 404
       :headers {"Content-Type" "text/html"
                 "Access-Control-Allow-Origin" "*"}
       :body "Weather server failure"})))

#_(defroutes rts
  (GET "/" [] (resp/resource-response "index.html" {:root "public"}))
  (GET "/wx/:city" [city] (handle city))
  (route/resources "/")
  (route/files "public")
  (route/not-found "Weather info not found"))

(defn app [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    "hello HTTP!"})

(defn -main
  []
  (svr/run-server app {:port 7070})
  #_(ra/run-jetty rts {:port 3000}))

;; -------------------------------------
(comment
  @(http/get wxurl options
             (fn [{:keys [status headers body error]}] ;; asynchronous response handling
               (if error
                 (println "Failed, exception is " error)
                 (println "Async HTTP GET: " status)))))



