(ns day8.re-frame-10x.preload
  (:require [day8.re-frame-10x :as trace]
            [mranderson047.re-frame.v0v10v2.re-frame.core :as rf]))


;; Use this namespace with the :preloads compiler option to perform the necessary setup for enabling tracing:
;; {:compiler {:preloads [day8.re-frame-10x.preload] ...}}
(rf/clear-subscription-cache!)
(defonce _ (trace/init-tracing!))
