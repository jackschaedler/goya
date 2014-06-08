(ns goya.globalcommands
  (:require-macros [cljs.core.async.macros :refer [go]])
	(:require [cljs.core.async :refer [put! chan <! alts!]]))


(def command-chan (chan))

(defn copy []
  (put! command-chan :copy))

(defn paste []
  (put! command-chan :paste))
