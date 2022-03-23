(ns pva-radio-be.handler
  (:import (java.net InetAddress))
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults site-defaults]]
            [ring.middleware.cors :refer [wrap-cors]]
            [clojure.data.json :as json]
            [clj-http.client :as client]
            [clojure.java.shell :refer [sh with-sh-dir]]
            [clojure.string :as s]
            [clojure.java.jdbc :refer :all]
            [ring.adapter.jetty :as ring-jetty])
  (:gen-class))

(def db
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     "database.db"})

(defn now
  []
  (.format (new java.text.SimpleDateFormat "yyyy-MM-dd HH:mm:ss") (java.util.Date.)))

(defn init
  []
  (execute! db ["create table stations (uuid text primary key, url text, name text, icon text, update_date text);"])
  (execute! db ["create table favorites (uuid text primary key, score integer);"])
  (execute! db ["create table history (time text primary key, uuid text);"])
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body  "DB initialized"})

(defn get-hostname
  []
  (rand-nth
   (map (fn [addr] (.getCanonicalHostName addr))
        (InetAddress/getAllByName "all.api.radio-browser.info"))))

(defn station>db
  [s]
  {:uuid (get s "stationuuid")
   :url  (get s "url")
   :name (get s "name")
   :icon (get s "favicon" s)})

(defn call-radio-browser
  [suffix]
  (let [host (get-hostname)
        stations (->> (client/get (str "https://" host "/json/stations/" suffix "?order=clickcount&reverse=true&limit=20")
                                  {:headers {"User-Agent" "pva-radio/0.1"}})
                      :body
                      json/read-str
                      (take 20)
                      (map station>db))]
    (doall
     (map (fn [s]
            (let [{:keys [uuid name url icon]} s]
              (execute! db ["insert into stations(uuid, name, url, icon, update_date) 
values (?,?,?,?,?) on conflict(uuid) do update 
set name=excluded.name, 
    url=excluded.url, 
    icon=excluded.icon, 
    update_date=excluded.update_date"
                            uuid name url icon (now)])))
          stations))
    stations))

(defn search
  [term]
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body  (json/write-str
             {:byname (call-radio-browser (str "byname/" term))
              :bytag  (call-radio-browser (str "bytag/" term))})})

(defn- dosh
  ([command]
   (let [cmd-result (apply sh (s/split command  #" "))]
    (println (:out cmd-result))
    (println (:err cmd-result))
    cmd-result))
  ([dir command]
   (with-sh-dir dir (dosh command))))

(defn on-windows?
  []
  (s/includes? (s/lower-case (System/getProperty "os.name")) "windows"))

(defn kill
  []
  (if (on-windows?)
    (dosh "taskkill /IM \"vlc.exe\" /F")
    (dosh "killall vlc"))
  {:status 200
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body (json/write-str {:Holly "They're all dead Dave."})})

(defn play
  [uuid]
  (kill)
  (let [url   (-> (client/get (str "https://" (get-hostname) "/json/url/" uuid)
                              {:headers {"User-Agent" "pva-radio/0.1"}})
                  :body
                  json/read-str
                  (get "url"))]
    (execute! db ["insert into history(time, uuid) values (?,?);" (now) uuid])
    (if (on-windows?)
      (.start (Thread. (fn [] (dosh (str "vlc --intf dummy " url)))))
      (.start (Thread. (fn [] (dosh (str "cvlc " url)))))))
  {:status 200
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body (json/write-str {:playing uuid})})

(comment
  (play "961e622e-0601-11e8-ae97-52543be04c81")
  (kill)
  )

(defn list-stored
  []
  (let [rs (query db ["select * from stations"])]
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body (json/write-str rs)}))

(defn history
  []
  (let [rs (query db ["select distinct st.* from stations st join history hs on hs.uuid = st.uuid order by hs.time desc limit 20;"])]
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body (json/write-str rs)}))

(defn favorites
  []
  (let [rs (query db ["select distinct st.* from stations st join favorites fv on fv.uuid = st.uuid order by fv.score desc limit 20;"])]
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body (json/write-str rs)}))

(defn voteup
  [uuid]
  (execute! db ["insert into favorites(uuid, score) values (?,?) on conflict(uuid) do update set score=score+1;" uuid 1]))

(defn remove-fav
  [uuid]
  (execute! db ["delete from favorites where uuid = ?;" uuid]))

(defroutes api-routes
  (GET "/api/search/:term" [term] (search term))
  (GET "/api/play/:uuid" [uuid] (play uuid))
  (GET "/api/list-stored" [] (list-stored));;todo remove
  (GET "/api/history" [] (history))
  (GET "/api/favorites" [] (favorites))
  (GET "/api/voteup/:uuid" [uuid] (voteup uuid))
  (GET "/api/remove/:uuid" [uuid] (remove-fav uuid))
  (GET "/api/init-db" [] (init))
  (GET "/api/kill" [] (kill)))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (routes
   (-> api-routes
              (wrap-routes wrap-defaults api-defaults)
              (wrap-cors :access-control-allow-origin [#".*"]
                         :access-control-allow-methods [:get :post]))
   (-> app-routes (wrap-routes wrap-defaults site-defaults))))

(defn string->number [str default]
  (if (s/blank? str)
    default
    (let [n (read-string str)]
      (if (number? n) n default))))

(defn -main [& args]
  (let [port (string->number (first args) 8080)]
    (ring-jetty/run-jetty app {:port port})))
