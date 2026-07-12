# Launches the CodeArena backend with Groq AI enabled.
# Prompts for your GROQ_API_KEY securely (input is masked and never written to
# disk or source control). Run this from the `backend` folder:
#     .\run-with-ai.ps1

$secure = Read-Host 'Paste your GROQ_API_KEY (input hidden)' -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
$env:GROQ_API_KEY = [Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)

if (-not $env:GROQ_API_KEY) {
    Write-Host 'No key entered — the app will use offline heuristic hints.' -ForegroundColor Yellow
} else {
    Write-Host 'Key set for this session. Starting backend with Groq AI enabled…' -ForegroundColor Green
}

.\mvnw.cmd spring-boot:run
