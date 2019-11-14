(ns wxbridge.core
  (:require
   [ring.adapter.jetty :as ra]
   #_[ring.middleware.file :as rmf :refer [wrap-file]]
   #_[ring.middleware.resource :as rmr :refer [wrap-resource]]
   [ring.util.response :as resp]
   [compojure.core :refer [defroutes GET]]
   [compojure.route :as route]
   [org.httpkit.client :as http]
   [wxbridge.conf :as conf :refer [appid]])
  (:gen-class))


(def wxurl "https://api.openweathermap.org/data/2.5/forecast")

(def options {:timeout 1000
              :query-params {"units" "imperial"
                             "appid" appid}})

(def cities ["Boston" "Paris,FR"])

(def svrdb (atom {}))

(comment
    @(http/get wxurl options
              (fn [{:keys [status headers body error]}] ;; asynchronous response handling
                (if error
                  (println "Failed, exception is " error)
                  (println "Async HTTP GET: " status)))))

(defn make-options [city]
  (assoc-in options [:query-params "q"] city))

(defn get-city-data [city db]
  (http/get wxurl (make-options city)
            (fn [{:keys [status headers body error]}] ;; asynchronous response handling
              (prn status)
              (if error
                (swap! db assoc city {:success false
                                      :error error})
                (swap! db assoc city {:success true
                                      :wxdata body})))))

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

#_(defn -main
  []
  (ra/run-jetty rts {:port 3000}))


