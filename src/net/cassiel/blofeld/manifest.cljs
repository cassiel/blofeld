(ns net.cassiel.blofeld.manifest)

(def SOX 0xF0)
(def EOX 0xF7)

(def WALDORF 0x3E)
(def BLOFELD 0x13)
(def BROADCAST-ID 0x7F)

;; Sound Request:
(def SNDR 0x00)

;; Sound Dump:
(def SNDD 0x10)

;; Sound Parameter Change:
(def SNDP 0x20)

;; Offset of first data byte in sound dump (from 0xF0 inclusive):
(def SNDD-DATA-START 7)

;; Start of name in the data (after header stripped):
(def SNDD-NAME-OFFSET 363)

;; Length of patch name:
(def SNDD-NAME-LENGTH 16)

;; Timeout before requesting sound on program change. If a sound is requested,
;; the Blofeld will not send subsequent bank changes until done.
(def SOUND-REQUEST-MS 1000)
