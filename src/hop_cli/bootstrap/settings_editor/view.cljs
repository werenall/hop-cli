(ns view
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 ::set-active-view
 (fn [db [_ view]]
   (assoc db :active-view view)))

(rf/reg-sub
 ::active-view
 (fn [db]
   (get db :active-view)))
