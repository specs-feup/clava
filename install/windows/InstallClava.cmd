@echo off
setlocal
net session 1>NUL 2>NUL || (echo Please run this script as an Administrator. Exiting... & Exit /b 1)
echo Installing Clava on %PROGRAMFILES%\Clava
C:
cd %PROGRAMFILES%
if not exist Clava md Clava
cd Clava

if not exist clava.zip curl https://specs.fe.up.pt/tools/clava.zip > clava.zip
tar -xvf clava.zip
del clava.zip

echo @echo off > clava.cmd
echo java -jar "%%~dp0clava.jar" %%^* >> clava.cmd

if not exist icon.ico curl https://raw.githubusercontent.com/specs-feup/clava/master/install/windows/icon.ico > icon.ico
set ICO = "%PROGRAMFILES%/Clava/clava.ico"
set SCRIPT="%TEMP%\%RANDOM%-%RANDOM%-%RANDOM%-%RANDOM%.vbs"
echo Set oWS = WScript.CreateObject("WScript.Shell") >> %SCRIPT%
echo sLinkFile = "%USERPROFILE%\Desktop\Clava.lnk" >> %SCRIPT%
echo Set oLink = oWS.CreateShortcut(sLinkFile) >> %SCRIPT%
echo oLink.TargetPath = "%PROGRAMFILES%/Clava/clava.cmd" >> %SCRIPT%
echo oLink.IconLocation = "%PROGRAMFILES%/Clava/icon.ico" >> %SCRIPT%
echo oLink.Save >> %SCRIPT%
cscript /nologo %SCRIPT%
del %SCRIPT%

where /q java
if errorlevel 1 (
    echo:
    echo Java is missing! You need to have Java on the PATH for Clava to run.
    exit /B
) else (
    echo:
    echo Java found on the path.
)

echo: 
echo Clava successfully installed to %PROGRAMFILES%\Clava
echo:
echo Start Clava by opening clava.cmd, or by using the Desktop shortcut. 
echo You may also want to add Clava's install directory to the PATH if you want to access Clava from anywhere.
echo:
pause
