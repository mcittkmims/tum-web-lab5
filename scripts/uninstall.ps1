$ErrorActionPreference = "Stop"

$dest = "$env:LOCALAPPDATA\go2web\go2web.exe"
$dir  = Split-Path $dest

if (-not (Test-Path $dest)) {
    Write-Host "go2web is not installed at $dest"
    exit 1
}

Write-Host "Removing go2web..."
Remove-Item $dest -Force

$userPath = [Environment]::GetEnvironmentVariable("PATH", "User")
if ($userPath -like "*$dir*") {
    $newPath = ($userPath -split ";" | Where-Object { $_ -ne $dir }) -join ";"
    [Environment]::SetEnvironmentVariable("PATH", $newPath, "User")
    Write-Host "Removed from PATH. Restart your terminal."
}

Write-Host "Done. go2web has been uninstalled."
