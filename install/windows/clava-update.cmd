@echo off
setlocal
net session 1>NUL 2>NUL || (echo Please run this script as an Administrator. Exiting... & Exit /b 1)
C:
cd %PROGRAMFILES%
if not exist Clava (echo Clava is not installed in its default directory of %PROGRAMFILES%\Clava & Exit /b 1)
cd Clava

echo Updating Clava on %PROGRAMFILES%\Clava
echo:
curl https://specs.fe.up.pt/tools/clava.zip > clava.zip
tar -xvf clava.zip
del clava.zip
echo:
echo Clava successfully updated
echo:
pause
