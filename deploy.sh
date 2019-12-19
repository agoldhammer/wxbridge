echo Compiling frontend
cd ~/Prog/reefer
shadow-cljs release app
cd ~/Prog/wxbridge
echo Compiling backend
lein uberjar
echo Copying to cloud server
scp /home/agold/Prog/wxbridge/target/uberjar/wxbridge-0.1.0-standalone.jar aws2:~/.
echo Logging into server
ssh aws2


