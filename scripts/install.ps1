$ErrorActionPreference = "Stop"

$dest = "$env:LOCALAPPDATA\go2web\go2web.exe"
$dir  = Split-Path $dest

Write-Host "Installing go2web to $dir..."
New-Item -ItemType Directory -Force -Path $dir | Out-Null
Copy-Item "$PSScriptRoot\go2web.exe" -Destination $dest

$userPath = [Environment]::GetEnvironmentVariable("PATH", "User")
if ($userPath -notlike "*$dir*") {
    [Environment]::SetEnvironmentVariable("PATH", "$userPath;$dir", "User")
    Write-Host "Added to PATH. Restart your terminal."
}

Write-Host "Done! Run: go2web -h"
