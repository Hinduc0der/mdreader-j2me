@echo off
rem =====================================================================
rem  MD Reader - sborka MIDlet (javac -> preverify -> jar -> jad)
rem  Tekst v etom faile bez kirillicy naarochno: cmd.exe i cp866.
rem  Opisanie po-russki - v PLAN.ru.md i README.ru.md
rem
rem  Yazyk interfeisa vybiraetsya na sborke: javac poluchaet Strings.java
rem  iz i18n\<yazyk>\. Spisok strok v i18n\ru i i18n\en dolzhen sovpadat,
rem  inache yazyk prosto ne skompiliruetsya.
rem
rem  build.bat             - otladochnaya sborka, russkaya (dlya emulyatora)
rem  build.bat ru          - to zhe yavno
rem  build.bat en          - otladochnaya, angliyskaya
rem  build.bat release     - release OBOIH yazykov + arhiv versii
rem  build.bat release en  - release tolko odnogo yazyka
rem =====================================================================
setlocal enabledelayedexpansion
cd /d "%~dp0"

set JDK=C:\Program Files\Eclipse Adoptium\jdk-8.0.504.1-hotspot
set WTK=C:\WTK2.5.2_01
set PROGUARD=C:\j2me-tools\proguard-base-6.2.2.jar

set NAME=MDReader
set VERSION=0.2.0
set VENDOR=hinducoder
set MIDLET=MD Reader
set MAINCLASS=mdreader.MDReaderMIDlet

set BOOTCP=%WTK%\lib\cldcapi11.jar;%WTK%\lib\midpapi20.jar;%WTK%\lib\jsr75.jar

if not exist "%JDK%\bin\javac.exe" ( echo NET JDK: %JDK% & exit /b 1 )
if not exist "%WTK%\bin\preverify.exe" ( echo NET WTK: %WTK% & exit /b 1 )

rem ---- razbor argumentov ----
set REL=
set LANGS=
:parseargs
if "%~1"=="" goto argsdone
if /i "%~1"=="release" set REL=1
if /i "%~1"=="ru" set LANGS=!LANGS! ru
if /i "%~1"=="en" set LANGS=!LANGS! en
shift
goto parseargs
:argsdone

rem Bez yavnogo yazyka: release sobiraet oba, otladka - tolko russkiy.
if "!LANGS!"=="" (
  if "%REL%"=="1" ( set LANGS=ru en ) else ( set LANGS=ru )
)

if not exist dist mkdir dist
if not exist dist\versions mkdir dist\versions

set FAIL=
for %%L in (!LANGS!) do (
  call :buildone %%L
  if errorlevel 1 set FAIL=1
)
if defined FAIL (
  echo.
  echo SBORKA NE PROSHLA
  exit /b 1
)

echo.
if "%REL%"=="1" (
  echo Gotovye pary lezhat v dist\versions - na telefon kopiruem OBA faila odnoy versii i yazyka.
) else (
  echo Otladochnaya sborka - v dist\versions ne kladem. Dlya arhiva: build.bat release
)
endlocal
exit /b 0

rem =====================================================================
rem  :buildone <ru|en>  - odin yazyk celikom
rem =====================================================================
:buildone
set LNG=%1
set OUT=%NAME%-%LNG%
set STRINGS=i18n\%LNG%\mdreader\Strings.java

echo.
echo ================ %LNG% ================
if not exist "%STRINGS%" ( echo NET FAILA: %STRINGS% & exit /b 1 )

if exist build rmdir /s /q build
mkdir build\classes
mkdir build\pre

echo [1/4] javac
"%JDK%\bin\javac.exe" -source 1.3 -target 1.3 -nowarn -encoding UTF-8 ^
  -bootclasspath "%BOOTCP%" -d build\classes src\mdreader\*.java "%STRINGS%"
if errorlevel 1 exit /b 1

if "%REL%"=="1" goto shrink

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
"%JDK%\bin\jar.exe" cfm dist\%OUT%.jar build\MANIFEST.MF -C build\pre .
if errorlevel 1 exit /b 1

for %%F in (dist\%OUT%.jar) do set JARSIZE=%%~zF
call :writejad dist\%OUT%.jad %OUT%.jar
echo GOTOVO: dist\%OUT%.jar (!JARSIZE! bytes) + .jad

if not "%REL%"=="1" goto :eof

echo [5/5] arhiv versii
set ARC=dist\versions\%NAME%-%VERSION%-%LNG%
if exist "%ARC%.jar" echo VNIMANIE: %ARC%.jar uzhe byl - perezapisan. Zabyli podnyat VERSION?
copy /y dist\%OUT%.jar "%ARC%.jar" >nul
rem U arhivnogo .jad svoyo imya .jar ryadom, inache telefon ne naydet fail.
call :writejad "%ARC%.jad" %NAME%-%VERSION%-%LNG%.jar
echo V ARHIVE: %ARC%.jar + .jad
goto :eof

rem =====================================================================
rem  :writejad <put k .jad> <imya .jar v ssylke>
rem =====================================================================
:writejad
> %1 echo MIDlet-Name: %NAME%
>> %1 echo MIDlet-Version: %VERSION%
>> %1 echo MIDlet-Vendor: %VENDOR%
>> %1 echo MIDlet-1: %MIDLET%,/icon.png,%MAINCLASS%
>> %1 echo MicroEdition-Configuration: CLDC-1.1
>> %1 echo MicroEdition-Profile: MIDP-2.0
>> %1 echo MIDlet-Jar-URL: %2
>> %1 echo MIDlet-Jar-Size: !JARSIZE!
goto :eof
