<#
.SYNOPSIS
    Open / close an SSH tunnel that maps Staging Docker internal IPs to local ports.
    Windows port of run_tunnel.py in docs/BACKEND_SETUP_GUIDE.md (Section 3.2).

.DESCRIPTION
    Maps these staging Docker internal IPs to 0.0.0.0 on the local host:
        0.0.0.0:3307  -> 172.21.0.6:3306   (MySQL Staging)
        0.0.0.0:27018 -> 172.21.0.4:27017  (MongoDB Staging)
        0.0.0.0:6379  -> 172.21.0.5:6379   (Redis Staging)
        0.0.0.0:5672  -> 172.21.0.3:5672   (RabbitMQ Staging)
        0.0.0.0:5701  -> 172.21.0.2:5701   (Hazelcast Staging)

    Uses plink.exe (PuTTY) for password-based SSH on Windows. Backgrounded via
    Start-Process; PID is saved to $env:TEMP\sunwinkr-tunnel.pid so Close can
    find it later.

.PARAMETER Action
    Open | Close | Status. Default: Open.

.PARAMETER StopLocalContainers
    When -Action Open: stop conflicting local DB containers
    (sunwinkr-mysql, -mongodb, -redis, -rabbitmq, -hazelcast) before opening
    the tunnel so port 3307/27018/6379/5672/5701 are free.

.EXAMPLE
    ./staging-tunnel.ps1 -Action Open -StopLocalContainers

.EXAMPLE
    ./staging-tunnel.ps1 -Action Status
    ./staging-tunnel.ps1 -Action Close

.NOTES
    The original run_tunnel.py uses `ssh -L 0.0.0.0:...` which requires
    GatewayPorts=yes on the SSH server. plink behaves the same. The bound
    address 0.0.0.0 means OTHER hosts on your LAN can reach the staging DBs
    via your machine -- be aware of that.
#>
[CmdletBinding()]
param(
    [ValidateSet('Open', 'Close', 'Status')]
    [string]$Action = 'Open',
    [string]$StagingHost = '140.99.130.21',
    [string]$StagingUser = 'root',
    [string]$Password,
    [switch]$StopLocalContainers
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\_common.ps1"

$pidFile = Join-Path $env:TEMP 'sunwinkr-tunnel.pid'

# Resolved at Open time via `docker inspect` over SSH -- the static IPs in
# BACKEND_SETUP_GUIDE.md drift every time staging recreates containers.
# Mapping: local host port -> @{ Container, RemotePort, Label }
$portMap = @(
    @{ Local = 3307;  Container = 'sunwinkr-mysql';     RemotePort = 3306;  Service = 'MySQL Staging' },
    @{ Local = 27018; Container = 'sunwinkr-mongodb';   RemotePort = 27017; Service = 'MongoDB Staging' },
    @{ Local = 6379;  Container = 'sunwinkr-redis';     RemotePort = 6379;  Service = 'Redis Staging' },
    @{ Local = 5672;  Container = 'sunwinkr-rabbitmq';  RemotePort = 5672;  Service = 'RabbitMQ Staging' },
    @{ Local = 5701;  Container = 'sunwinkr-hazelcast'; RemotePort = 5701;  Service = 'Hazelcast Staging' }
)

# Resolve current Docker IP for a container on the staging host. Returns the
# first non-empty IP across networks.
function Resolve-StagingContainerIp {
    param(
        [Parameter(Mandatory)] [string]$Container,
        [string]$StagingHost,
        [string]$StagingUser,
        [string]$Password
    )
    $cmd = "docker inspect $Container --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}|{{end}}'"
    $raw = Invoke-StagingSsh -Command $cmd `
        -StagingHost $StagingHost -StagingUser $StagingUser -Password $Password
    $ip = ($raw -split '\|') | Where-Object { $_ -match '^\d+\.\d+\.\d+\.\d+$' } | Select-Object -First 1
    if (-not $ip) { throw "Could not resolve IP for container '$Container' on staging." }
    return $ip
}

function Get-RunningTunnelPid {
    if (-not (Test-Path $pidFile)) { return $null }
    $candidate = (Get-Content $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1).Trim()
    if (-not $candidate) { return $null }
    $proc = Get-Process -Id $candidate -ErrorAction SilentlyContinue
    if ($proc -and $proc.ProcessName -eq 'plink') { return [int]$candidate }
    return $null
}

function Open-Tunnel {
    Write-Step "Opening Staging Full-Infra SSH tunnel"

    $existing = Get-RunningTunnelPid
    if ($existing) {
        Write-Warn "Tunnel already running (PID $existing). Use -Action Close first."
        return
    }

    if ($StopLocalContainers) {
        $containers = @('sunwinkr-mysql', 'sunwinkr-mongodb', 'sunwinkr-redis', 'sunwinkr-rabbitmq', 'sunwinkr-hazelcast')
        Write-Host "    Stopping local containers to free ports: $($containers -join ', ')"
        foreach ($c in $containers) {
            if (Test-DockerContainer -Name $c) {
                & docker stop $c | Out-Null
                Write-Host "      stopped $c"
            }
        }
    } else {
        Write-Warn "Local containers NOT stopped. If port 3307/27018/6379/5672/5701 are bound, the tunnel will fail."
        Write-Warn "Re-run with -StopLocalContainers if you hit 'bind: address already in use'."
    }

    Assert-PuttyTools
    $pw = Get-StagingPassword -Override $Password

    Write-Host "    Resolving current staging container IPs..."
    foreach ($p in $portMap) {
        $p.RemoteIp = Resolve-StagingContainerIp -Container $p.Container `
            -StagingHost $StagingHost -StagingUser $StagingUser -Password $pw
        Write-Host ("      {0,-22} -> {1}" -f $p.Container, $p.RemoteIp)
    }

    # Build plink arg list. -N: no remote command, -batch: never prompt.
    # -hostkey: pin server fingerprint (otherwise batch mode aborts on first connect).
    # We do NOT use -f (background fork) -- plink on Windows backgrounds via
    # Start-Process -WindowStyle Hidden instead.
    $plinkArgs = @('-ssh', '-batch', '-N', '-pw', $pw, '-hostkey', $script:DefaultStagingHostKey)
    foreach ($p in $portMap) {
        $plinkArgs += @('-L', "0.0.0.0:$($p.Local):$($p.RemoteIp):$($p.RemotePort)")
    }
    $plinkArgs += "$StagingUser@$StagingHost"

    $proc = Start-Process -FilePath 'plink.exe' -ArgumentList $plinkArgs `
        -WindowStyle Hidden -PassThru
    Start-Sleep -Seconds 2

    if ($proc.HasExited) {
        throw "plink exited immediately (exit code $($proc.ExitCode)). Check credentials / port conflicts."
    }

    Set-Content -Path $pidFile -Value $proc.Id -Encoding ascii
    Write-Ok "Tunnel opened (PID $($proc.Id))"
    Write-Host ""
    foreach ($p in $portMap) {
        Write-Host ("    0.0.0.0:{0,-5} -> {1}:{2,-5} ({3})" -f $p.Local, $p.RemoteIp, $p.RemotePort, $p.Service)
    }
    Write-Host ""
    Write-Host "Backend Java configs should target 172.17.0.1 (Docker gateway) on these ports."
    Write-Host "Run:  ./set-config-target.ps1 -Target Tunnel"
}

function Close-Tunnel {
    Write-Step "Closing Staging tunnel"
    $tunnelPid = Get-RunningTunnelPid
    if (-not $tunnelPid) {
        Write-Warn "No running tunnel found (no valid PID file)."
        if (Test-Path $pidFile) { Remove-Item $pidFile -Force }
        return
    }
    Stop-Process -Id $tunnelPid -Force
    Remove-Item $pidFile -Force
    Write-Ok "Tunnel PID $tunnelPid stopped."
}

function Show-Status {
    Write-Step "Tunnel status"
    $tunnelPid = Get-RunningTunnelPid
    if ($tunnelPid) {
        Write-Host "    Running. PID: $tunnelPid"
        foreach ($p in $portMap) {
            $listening = Get-NetTCPConnection -LocalPort $p.Local -State Listen -ErrorAction SilentlyContinue
            $marker = if ($listening) { '[OK]' } else { '[--]' }
            Write-Host ("    {0} 0.0.0.0:{1,-5} -> {2}" -f $marker, $p.Local, $p.Service)
        }
    } else {
        Write-Host "    Not running."
    }
}

switch ($Action) {
    'Open'   { Open-Tunnel }
    'Close'  { Close-Tunnel }
    'Status' { Show-Status }
}
