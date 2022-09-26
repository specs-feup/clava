@echo off
setlocal
net session 1>NUL 2>NUL || (echo Please run this script as an Administrator. Exiting... & Exit /b 1)
echo Installing Clava on %PROGRAMFILES%\Clava
C:
cd %PROGRAMFILES%
if not exist Clava md Clava
cd Clava
if not exist clava.zip curl specs.fe.up.pt/tools/clava.zip > clava.zip
tar -xvf clava.zip
del clava.zip
echo @echo off > clava.cmd
echo java -jar "%%~dp0clava.jar" %%^* >> clava.cmd
echo: 
echo Clava successfully installed to %PROGRAMFILES%\Clava
echo Start Clava by opening clava.cmd. It requires Java to be installed and on the PATH.
echo You may also want to add Clava's install directory to the PATH if you want to access Clava from anywhere.
echo:
pause