(ns leiningen.uberjar-deploy
  (:require
    [leiningen.core.main :as main :refer [info]]
    [leiningen.deploy :as deploy :refer [deploy]]
    [leiningen.jar :as jar :refer [get-jar-filename]]
    [leiningen.pom :as pom :refer [snapshot?]]
    [clojure.java.shell :as sh]))

(defmacro with-private-fns [[ns fns] & code]
  "Refers private fns from ns and runs code in context."
  `(let ~(reduce #(conj %1 %2 `(ns-resolve '~ns '~%2)) [] fns)
     ~@code))


(defn cur-branch []
  (-> (sh/sh "git" "rev-parse" "--abbrev-ref" "HEAD")
      :out
      butlast
      clojure.string/join))


(with-private-fns [leiningen.deploy [in-branches]]
  (defn uberjar-deploy
    "Deploys an existing uberjar with lein deploy"
    [project & args]
    (main/info "Preparing to deploy uberjar")

    (let [branches (set (:deploy-branches project))]
       (when (and (seq branches)
                  (in-branches branches))
         (apply main/abort "Can only deploy from branches listed in"
                ":deploy-branches:" branches
                "; current branch: " (cur-branch))))

    (let [repository (if (pom/snapshot? project) "snapshots" "releases")
          group (:group project)
          name (:name project)
          identifier (format "%s/%s" group name)
          version (:version project)
          file (or (:jar-filename project) (jar/get-jar-filename project :standalone))]
      (main/info "Running: lein deploy" repository identifier version file)
      (deploy/deploy project repository identifier version file)))
)