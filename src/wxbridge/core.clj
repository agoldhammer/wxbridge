(ns wxbridge.core
  (:require
   #_[ring.adapter.jetty :as ra]
   #_[ring.middleware.file :as rmf :refer [wrap-file]]
   #_[ring.middleware.resource :as rmr :refer [wrap-resource]]
   [clojure.core.async :as async :refer [go >! <!! chan]]
   [clojure.tools.logging :as log]
   [ring.util.response :as resp]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
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

(defn make-options [city]
  (assoc-in options [:query-params "q"] city))

(defn get-city-data [city channel]
  (http/get wxurl (make-options city)
            (fn [resp] (go (>! channel (dissoc resp :opts))))
            #_(fn [{:keys [status headers body error]}] ;; asynchronous response handling
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

(defn handle-city [city]
  (let [out-chan (chan)]
    (get-city-data city out-chan)
    (let [resp (<!! out-chan)]
      #_(prn resp)
      (log/info "wxbridge: query:" city "status:" (:status resp))
      {:status (:status resp)
       :headers {"Content-Type" "application/json; charset=utf-8"
                 "Connection" "keep-alive"
                 "Access-Control-Allow-Origin" "*"}
       :body (:body resp)})))

(defroutes app
  #_(GET "/" [] (resp/resource-response "index.html"))
  (GET "/" [] (resp/resource-response "index.html" {:root "public"}))
  (GET "/wx/:city" [city] (handle-city city))
  (route/resources "/")
  (route/files "public")
  (route/not-found "Weather info not found"))

#_(defn app [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    "hello HTTP!"})

(def site (wrap-defaults app site-defaults))

(defn -main
  []
  (let [port 3033]
    (log/info "wxbridge: started on port " port)
    (svr/run-server site {:port port}))
  #_(ra/run-jetty rts {:port 3000}))

;; -------------------------------------
(comment
  @(http/get wxurl options
             (fn [{:keys [status _headers _body error]}] ;; asynchronous response handling
               (if error
                 (println "Failed, exception is " error)
                 (println "Async HTTP GET: " status)))))



