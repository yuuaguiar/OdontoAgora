param([switch]$Headless)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$configPath=Join-Path $projectRoot 'local-demo.json'
if(-not (Test-Path -LiteralPath $configPath)){throw 'Configuracao local ausente. Siga as instrucoes de execucao em outra maquina no README.md.'}
$config=Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
$logDir=Join-Path $projectRoot 'demo-logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$healthy=$false
try{$healthy=(Invoke-RestMethod 'http://127.0.0.1:8080/health' -TimeoutSec 2).service -eq 'OdontoAgora'}catch{}
if(-not $healthy){
    $serverFile=Join-Path $projectRoot 'server\server.py'
    Start-Process -FilePath $config.python -ArgumentList "`"$serverFile`" --demo" -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logDir 'server.log') -RedirectStandardError (Join-Path $logDir 'server-error.log')
    for($attempt=0;$attempt -lt 12;$attempt++){
        Start-Sleep -Milliseconds 500
        try{if((Invoke-RestMethod 'http://127.0.0.1:8080/health' -TimeoutSec 1).service -eq 'OdontoAgora'){$healthy=$true;break}}catch{}
    }
    if(-not $healthy){throw 'A API nao iniciou. Confira demo-logs\server-error.log.'}
}
$adb=Join-Path $config.sdk 'platform-tools\adb.exe'
$env:ANDROID_SDK_ROOT=$config.sdk
$env:ANDROID_AVD_HOME=$config.avdHome
$devices=(& $adb devices) -join "`n"
if($devices -notmatch 'emulator-5554\s+device'){
    $emulator=Join-Path $config.sdk 'emulator\emulator.exe'
    $emulatorArgs='-avd OdontoAgora -no-snapshot -no-boot-anim -gpu swiftshader_indirect -port 5554'
    if($Headless){$emulatorArgs+=' -no-window'}
    Start-Process -FilePath $emulator -ArgumentList $emulatorArgs -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logDir 'emulator.log') -RedirectStandardError (Join-Path $logDir 'emulator-error.log')
}
Write-Output 'Aguardando o Android iniciar...'
$booted=$false
for($attempt=0;$attempt -lt 90;$attempt++){
    $boot=(& $adb -s emulator-5554 shell getprop sys.boot_completed 2>$null) -join ''
    if($boot.Trim() -eq '1'){$booted=$true;break}
    Start-Sleep -Seconds 1
}
if(-not $booted){throw 'O emulador nao iniciou. Confira os arquivos em demo-logs.'}
$apk=Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'
if(-not (Test-Path -LiteralPath $apk)){
    $env:JAVA_HOME=$config.java
    Push-Location $projectRoot
    try{& '.\gradlew.bat' assembleDebug --console=plain;if($LASTEXITCODE -ne 0){throw 'Falha ao compilar o aplicativo.'}}finally{Pop-Location}
}
& $adb -s emulator-5554 install -r $apk
if($LASTEXITCODE -ne 0){throw 'Falha ao instalar o APK.'}
& $adb -s emulator-5554 shell am start -n br.edu.odontoagora/.MainActivity
Write-Output 'OdontoAgora pronto. Use Entrar na demonstracao em qualquer um dos perfis.'
