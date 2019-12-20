(ns wxbridge.core
  (:import [org.apache.commons.daemon Daemon DaemonContext])
  (:require
   #_[ring.adapter.jetty :as ra]
   #_[ring.middleware.file :as rmf :refer [wrap-file]]
   #_[ring.middleware.resource :as rmr :refer [wrap-resource]]
   [clojure.core.async :as async :refer [go >! <!! chan]]
   #_[clojure.tools.logging :as log]
   [taoensso.timbre :as timbre :refer [info]]
   [ring.util.response :as resp]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [ring.middleware.resource :refer [wrap-resource]]
   [compojure.core :refer [defroutes GET]]
   [compojure.route :as route]
   [org.httpkit.client :as http]
   [org.httpkit.server :as svr]
   [wxbridge.conf :as conf :refer [appid]])
  (:gen-class
   :implements [org.apache.commons.daemon.Daemon]))

;; state is map
;; {:verbose true|false :server fn to stop server}
(defonce state (atom {}))

(defonce wxurl "https://api.openweathermap.org/data/2.5/forecast")

(defonce options {:timeout 1500
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
      (when (:verbose @state)
        (info "query:" city "status:" (:status resp)))
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

(defn log-requests [handler]
  (fn [request]
    (when (:verbose @state)
      (info (:remote-addr request) ":" (:uri request)))
    (handler request)))

(def site (log-requests (wrap-defaults (beef-up-app app) site-defaults)))

(defn init [args]
  (let [verbose (= (first args) "-v")]
    (swap! state assoc :verbose verbose)))

(defn stop []
  ;; :server slot of state holds the stop function returned by start
  (info "Shutting down server")
  ((:server @state)))

(defn start []
  (let [port 3033]
    (info "wxbridge: started on port " port "verbose: " (:verbose @state))
    (swap! state assoc :server (svr/run-server site {:port port}))))

;; daemonize from Clojure Cookbook

(defn -init [this ^DaemonContext context]
  (init (.getArguments context)))

(defn -start [this]
  (start))

(defn -stop [this]
  (stop))

(defn -destroy [this])

;; Deployment info: https://www.http-kit.org/server.html
;; Also see for hotcode reloadable setup: 
;; https://www.http-kit.org/migration.html

; -v flags verbose output

(defn -main
  [& args]
  (info "args" args)
  (init args)
  
  (start)
  #_(ra/run-jetty rts {:port 3000}))




