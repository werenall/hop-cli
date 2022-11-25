(ns hop-cli.bootstrap.main
  (:require [hop-cli.bootstrap.infrastructure :as infrastructure]
            [hop-cli.bootstrap.infrastructure.aws]
            [hop-cli.bootstrap.profile :as profile]
            [hop-cli.bootstrap.settings-reader :as sr]
            [hop-cli.util.thread-transactions :as tht]
            [hop-cli.bootstrap.util :as bp.util]))

(defn bootstrap-hop
  [{:keys [settings-file-path target-project-dir]}]
  (->
   [{:txn-fn
     (fn read-settings [_]
       (let [result (sr/read-settings settings-file-path)]
         (if (:success? result)
           {:success? true
            :settings
            (assoc-in (:settings result) [:project :target-dir] target-project-dir)}
           {:success? false
            :reason :could-not-read-settings
            :error-details result})))}
    {:txn-fn
     (fn provision-infrastructure
       [{:keys [settings]}]
       (let [result (infrastructure/provision-initial-infrastructure settings)]
         (if (:success? result)
           {:success? true
            :settings (:settings result)}
           {:success? false
            :reason :failed-to-provision-infrastructure
            :error-details result})))}
    {:txn-fn
     (fn execute-profiles
       [{:keys [settings]}]
       (let [result (profile/execute-profiles! settings)]
         (if (:success? result)
           {:success? true
            :settings (:settings result)}
           {:success? false
            :reason :could-not-execute-profiles
            :error-details result})))}
    {:txn-fn
     (fn save-environment-variables
       [{:keys [settings]}]
       (let [result (infrastructure/save-environment-variables settings)]
         (if (:success? result)
           result
           {:success? false
            :reason :could-not-save-env-variables
            :error-details result})))}
    {:txn-fn
     (fn post-installation-messages
       [{:keys [settings] :as prv-result}]
       (let [messages (bp.util/get-settings-value settings :project/post-installation-messages)]
         (doseq [msg messages]
           (println msg))
         prv-result))}]
   (tht/thread-transactions {})))
