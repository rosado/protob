PROTOB
######

Simple way of turning Clojure code into protocol buffer language.

It numbers the fields itself, so you don't have to doit yourself
whenever you change the order of fields in your messages. 

Example: 
         (message Msg
           :required :int32 id
           :optional :string text
           :optional :int32 timeout [default = 100])

Becomes: 
         message Msg {
           required int32 id = 1;
           optional string text = 2;
           optional int32 timeout = 3 [default = 100];
         }
