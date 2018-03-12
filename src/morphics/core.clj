(ns morphics.core)

(defn oo
  "Look up `funkey` in `funmap`, make sure it is a function, and apply it to `rest`"
  [funmap funkey & rest]
  { 
   :pre [(fn? (get funmap funkey))] 
   }
  (apply (get funmap funkey) rest))
