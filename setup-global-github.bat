@echo off
setlocal

echo ==============================================================
echo    Permanent GitHub Configuration for All Future Projects
echo ==============================================================
echo.
echo This script configures your machine once so you can seamlessly
echo use GitHub for PureTube and all your future projects.
echo.

echo [1/4] Configuring Global Git Identity...
git config --global user.name "Shivanshu Bisht"
git config --global user.email "bishtshivanshu@users.noreply.github.com"
git config --global init.defaultBranch main
git config --global credential.helper manager

echo    [OK] User name set to: Shivanshu Bisht
echo    [OK] User email set to: bishtshivanshu@users.noreply.github.com
echo    [OK] Default branch set to: main
echo    [OK] Windows Credential Manager configured (passwords/tokens saved in Windows Vault)
echo.

echo [2/4] Checking GitHub CLI (gh)...
where gh >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo GitHub CLI (gh) is not installed yet.
    echo GitHub CLI allows you to create and push repositories directly from terminal!
    echo.
    echo Would you like to install GitHub CLI now via winget? (Y/N)
    set /p INSTALL_GH="> "
    if /i "%INSTALL_GH%"=="Y" (
        echo Installing GitHub CLI...
        winget install --id GitHub.cli -e --source winget
        echo.
        echo GitHub CLI installed! Please restart your terminal after this script.
    )
) else (
    echo    [OK] GitHub CLI is already installed.
)

echo.
echo [3/4] Authenticating with GitHub...
where gh >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo Running "gh auth login" to link your GitHub account permanently...
    gh auth login -w -p https
    gh auth setup-git
) else (
    echo Once you install GitHub CLI, run "gh auth login" to link your account.
)

echo.
echo ==============================================================
echo    Setup Complete!
echo ==============================================================
echo In any future project, you can now simply run:
echo    git init
echo    git add .
echo    git commit -m "initial commit"
echo    gh repo create my-project-name --public --source=. --push
echo.
echo And it will automatically create and push to your GitHub!
echo ==============================================================
pause
