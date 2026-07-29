# generate-flyway-migrations.ps1 (PS 5.1 compatible)
$ErrorActionPreference = "Stop"
$databaseDir = "D:\TEACH\teaching-system\database"
$migrationDir = "D:\TEACH\teaching-system\backend\src\main\resources\db\migration"

# Step 1: Parse init.sql
Write-Host "=== Step 1: Parse init.sql ==="
$initContent = Get-Content -Path "$databaseDir\init.sql" -Raw -Encoding UTF8
$tableNames = @{}
$createTablePattern = 'CREATE TABLE\s+`?(\w+)`?'
foreach ($match in [regex]::Matches($initContent, $createTablePattern)) {
    $tableNames[$match.Groups[1].Value.ToLower()] = $true
}
Write-Host "  Tables in init.sql: $($tableNames.Count)"

$tableColumns = @{}
$lines = $initContent -split "`n"
$currentTable = ""
$inCreateBlock = $false
$parenDepth = 0

foreach ($line in $lines) {
    $trimmed = $line.Trim()
    if ($trimmed -match 'CREATE TABLE\s+`?(\w+)`?') {
        $currentTable = $Matches[1].ToLower()
        $inCreateBlock = $true
        $parenDepth = 0
        $tableColumns[$currentTable] = @{}
    }
    if ($inCreateBlock) {
        foreach ($ch in $line.ToCharArray()) {
            if ($ch -eq '(') { $parenDepth++ }
            elseif ($ch -eq ')') { $parenDepth-- }
        }
        if ($line -match '^\s+`(\w+)`') {
            $tableColumns[$currentTable][$Matches[1].ToLower()] = $true
        }
        if ($parenDepth -le 0 -and $trimmed -match '\)') {
            $inCreateBlock = $false
        }
    }
}

# Step 2: Collect and classify files
Write-Host "`n=== Step 2: Classify SQL files ==="
$allFiles = Get-ChildItem -Path $databaseDir -Filter "v*.sql" -File |
    Where-Object { $_.DirectoryName -eq $databaseDir }

$versionEntries = @()
foreach ($file in $allFiles) {
    if ($file.Name -match "^v(\d+)") {
        $verNum = [int]$Matches[1]
        $versionEntries += [PSCustomObject]@{
            File    = $file
            Version = $verNum
            Name    = $file.Name
        }
    }
}
$versionEntries = $versionEntries | Sort-Object Version, Name
Write-Host "  Total files: $($versionEntries.Count)"

$classified = @()
foreach ($entry in $versionEntries) {
    $content = Get-Content -Path $entry.File.FullName -Raw -Encoding UTF8
    $hasDdl = $content -match '(?im)^\s*(CREATE\s+(TABLE|INDEX|UNIQUE\s+INDEX)|ALTER\s+TABLE|DROP\s+(TABLE|INDEX))' -or
              $content -match '(?im)ADD\s+(COLUMN|INDEX|UNIQUE|KEY|PRIMARY\s+KEY|CONSTRAINT)'
    $hasDml = $content -match '(?im)^\s*(INSERT\s+INTO|SET\s+@|LOCK\s+TABLES|UNLOCK\s+TABLES)'

    $category = "OTHER"
    if ($hasDdl -and $hasDml) { $category = "MIXED" }
    elseif ($hasDdl) { $category = "DDL_ONLY" }
    elseif ($hasDml) { $category = "DML_ONLY" }

    $alreadyInInit = $false
    if ($hasDdl) {
        $alreadyInInit = $true
        # Check CREATE TABLE
        foreach ($m in [regex]::Matches($content, '(?im)CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?(\w+)`?')) {
            $tbl = $m.Groups[1].Value.ToLower()
            if (-not $tableNames.ContainsKey($tbl)) { $alreadyInInit = $false; break }
        }
        # Check ALTER TABLE ADD COLUMN
        if ($alreadyInInit) {
            foreach ($m in [regex]::Matches($content, '(?im)ALTER\s+TABLE\s+`?(\w+)`?')) {
                $tbl = $m.Groups[1].Value.ToLower()
                foreach ($ac in [regex]::Matches($content, '(?im)ADD\s+(?:COLUMN\s+)?`?(\w+)`?\s+\w+')) {
                    $col = $ac.Groups[1].Value.ToLower()
                    if ($tableNames.ContainsKey($tbl)) {
                        if (-not $tableColumns.ContainsKey($tbl) -or -not $tableColumns[$tbl].ContainsKey($col)) {
                            $alreadyInInit = $false
                            break
                        }
                    }
                }
                if (-not $alreadyInInit) { break }
            }
        }
    }

    $classified += [PSCustomObject]@{
        Entry         = $entry
        Category      = $category
        HasDdl        = $hasDdl
        HasDml        = $hasDml
        AlreadyInInit = $alreadyInInit
    }
}

$stats = $classified | Group-Object Category
Write-Host "`n  Classification:"
foreach ($s in $stats) { Write-Host "    $($s.Name): $($s.Count)" }

$ddlFiles = $classified | Where-Object { $_.HasDdl -and -not $_.AlreadyInInit }
$skippedFiles = $classified | Where-Object { $_.AlreadyInInit }
$dmlOnlyFiles = $classified | Where-Object { $_.Category -eq "DML_ONLY" }

Write-Host "`n  DDL to migrate (post-init.sql): $($ddlFiles.Count)"
foreach ($f in $ddlFiles) { Write-Host "    $($f.Entry.Name) [$($f.Category)]" }
Write-Host "  Skipped (already in init.sql): $($skippedFiles.Count)"
Write-Host "  DML-only (seed data): $($dmlOnlyFiles.Count)"

# Step 3: Generate migration files
Write-Host "`n=== Step 3: Generate Flyway migrations ==="
if (Test-Path $migrationDir) {
    Get-ChildItem -Path $migrationDir -Filter "*.sql" | Remove-Item -Force
} else {
    New-Item -ItemType Directory -Path $migrationDir -Force | Out-Null
}

# V000: baseline from init.sql DDL
Write-Host "  Generating V000__baseline_schema.sql ..."
$initLines = $initContent -split "`n"
$baselineParts = [System.Collections.ArrayList]::new()
[void]$baselineParts.Add("-- ============================================================")
[void]$baselineParts.Add("-- V000: Baseline schema extracted from init.sql")
[void]$baselineParts.Add("-- Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
[void]$baselineParts.Add("-- DDL only. Seed data remains in database/ directory.")
[void]$baselineParts.Add("-- ============================================================")
[void]$baselineParts.Add("")

$inInsertBlock = $false
foreach ($line in $initLines) {
    $trimmed = $line.Trim()
    if ($trimmed -match '^\s*/\*!') { continue }
    if ($trimmed -match '^\s*SET\s+') { continue }
    if ($trimmed -match '^\s*(LOCK\s+TABLES|INSERT\s+INTO)') { $inInsertBlock = $true; continue }
    if ($inInsertBlock) {
        if ($trimmed -match '^\s*(UNLOCK|CREATE|DROP|--\s*Table structure)') { $inInsertBlock = $false }
        else { continue }
    }
    if ($trimmed -match 'ALTER\s+TABLE.*(?:DISABLE|ENABLE)\s+KEYS') { continue }
    [void]$baselineParts.Add($line)
}
$baselineContent = $baselineParts -join "`n"
$baselineFile = "$migrationDir\V000__baseline_schema.sql"
Set-Content -Path $baselineFile -Value $baselineContent -Encoding UTF8 -NoNewline
$tableCount = ($baselineParts | Where-Object { $_ -match 'CREATE TABLE' }).Count
Write-Host "    Done: $tableCount CREATE TABLE statements"

# V001+: incremental DDL
$flywaySeq = 1
$migrationFiles = @()
$sortedDdl = $ddlFiles | Sort-Object { $_.Entry.Version }, { $_.Entry.Name }

foreach ($item in $sortedDdl) {
    $flywayVer = "V{0:D3}" -f $flywaySeq
    $flywaySeq++
    $baseName = $item.Entry.Name -replace '^v\d+_?', '' -replace '\.sql$', ''
    $desc = $baseName -replace '[^a-zA-Z0-9_]', '_' -replace '_+', '_' -replace '^_|_$', ''
    $filename = "${flywayVer}__${desc}.sql"
    $filepath = "$migrationDir\$filename"
    $content = Get-Content -Path $item.Entry.File.FullName -Raw -Encoding UTF8

    if ($item.Category -eq "MIXED") {
        $ddlParts = [System.Collections.ArrayList]::new()
        [void]$ddlParts.Add("-- Original file: $($item.Entry.Name) (DDL extracted from MIXED file)")
        [void]$ddlParts.Add("-- ============================================================")
        [void]$ddlParts.Add("")
        $fileLines = $content -split "`n"
        $inDmlBlock = $false
        foreach ($line in $fileLines) {
            $t = $line.Trim()
            if ($t -match '^\s*(INSERT\s+INTO|SET\s+@|LOCK\s+TABLES|UNLOCK\s+TABLES)') { $inDmlBlock = $true; continue }
            if ($inDmlBlock) {
                if ($t -match '^\s*(UNLOCK|CREATE|ALTER|DROP|--\s*---)') { $inDmlBlock = $false }
                else { continue }
            }
            if ($t -match 'ALTER\s+TABLE.*(?:DISABLE|ENABLE)\s+KEYS') { continue }
            [void]$ddlParts.Add($line)
        }
        $content = $ddlParts -join "`n"
    }

    Set-Content -Path $filepath -Value $content -Encoding UTF8 -NoNewline
    $migrationFiles += [PSCustomObject]@{
        FlywayVersion = $flywayVer
        Filename      = $filename
        OriginalFile  = $item.Entry.Name
        Category      = $item.Category
    }
    Write-Host "  $flywayVer -> $filename"
}

# Step 4: Generate mapping doc
Write-Host "`n=== Step 4: Generate mapping doc ==="
$mapLines = [System.Collections.ArrayList]::new()
[void]$mapLines.Add("# Flyway Migration Mapping")
[void]$mapLines.Add("")
[void]$mapLines.Add("| Flyway | Filename | Original | Category |")
[void]$mapLines.Add("|--------|----------|----------|----------|")
[void]$mapLines.Add("| V000 | V000__baseline_schema.sql | init.sql | BASELINE |")
foreach ($mf in $migrationFiles) {
    [void]$mapLines.Add("| $($mf.FlywayVersion) | $($mf.Filename) | $($mf.OriginalFile) | $($mf.Category) |")
}
[void]$mapLines.Add("")
[void]$mapLines.Add("Total migrations: $($migrationFiles.Count + 1)")
Set-Content -Path "$migrationDir\MIGRATION_MAPPING.md" -Value ($mapLines -join "`n") -Encoding UTF8

Write-Host "`n=== Done ==="
Write-Host "Migration dir: $migrationDir"
Write-Host "Total files: $($migrationFiles.Count + 1) (1 baseline + $($migrationFiles.Count) incremental)"
