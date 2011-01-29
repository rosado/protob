(ns rosado.protob-test
  (:use clojure.test)
  (:require [rosado.protob :as pb]))

(def simple-message "message A {
  required int32 id = 1;
}
")

(def message-with-opts "message B {
  required int32 id = 1 [default = 99];
}
")

(def nested-msg "message C {
  required int32 id = 1;
  optional int32 count = 2;
  message D {
    required int32 x = 1;
  }
  optional D d_msg = 3;
}
")

(def A (with-out-str (pb/message A :required :int32 id)))
(def B (with-out-str (pb/message B :required :int32 id [default = 99])))
(def C (with-out-str (pb/message C
                                 :required :int32 id
                                 :optional :int32 count
                                 (pb/message D :required :int32 x)
                                 :optional :D d_msg)))

(deftest messages
  (is (= simple-message A))
  (is (= message-with-opts B))
  (is (= nested-msg C)))

(def EnumA (with-out-str (pb/enum A :ONE :TWO :THREE)))
(def simple-enum "enum A {
  ONE = 0;
  TWO = 1;
  THREE = 2;
}
")

(deftest enums
  (is (= EnumA simple-enum)))
