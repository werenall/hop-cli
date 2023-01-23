(ns hop-cli.bootstrap.profile.registry.on-premises
  (:require [hop-cli.bootstrap.profile.registry :as registry]
            [hop-cli.bootstrap.util :as bp.util]))

(defn- build-docker-compose-files
  [settings]
  (let [ssl-termination-choice (bp.util/get-settings-value settings :project.profiles.on-premises.ssl-termination/value)]
    (cond-> {:to-develop [] :to-deploy [] :ci []}
      (= :https-portal ssl-termination-choice)
      (update :to-deploy concat ["docker-compose.https-portal.to-deploy.yml"]))))

(defn- build-env-variables
  [settings environment]
  (let [base-path [:project :profiles :on-premises]
        ssl-termination-choice (conj base-path :ssl-termination :value)
        domain (bp.util/get-settings-value settings (conj base-path :ssl-termination :https-portal :domain environment))
        persistent-data-dir (bp.util/get-settings-value settings (conj base-path :operating-system :persistent-data-dir))]
    (cond-> {:PERSISTENT_DATA_DIR persistent-data-dir}
      (= :http-portal ssl-termination-choice)
      (assoc :HTTPS_PORTAL_STAGE "staging"
             :HTTPS_PORTAL_DOMAINS (format "%s => https://%s, %s -> http://proxy:8080" domain domain domain)))))

(defmethod registry/pre-render-hook :on-premises
  [_ settings]
  {:files [{:src "on-premises"}]
   :deploy-files ["on-premises-files"]
   :environment-variables {:test (build-env-variables settings :test)
                           :prod (build-env-variables settings :prod)}
   :docker-compose (build-docker-compose-files settings)})
