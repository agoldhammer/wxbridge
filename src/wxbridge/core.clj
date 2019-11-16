(ns wxbridge.core
  (:require
   #_[ring.adapter.jetty :as ra]
   #_[ring.middleware.file :as rmf :refer [wrap-file]]
   #_[ring.middleware.resource :as rmr :refer [wrap-resource]]
   [clojure.core.async :as async :refer [go >! <!! chan]]
   [clojure.tools.logging :as log]
   [ring.util.response :as resp]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [ring.middleware.resource :refer [wrap-resource]]
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
            (fn [resp] (go (>! channel (dissoc resp :opts))))))

(defn handle-city [city]
  (let [out-chan (chan)]
    (get-city-data city out-chan)
    (let [resp (<!! out-chan)]
      (log/info "wxbridge: query:" city "status:" (:status resp))
      {:status (:status resp)
       :headers {"Content-Type" "application/json; charset=utf-8"
                 "Connection" "keep-alive"
                 "Access-Control-Allow-Origin" "*"}
       :body (:body resp)})))

(defn wrap-text
  "changes response type on bare index call to text/html
to prevent file from being downloaded"
  [response]
  (assoc-in response [:headers "Content-Type"]
            "text/html; charset=utf-8"))

(defroutes app
  (GET "/" [] (wrap-text (resp/resource-response "public/index.html")))
  (GET "/wx/:city" [city] (handle-city city))
  (route/resources "/")
  (route/files "public")
  (route/not-found "Weather info not found"))

(defn beef-up-app
  [my-app]
  (wrap-resource my-app "public" {:allow-symlinks? true}))

(def site (wrap-defaults (beef-up-app app) site-defaults))

;; Deployment info: https://www.http-kit.org/server.html
;; Also see for hotcode reloadable setup: 
;; https://www.http-kit.org/migration.html

(defn -main
  []
  (let [port 3033]
    (log/info "wxbridge-beefed: started on port " port)
    (svr/run-server site {:port port}))
  #_(ra/run-jetty rts {:port 3000}))




