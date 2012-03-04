;; Copyright (c) 2011, Roland Sadowski
;; All rights reserved.
;;
;; Redistribution and use in source and binary forms, with or without
;; modification, are permitted provided that the following conditions are met:
;;     * Redistributions of source code must retain the above copyright
;;       notice, this list of conditions and the following disclaimer.
;;     * Redistributions in binary form must reproduce the above copyright
;;       notice, this list of conditions and the following disclaimer in the
;;       documentation and/or other materials provided with the distribution.
;;     * Neither the name of the <organization> nor the
;;       names of its contributors may be used to endorse or promote products
;;       derived from this software without specific prior written permission.
;;
;; THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
;; ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
;; WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
;; DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
;; DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
;; (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
;; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
;; ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
;; (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
;; SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
;;
;;; protocol buffer code generator

(ns rosado.protob)

(def ^:dynamic *indent-level* 0)

(def strings
     {:message-begin "message "
      :enum-begin "enum "})

(defn- fix-name [sb name]
  (doseq [c (replace {\- \_} name)]
    (.append sb c))
  sb)

(defn- indent [sb]
  (doseq [i (range *indent-level*)]
    (.append sb " ")))

(defn- decl-start [^StringBuilder sb kw name]
  (indent sb)
  (doto sb
    (.append (strings kw))
    (.append name)
    (.append " {\n")))

(defmacro package
  "Generates package declaration."
  [package-name]
  `(.write *out* ~(str "package " package-name \; \newline)))

(defmacro enum
  "Generates enum declaration. If second argument is an integer enum
values will start with it. The result string is written to *out*.

Example: (enum Protocol :TCP :IP) --> enum Protocol { TCP = 0; IP = 1; }"
  [enum-name & elems]
  (let [sb (StringBuilder. 100)
        write-elem (fn [n p]
                     (-> sb (.append (name n))
                         (.append " = ")
                         (.append p)
                         (.append \;)
                         (.append \newline)))
        [init-pos init? elems] (if (integer? (first elems))
                                 [(first elems) true (next elems)]
                                 [0  false elems])]
    (decl-start sb :enum-begin (str enum-name))
    (binding [*indent-level* (+ 2 *indent-level*)]
      (loop [d (first elems) elems (next elems) pos init-pos]
        (when d
          (indent sb)
          (if (number? (first elems))
            (do
              ;; (when init? (throw (Exception. "You can not specify initial value and inline for enum.")))
              (write-elem d (first elems))
              (recur (fnext elems) (nnext elems) (inc (first elems))))
            (do
              (write-elem d pos)
              (recur (first elems) (next elems) (inc pos)))))))
    (.append sb "}\n")
    `(.write *out* ~(.toString sb))))

(defmacro message
  "Generates message declaration. The result string is written to *out*.

Field declarations consist of keywords, symbols and vectors. Messages
can contain other messages and enums.

Example: (message Msg
           :required :int32 id
           :optional :string text
           :optional :int32 timeout [default = 100])

Becomes: message Msg {
           required int32 id = 1;
           optional string text = 2;
           optional int32 timeout = 3 [default = 100];
         }"
  [msg-name & decls]
  (let [sb (StringBuilder. 100)
        pos 1
        spaces (fn [sb nl?]
                 (if nl?
                   (indent sb)
                   (.append sb " ")))]
    (decl-start sb :message-begin (name msg-name))
    (binding [*indent-level* (+ 2 *indent-level*)]
      (loop [d (first decls) decls (next decls) pos pos nl? true]
        (cond
         (keyword? d) (do (spaces sb nl?)
                          (.append sb (name d))
                          (recur (first decls) (next decls) pos false))
         (symbol? d) (do (spaces sb nl?)
                         (-> sb (fix-name (name d)) (.append " = ") (.append pos))
                         (if (vector? (first decls))
                           (do
                             (-> sb (.append " ") (.append (str (first decls))))
                             (-> sb (.append ";") (.append \newline))
                             (recur (fnext decls) (nnext decls) (inc pos) true))
                           (do
                             (-> sb (.append ";") (.append \newline))
                             (recur (first decls) (next decls) (inc pos) true))))
         (list? d) (do (.append sb (with-out-str (eval d)))
                       ;; (.append sb \newline)
                       (recur (first decls) (next decls) pos true))
         (nil? d) :do-nothing
         :else (throw (Exception. (str "Unexpected element of type " (type d) ": " d))))))
    (do
      (indent sb)
      (.append sb "}\n"))
    `(.write *out* ~(.toString sb))))
