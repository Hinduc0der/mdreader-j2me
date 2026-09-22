@echo off
rem =====================================================================
rem  MD Reader - sborka MIDlet (javac -> preverify -> jar -> jad)
rem  Tekst v etom faile bez kirillicy naarochno: cmd.exe i cp866.
rem  Opisanie po-russki - v PLAN.md i README.md
rem
rem  build.bat          - obychnaya sborka (klassy kak est, udobno otlazhivat)
rem  build.bat release  - + ProGuard (szhatie, obfuskaciya) - dlya telefona
rem =====================================================================
setlocal enabledelayedexpansion
cd /d "%~dp0"

set JDK=C:\Program Files\Eclipse Adoptium\jdk-8.0.504.1-hotspot
set WTK=C:\WTK2.5.2_01
set PROGUARD=C:\j2me-tools\proguard-base-6.2.2.jar

set NAME=MDReader
set VERSION=0.1.2
set VENDOR=hinducoder
set MIDLET=MD Reader
set MAINCLASS=mdreader.MDReaderMIDlet

set BOOTCP=%WTK%\lib\cldcapi11.jar;%WTK%\lib\midpapi20.jar;%WTK%\lib\jsr75.jar

if not exist "%JDK%\bin\javac.exe" ( echo NET JDK: %JDK% & exit /b 1 )
if not exist "%WTK%\bin\preverify.exe" ( echo NET WTK: %WTK% & exit /b 1 )

rem V arhiv skladyvaem tolko release-sborki: imenno oni uezzhayut na telefon.
set REL=
if "%1"=="release" set REL=1

if exist build rmdir /s /q build
mkdir build\classes
mkdir build\pre
if not exist dist mkdir dist
if not exist dist\versions mkdir dist\versions

echo [1/4] javac
"%JDK%\bin\javac.exe" -source 1.3 -target 1.3 -nowarn -encoding UTF-8 ^
  -bootclasspath "%BOOTCP%" -d build\classes src\mdreader\*.java
if errorlevel 1 exit /b 1

if "%1"=="release" goto shrink

echo [2/4] preverify
"%WTK%\bin\preverify.exe" -classpath "%BOOTCP%" -d build\pre build\classes
if errorlevel 1 exit /b 1
goto manifest

:shrink
echo [2/4] proguard + preverify
"%JDK%\bin\java.exe" -jar "%PROGUARD%" @proguard.cfg ^
  -injars build\classes -outjars build\pre ^
  -libraryjars "%WTK%\lib\cldcapi11.jar" ^
  -libraryjars "%WTK%\lib\midpapi20.jar" ^
  -libraryjars "%WTK%\lib\jsr75.jar"
if errorlevel 1 exit /b 1

:manifest
echo [3/4] manifest
> build\MANIFEST.MF echo MIDlet-Name: %NAME%
>> build\MANIFEST.MF echo MIDlet-Version: %VERSION%
>> build\MANIFEST.MF echo MIDlet-Vendor: %VENDOR%
>> build\MANIFEST.MF echo MIDlet-1: %MIDLET%,/icon.png,%MAINCLASS%
>> build\MANIFEST.MF echo MicroEdition-Configuration: CLDC-1.1
>> build\MANIFEST.MF echo MicroEdition-Profile: MIDP-2.0
>> build\MANIFEST.MF echo MIDlet-Description: Markdown reader

echo [4/4] jar
if exist res\icon.png copy /y res\icon.png build\pre\icon.png >nul
"%JDK%\bin\jar.exe" cfm dist\%NAME%.jar build\MANIFEST.MF -C build\pre .
if errorlevel 1 exit /b 1

for %%F in (dist\%NAME%.jar) do set JARSIZE=%%~zF
> dist\%NAME%.jad echo MIDlet-Name: %NAME%
>> dist\%NAME%.jad echo MIDlet-Version: %VERSION%
>> dist\%NAME%.jad echo MIDlet-Vendor: %VENDOR%
>> dist\%NAME%.jad echo MIDlet-1: %MIDLET%,/icon.png,%MAINCLASS%
>> dist\%NAME%.jad echo MicroEdition-Configuration: CLDC-1.1
>> dist\%NAME%.jad echo MicroEdition-Profile: MIDP-2.0
>> dist\%NAME%.jad echo MIDlet-Jar-URL: %NAME%.jar
>> dist\%NAME%.jad echo MIDlet-Jar-Size: !JARSIZE!

echo.
echo GOTOVO: dist\%NAME%.jar (!JARSIZE! bytes), dist\%NAME%.jad

if not "%REL%"=="1" (
  echo Otladochnaya sborka - v dist\versions ne kladem. Dlya arhiva: build.bat release
  goto done
)

echo [5/5] arhiv versii
set ARC=dist\versions\%NAME%-%VERSION%
if exist "%ARC%.jar" echo VNIMANIE: %ARC%.jar uzhe byl - perezapisan. Zabyli podnyat VERSION?
copy /y dist\%NAME%.jar "%ARC%.jar" >nul
rem U arhivnogo .jad svoyo imya .jar ryadom, inache telefon ne naydet fail.
> "%ARC%.jad" echo MIDlet-Name: %NAME%
>> "%ARC%.jad" echo MIDlet-Version: %VERSION%
>> "%ARC%.jad" echo MIDlet-Vendor: %VENDOR%
>> "%ARC%.jad" echo MIDlet-1: %MIDLET%,/icon.png,%MAINCLASS%
>> "%ARC%.jad" echo MicroEdition-Configuration: CLDC-1.1
>> "%ARC%.jad" echo MicroEdition-Profile: MIDP-2.0
>> "%ARC%.jad" echo MIDlet-Jar-URL: %NAME%-%VERSION%.jar
>> "%ARC%.jad" echo MIDlet-Jar-Size: !JARSIZE!
echo V ARHIVE: %ARC%.jar + .jad
echo Na telefon kopiruem PARU iz dist\versions - oba faila v odnu papku.

:done
endlocal
