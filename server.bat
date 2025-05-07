@echo off
@title  104.1°æ 
Color 0

:StartServer
PATH=jdk-1.8\bin;%PATH%;
set CLASSPATH=.;target\*
java -Xms128m -Xmx4g -server -XX:+UseG1GC -Xloggc:logs/gc.log -Dwzpath=wz\ server.Start
pause