$ErrorActionPreference = "Continue"
$outDir = "D:\HYLcode\cola\code\SitBreak\app\src\main\res\drawable"
New-Item -ItemType Directory -Force $outDir | Out-Null

# name -> iconify mdi icon id
$icons = @{
    "ic_timer"         = "timer-outline"
    "ic_walk"          = "walk"
    "ic_shoe_print"    = "shoe-print"
    "ic_chart"         = "chart-bar"
    "ic_cog"           = "cog-outline"
    "ic_bell_ring"     = "bell-ring-outline"
    "ic_vibrate"       = "vibrate"
    "ic_volume_high"   = "volume-high"
    "ic_alarm"         = "alarm-light-outline"
    "ic_fire"          = "fire"
    "ic_trophy"        = "trophy-outline"
    "ic_medal"         = "medal-outline"
    "ic_check_circle"  = "check-circle"
    "ic_play"          = "play-circle-outline"
    "ic_pause"         = "pause-circle-outline"
    "ic_height"        = "human-male-height"
    "ic_weight"        = "weight-kilogram"
    "ic_calendar"      = "calendar-outline"
    "ic_bulb"          = "lightbulb-on-outline"
    "ic_moon"          = "weather-night"
    "ic_handsup"       = "human-handsup"
    "ic_clock"         = "clock-outline"
    "ic_shield"        = "shield-check-outline"
    "ic_seat"          = "seat-passenger"
    "ic_run_fast"      = "run-fast"
    "ic_power"         = "power-settings"
}

$ok = 0; $fail = @()
foreach ($entry in $icons.GetEnumerator()) {
    $fileName = $entry.Key; $iconId = $entry.Value
    $svgUrl = "https://api.iconify.design/mdi/$iconId.svg"
    try {
        $svg = (Invoke-WebRequest -Uri $svgUrl -UseBasicParsing -TimeoutSec 20).Content
        $ds = [regex]::Matches($svg, '<path[^>]*d="([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
        if ($ds.Count -eq 0) { throw "no path found" }
        $paths = ""
        foreach ($d in $ds) {
            $paths += "    <path`n        android:fillColor=""#FF000000""`n        android:pathData=""$d"" />`n"
        }
        $xml = "<?xml version=""1.0"" encoding=""utf-8""?>`n<vector xmlns:android=""http://schemas.android.com/apk/res/android""`n    android:width=""24dp""`n    android:height=""24dp""`n    android:viewportWidth=""24""`n    android:viewportHeight=""24"">`n$paths</vector>`n"
        [System.IO.File]::WriteAllText("$outDir\$fileName.xml", $xml, (New-Object System.Text.UTF8Encoding($false)))
        $ok++
    } catch {
        $fail += "$fileName($iconId): $($_.Exception.Message)"
    }
}
"Downloaded OK: $ok / $($icons.Count)"
if ($fail.Count -gt 0) { "FAILED:"; $fail }
