param([string[]]$Versions = @('1.1.23', '1.8.62', '1.9.63', '1.9.64', '1.9.65'))
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    New-Item -ItemType Directory -Force build | Out-Null
    $baselineClasses = $null
    foreach ($version in $Versions) {
        if ($version -notmatch '^\d+\.\d+\.\d+$') { throw "Invalid version: $version" }
        if (!(Test-Path "libs/SlashBladeResharped-1.20.1-$version.jar")) { throw "Missing local dependency: $version" }
        Copy-Item -LiteralPath "libs/SlashBladeResharped-1.20.1-$version.jar" -Destination "libs/SlashBladeRuntime-1.20.1-$version.jar"
        $log = "build/compat-runtime-$version.log"
        # Keep compilation on 1.1.23: test binary compatibility, not just source compatibility.
        & ./gradlew.bat --offline "-PslashbladeRuntimeVersion=$version" "-PrunDir=run-compat-runtime-$version" runGameTestServer *> $log
        if ($LASTEXITCODE -ne 0 -or !(Select-String -LiteralPath $log -Pattern 'All 16 required tests passed' -Quiet)) {
            Get-Content -LiteralPath $log -Tail 60
            throw "Compatibility tests failed: $version"
        }
        $classHashes = (Get-ChildItem build/classes/java/main -Recurse -Filter '*.class' |
            Sort-Object FullName | Get-FileHash -Algorithm SHA256 | ForEach-Object Hash) -join ','
        if ($null -eq $baselineClasses) { $baselineClasses = $classHashes }
        elseif ($classHashes -ne $baselineClasses) { throw "Compiled classes changed during runtime-only test: $version" }
        Write-Output "PASS SlashBlade $version (16 required tests; optional Boss tests may skip)"
    }
} finally { Pop-Location }
