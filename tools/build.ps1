param([switch]$Test, [switch]$BossTests, [switch]$Legacy)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE '.gradle' }
Push-Location $projectRoot
try {
    $buildArgs = @('build')
    if ($Test) { $buildArgs = @('runGameTestServer', '-x', 'downloadAssets') }
    if ($BossTests) { $buildArgs += '-PbossTests' }
    if ($Legacy) { $buildArgs += @('-PslashbladeVersion=1.1.23', '-PrunDir=run-legacy') }
    & .\gradlew.bat @buildArgs
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with code $LASTEXITCODE" }
} finally { Pop-Location }
