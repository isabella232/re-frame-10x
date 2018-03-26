(ns day8.re-frame-10x.view.event
  (:require [day8.re-frame-10x.utils.re-com :as rc]
            [day8.re-frame-10x.view.components :as components]
            [day8.re-frame-10x.common-styles :as common]
            [mranderson047.garden.v1v3v3.garden.units :as units]
            [mranderson047.reagent.v0v7v0.reagent.core :as reagent]
            [mranderson047.re-frame.v0v10v2.re-frame.core :as rf]
            [zprint.core :as zp]
            [goog.string]
            [clojure.string :as str])
  (:require-macros [day8.re-frame-10x.utils.macros :refer [with-cljs-devtools-prefs]]
                   [day8.re-frame-10x.utils.re-com :refer [handler-fn]]))

(def code-border (str "1px solid " common/white-background-border-color))


(def event-styles
  [:#--re-frame-10x--
   [:.event-panel
    {:padding "19px 19px 0px 0px"}]
   [:.bold {:font-weight "bold"}]
   [:.event-section]
   [:.event-section--header
    {:background-color common/navbar-tint-lighter
     :color            common/navbar-text-color
     :height           common/gs-19
     :font-size        "14px"
     :padding          [[0 common/gs-12]]
     }]
   [:.event-section--data
    {:background-color "rgba(100, 255, 100, 0.08)"
     :padding-left     (units/px- common/gs-12 common/expansion-button-horizontal-padding)
     :overflow-x       "auto"}]
   ])


;; Terminology:
;; Form: a single Clojure form (may have nested children)
;; Result: the result of execution of a single form
;; Fragment: the combination of a form and result
;; Listing: a block of traced Clojure code, e.g. an event handler function


(defn no-event-instructions
  []
  [rc/v-box
   :children [[rc/p {:style {:font-style "italic"}} "Code trace is not currently available for this event"]
              [:br]
              [rc/hyperlink-href
               :label  "Instructions for enabling Event Code Tracing"
               :attr   {:rel "noopener noreferrer"}
               :target "_blank"
               :href   "https://github.com/Day8/re-frame-10x/blob/master/docs/HyperlinkedInformation/EventCodeTracing.md"]]])


(defn code-header
  [code-execution-id line]
  ;(println ">>>>>> code-header:" (:id line))
  (let [open?-path [@(rf/subscribe [:epochs/current-epoch-id]) code-execution-id (:id line)]
        open?      (get-in @(rf/subscribe [:code/code-open?]) open?-path)]
    [rc/h-box
     :style {:border   code-border
             :overflow "hidden"
             :padding  "1px 6px"}
     :children [[rc/box
                 :width  "17px"
                 :height "17px"
                 :class  "noselect"
                 :style  {:cursor "pointer"
                          :color  "#b0b2b4"}
                 :attr   {:on-click (handler-fn (rf/dispatch [:code/set-code-visibility open?-path (not open?)]))}
                 :child  [rc/box
                          :margin "auto"
                          :child  [:span.arrow (if open? "▼" "▶")]]]
                [:pre
                 {:style {:margin-left "2px"
                          :margin-top  "2px"}}
                 (str (:form line))]]]))


(defn code-block
  [code-execution-id line]
  ;(println ">>>>>> code-block:" (:id line))
  [rc/box
   :style {:background-color "rgba(100, 255, 100, 0.08)"
           :border           code-border
           :margin-top       "-1px"
           :overflow-x       "auto"
           :overflow-y       "hidden"
           :padding          "0px 3px"}
   :child [components/simple-render (:result line) [@(rf/subscribe [:epochs/current-epoch-id]) code-execution-id (:id line)]]])

(defn find-bounds
  "Try and find the bounds of the form we are searching for. Uses some heuristics to
  try and avoid matching partial forms, e.g. 'default-|weeks| for the form 'weeks."
  [form-str search-str]
  (let [re         (re-pattern (str "(\\s|\\(|\\[|\\{)" "(" (goog.string.regExpEscape search-str) ")"))
        result     (.exec re form-str)]
    (if (some? result)
      (let [index        (.-index result)
            pre-match    (aget result 1)
            matched-form (aget result 2)
            index        (+ index (count pre-match))]
        [index (+ index (count matched-form))])
      ;; If the regex fails, fall back to string index just in case.
      (let [start  (str/index-of form-str search-str)
            length (if (and (some? search-str) (some? start))
                     (count (pr-str search-str))
                     0)]
        [start (+ start length)]))))

(defn event-expression
  []
  (let [scroll-pos (rf/subscribe [:code/scroll-pos])]
    (reagent/create-class
      {:component-did-update
       (fn event-expression-component-did-update [this]
         (let [node (reagent/dom-node this)]
           (set! (.-scrollTop node) (:top @scroll-pos))
           (set! (.-scrollLeft node) (:left @scroll-pos))))

       :display-name
       "event-expression"

       :reagent-render
       (fn
         []
         (let [highlighted-form @(rf/subscribe [:code/highlighted-form])
               form-str         @(rf/subscribe [:code/current-zprint-form])
               [start-index end-index] (find-bounds form-str (zp/zprint-str highlighted-form))
               before           (subs form-str 0 start-index)
               highlight        (subs form-str start-index end-index)
               after            (subs form-str end-index)]
           ;(println ">> event-expression:" (pr-str (subs (pr-str highlighted-form) 0 30)))
           ; DC: We get lots of React errors if we don't force a creation of a new element when the highlight changes. Not really sure why...
           ^{:key (pr-str highlighted-form)}
           [rc/box
            :style {:max-height       (str (* 10 17) "px")  ;; Add scrollbar after 10 lines
                    :overflow         "auto"
                    :border           "1px solid #e3e9ed"
                    :background-color common/white-background-color}
            :attr {:on-scroll (handler-fn (rf/dispatch [:code/save-scroll-pos (-> event .-target .-scrollTop) (-> event .-target .-scrollLeft)]))}
            :child (if (some? highlighted-form)
                     [components/highlight {:language "clojure"}
                      (list ^{:key "before"} before
                            ^{:key "hl"} [:span.code-listing--highlighted highlight]
                            ^{:key "after"} after)]
                     [components/highlight {:language "clojure"}
                      form-str])]))})))


(defn event-fragments
  [fragments code-exec-id]
  ;(println ">> event-fragments - count:" (count fragments))
  (let [code-open? @(rf/subscribe [:code/code-open?])]
    [rc/v-box
     :size     "1"
     :style    {:overflow-y "auto"}
     :children (doall
                 (for [frag fragments]
                   (let [id (:id frag)]
                     ^{:key id}
                     [rc/v-box
                      :class    "code-fragment"
                      :style    {:margin-left (str (* 9 (dec (:indent-level frag))) "px")
                                 :margin-top  (when (pos? id) "-1px")}
                      :attr     {:on-mouse-enter (handler-fn #_(println "OVER:" (:id frag)) (rf/dispatch [:code/hover-form (:form frag)]))
                                 :on-mouse-leave (handler-fn #_(println " OUT:" (:id frag)) (rf/dispatch [:code/exit-hover-form (:form frag)]))}
                      :children [[code-header code-exec-id frag]
                                 (when (get-in code-open? [@(rf/subscribe [:epochs/current-epoch-id]) code-exec-id id])
                                   [code-block code-exec-id frag id])]])))]))


(defn event-code
  []
  (let [code-traces      @(rf/subscribe [:code/current-code])
        code-execution   (first code-traces) ;; Ignore multiple code executions for now
        highlighted-form (rf/subscribe [:code/highlighted-form])
        #_#_debug?           @(rf/subscribe [:settings/debug?])]
    ;(println "EVENT-CODE")
    (if-not code-execution
      [no-event-instructions]
      [rc/v-box
       :size "1 1 auto"
       :class "code-panel"
       :children [#_(when debug? [:pre "Hover " (pr-str @highlighted-form) "\n"])
                  [event-expression]
                  [rc/gap-f :size common/gs-19s]
                  [event-fragments (->> (:code code-execution)
                                        (remove (fn [line] (fn? (:result line)))))
                   (:id code-execution)]]])))


(defn render []
  (let [epoch-id @(rf/subscribe [:epochs/current-match-state])]
    ;; Create a new id on each panel because Reagent can throw an exception if
    ;; the data provided in successive renders is sufficiently different.
    ^{:key epoch-id}
    [rc/v-box
     :size     "1"
     :class    "event-panel"
     ;:style    {:margin-right common/gs-19s}
     :gap      common/gs-19s
     :children [[event-code]
                [rc/gap-f :size "0px"]]]))
