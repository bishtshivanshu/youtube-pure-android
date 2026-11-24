@echo off
setlocal

echo =======================================================
echo   PureTube - Automatic GitHub Actions Cloud Build
echo =======================================================
echo.

where gh >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo [1/3] GitHub CLI detected. Creating repository and pushing...
    gh repo create youtube-pure-android --public --source=. --remote=origin --push
    if %ERRORLEVEL% equ 0 (
        echo.
        echo [2/3] Repository created and code pushed successfully!
        echo [3/3] Opening GitHub Actions build page...
        start https://github.com/bishtshivanshu/youtube-pure-android/actions
        goto done
    )
)

echo [1/4] Checking remote origin...
git remote remove origin 2>nul
git remote add origin https://github.com/bishtshivanshu/youtube-pure-android.git

echo.
echo [2/4] If you haven't created the repository on GitHub yet:
echo       Opening https://github.com/new?name=youtube-pure-android ...
start https://github.com/new?name=youtube-pure-android
echo.
echo Please click "Create repository" in your browser, then press any key below.
pause

echo.
echo [3/4] Pushing code to GitHub...
git push -u origin main

if %ERRORLEVEL% equ 0 (
    echo.
    echo [4/4] Successfully pushed! Opening GitHub Actions page...
    start https://github.com/bishtshivanshu/youtube-pure-android/actions
    echo.
    echo GitHub Actions is now automatically building your Android 16 APK!
    echo Once completed (~2 mins), download the APK artifact directly to your phone.
) else (
    echo.
    echo Push failed. Please verify that the repository exists and you have write permissions.
)

:done
echo.
pause
