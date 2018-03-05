(ns day8.re-frame-10x
  (:require [re-frame.trace :as trace :include-macros true]
            [clojure.string :as str]
            [reagent.interop :refer-macros [$ $!]]
            [reagent.impl.util :as util]
            [reagent.impl.component :as component]
            [reagent.impl.batching :as batch]
            [reagent.ratom :as ratom]
            [goog.object :as gob]
            [re-frame.interop :as interop]
            [mranderson047.re-frame.v0v10v2.re-frame.core :as rf]
            [mranderson047.reagent.v0v7v0.reagent.core :as r]))

(goog-define debug? false)

;; from https://github.com/reagent-project/reagent/blob/3fd0f1b1d8f43dbf169d136f0f905030d7e093bd/src/reagent/impl/component.cljs#L274
(defn fiber-component-path [fiber]
  (let [name   (some-> fiber
                       ($ :type)
                       ($ :displayName))
        parent (some-> fiber
                       ($ :return))
        path   (some-> parent
                       fiber-component-path
                       (str " > "))
        res    (str path name)]
    (when-not (empty? res) res)))

(defn component-path [c]
  ;; Alternative branch for React 16
  (if-let [fiber (some-> c ($ :_reactInternalFiber))]
    (fiber-component-path fiber)
    (component/component-path c)))

(defn comp-name [c]
  (let [n (or (component-path c)
              (some-> c .-constructor util/fun-name))]
    (if-not (empty? n)
      n
      "")))

(def static-fns
  {:render
   (fn mp-render []                                         ;; Monkeypatched render
     (this-as c
       (trace/with-trace {:op-type   :render
                          :tags      {:component-path (component-path c)}
                          :operation (last (str/split (component-path c) #" > "))}
                         (if util/*non-reactive*
                           (reagent.impl.component/do-render c)
                           (let [rat        ($ c :cljsRatom)
                                 _          (batch/mark-rendered c)
                                 res        (if (nil? rat)
                                              (ratom/run-in-reaction #(reagent.impl.component/do-render c) c "cljsRatom"
                                                                     batch/queue-render reagent.impl.component/rat-opts)
                                              (._run rat false))
                                 cljs-ratom ($ c :cljsRatom)] ;; actually a reaction
                             (trace/merge-trace!
                               {:tags {:reaction      (interop/reagent-id cljs-ratom)
                                       :input-signals (when cljs-ratom
                                                        (map interop/reagent-id (gob/get cljs-ratom "watching" :none)))}})
                             res)))))})


(defonce real-custom-wrapper reagent.impl.component/custom-wrapper)
(defonce real-next-tick reagent.impl.batching/next-tick)
(defonce real-schedule reagent.impl.batching/schedule)
(defonce do-after-render-trace-scheduled? (atom false))

(defn monkey-patch-reagent []
  (let [#_#_real-renderer reagent.impl.component/do-render
        ]


    #_(set! reagent.impl.component/do-render
            (fn [c]
              (let [name (comp-name c)]
                (js/console.log c)
                (trace/with-trace {:op-type   :render
                                   :tags      {:component-path (component-path c)}
                                   :operation (last (str/split name #" > "))}
                                  (real-renderer c)))))

    (set! reagent.impl.component/static-fns static-fns)

    (set! reagent.impl.component/custom-wrapper
          (fn [key f]
            (case key
              :componentWillUnmount
              (fn [] (this-as c
                       (trace/with-trace {:op-type   key
                                          :operation (last (str/split (comp-name c) #" > "))
                                          :tags      {:component-path (component-path c)
                                                      :reaction       (interop/reagent-id ($ c :cljsRatom))}})
                       (.call (real-custom-wrapper key f) c c)))

              (real-custom-wrapper key f))))

    (set! reagent.impl.batching/next-tick
          (fn [f]
            ;; Schedule a trace to be emitted after a render if there is nothing else scheduled after that render.
            ;; This signals the end of the epoch.

            #_(swap! do-after-render-trace-scheduled?
                     (fn [scheduled?]
                       (js/console.log "Setting up scheduled after" scheduled?)
                       (if scheduled?
                         scheduled?
                         (do (reagent.impl.batching/do-after-render ;; a do-after-flush would probably be a better spot to put this if it existed.
                               (fn []
                                 (js/console.log "Do after render" reagent.impl.batching/render-queue)
                                 (reset! do-after-render-trace-scheduled? false)
                                 (when (false? (.-scheduled? reagent.impl.batching/render-queue))
                                   (trace/with-trace {:op-type :reagent/quiescent}))))
                             true))))
            (real-next-tick (fn []
                              (trace/with-trace {:op-type :raf}
                                                (f)
                                                (trace/with-trace {:op-type :raf-end})
                                                (when (false? (.-scheduled? reagent.impl.batching/render-queue))
                                                  (trace/with-trace {:op-type :reagent/quiescent}))

                                                )))))

    #_(set! reagent.impl.batching/schedule
            (fn []
              (reagent.impl.batching/do-after-render
                (fn []
                  (when @do-after-render-trace-scheduled?
                    (trace/with-trace {:op-type :do-after-render})
                    (reset! do-after-render-trace-scheduled? false))))
              (real-schedule)))))


(defn init-tracing!
  "Sets up any initial state that needs to be there for tracing. Does not enable tracing."
  []
  (monkey-patch-reagent))

(defn ^:export factory-reset! []
  (rf/dispatch [:settings/factory-reset]))
