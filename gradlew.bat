@ECHO OFF
SETLOCAL
SET DIR=%~dp0
IF NOT EXIST "%DIR%gradle\wrapper\gradle-wrapper.jar" (
  ECHO Falta gradle-wrapper.jar. Ejecuta primero Preparar-Gradle.ps1 en PowerShell.
  EXIT /B 1
)
IF DEFINED JAVA_HOME (
  SET "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) ELSE (
  SET "JAVA_EXE=java.exe"
)
"%JAVA_EXE%" -Xmx128m -Dorg.gradle.appname=gradlew -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
EXIT /B %ERRORLEVEL%
