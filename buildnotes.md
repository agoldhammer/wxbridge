# Notes for build and deployment
1. Frontend for this is reefer
2. Build reefer with shadow-cljs release app
3. Build wxbridge with lein uberjar
4. scp (standalone uberjar) aws2:~/wxbridge/
5. On aws2, java -jar (standalone uberjar)
6. Make sure dns is set to wx.noozewire.com