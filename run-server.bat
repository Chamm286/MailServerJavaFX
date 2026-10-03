@echo off
chcp 65001 >nul
title MAIL SERVER - UDP

set JDK=C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot
set JAVAFX=F:\MailServerJavaFX\lib\javafx-sdk-21.0.5\lib
set LIBS=F:\MailServerJavaFX\lib\ikonli-core-12.3.1.jar;F:\MailServerJavaFX\lib\ikonli-javafx-12.3.1.jar;F:\MailServerJavaFX\lib\ikonli-fontawesome5-pack-12.3.1.jar
set PROJ=F:\MailServerJavaFX

echo Starting SERVER...
"%JDK%\bin\java" --module-path "%JAVAFX%" --add-modules javafx.controls,javafx.fxml -cp "%PROJ%\bin;%LIBS%" server.ServerApp

pause