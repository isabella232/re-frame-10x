(defproject status-im/re-frame-10x "0.3.0-RN"
  :description "Become 10x more productive when developing and debugging re-frame applications."
  :url "https://github.com/Day8/re-frame-10x"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.671"]
                 [reagent "0.6.0" :scope "provided"]
                 [re-frame "0.10.3" :scope "provided"]
                 [binaryage/devtools "0.9.4"]
                 [cljsjs/react-flip-move "2.9.17-0"]
                 [com.yahoo.platform.yui/yuicompressor "2.4.8" :exclusions [rhino/js]]
                 [zprint "0.4.7"]
                 ;; Update to 1.0.7 once it is released in CLJSJS
                 ;; https://github.com/cljsjs/packages/pull/1512
                 [cljsjs/react-highlight "1.0.5-0"]
                 ;[expound "0.4.0"]
                 ]
  :plugins [[thomasa/mranderson "0.4.7"]
            [lein-less "RELEASE"]]
  :deploy-repositories {"releases"  :clojars
                        "snapshots" :clojars}

  ;:source-paths ["target/srcdeps"]

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]

  :profiles {:dev        {:dependencies [[binaryage/dirac "RELEASE"]]}
             :mranderson {:dependencies ^:replace [^:source-dep [re-frame "0.10.2"
                                                                 :exclusions [org.clojure/clojurescript
                                                                              cljsjs/react
                                                                              cljsjs/react-dom
                                                                              cljsjs/react-dom-server
                                                                              cljsjs/create-react-class
                                                                              org.clojure/tools.logging
                                                                              net.cgrand/macrovich]]
                                                   ^:source-dep [reagent "0.7.0"
                                                                 :exclusions [org.clojure/clojurescript
                                                                              cljsjs/react
                                                                              cljsjs/react-dom
                                                                              cljsjs/react-dom-server
                                                                              cljsjs/create-react-class
                                                                              org.clojure/tools.logging
                                                                              net.cgrand/macrovich]]
                                                   ^:source-dep [garden "1.3.3"
                                                                 :exclusions [com.yahoo.platform.yui/yuicompressor]]]}})
