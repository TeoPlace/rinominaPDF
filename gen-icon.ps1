# rinominaPDF - genera l'icona: resources\icons\win\icon.ico (installer ed eseguibile) e i PNG in resources\icons\png.
# Stesso metodo di callApi e degli altri prodotti teo.place: System.Drawing, ICO multi-risoluzione con payload PNG.
# Il disegno e' quello della linea (quadrato arrotondato con gradiente + segno bianco); cambiano i colori e il segno.
# Uso: powershell -ExecutionPolicy Bypass -File .\gen-icon.ps1

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
Add-Type -AssemblyName System.Drawing

$colA = [System.Drawing.Color]::FromArgb(255, 22, 163, 74)
$colB = [System.Drawing.Color]::FromArgb(255, 16, 185, 129)

$res = Join-Path $PSScriptRoot "resources\icons"
$winDir = Join-Path $res "win"
$pngDir = Join-Path $res "png"
New-Item -ItemType Directory -Force $winDir, $pngDir | Out-Null

function New-Mark([int]$size) {
  $bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $g.Clear([System.Drawing.Color]::Transparent)
  $s = $size / 256.0

  $r = [int](56 * $s); $d = $r * 2
  $path = New-Object System.Drawing.Drawing2D.GraphicsPath
  $path.AddArc(0, 0, $d, $d, 180, 90)
  $path.AddArc($size - $d, 0, $d, $d, 270, 90)
  $path.AddArc($size - $d, $size - $d, $d, $d, 0, 90)
  $path.AddArc(0, $size - $d, $d, $d, 90, 90)
  $path.CloseFigure()
  $rect = New-Object System.Drawing.RectangleF(0, 0, $size, $size)
  $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush($rect, $colA, $colB,
      [System.Drawing.Drawing2D.LinearGradientMode]::ForwardDiagonal)
  $g.FillPath($brush, $path)

  # Il segno del prodotto: un foglio con un barcode
  $white = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
  $soft = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(150, 255, 255, 255))
  $g.FillRectangle($white, [single](70 * $s), [single](46 * $s), [single](116 * $s), [single](164 * $s))
  $ink = New-Object System.Drawing.SolidBrush($colB)
  foreach ($b in @(@(88, 6), @(100, 3), @(108, 9), @(122, 3), @(130, 6), @(142, 3), @(150, 9), @(164, 6))) {
    $g.FillRectangle($ink, [single]($b[0] * $s), [single](112 * $s), [single]($b[1] * $s), [single](70 * $s))
  }
  foreach ($y in @(66, 80, 94)) { $g.FillRectangle($soft, [single](88 * $s), [single]($y * $s), [single](82 * $s), [single](6 * $s)) }
  $g.FillRectangle($ink, [single](88 * $s), [single](66 * $s), [single](40 * $s), [single](6 * $s))


  $g.Dispose()
  return $bmp
}

foreach ($sz in @(16, 32, 48, 64, 128, 256, 512, 1024)) {
  $b = New-Mark $sz
  $b.Save((Join-Path $pngDir ("{0}x{0}.png" -f $sz)), [System.Drawing.Imaging.ImageFormat]::Png)
  $b.Dispose()
}

$sizes = @(16, 24, 32, 48, 64, 128, 256)
$pngs = @()
foreach ($sz in $sizes) {
  $b = New-Mark $sz
  $ms = New-Object System.IO.MemoryStream
  $b.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
  $pngs += ,($ms.ToArray())
  $ms.Dispose(); $b.Dispose()
}
$out = New-Object System.IO.MemoryStream
$bw = New-Object System.IO.BinaryWriter($out)
$bw.Write([uint16]0); $bw.Write([uint16]1); $bw.Write([uint16]$sizes.Count)
$offset = 6 + 16 * $sizes.Count
for ($i = 0; $i -lt $sizes.Count; $i++) {
  $sz = $sizes[$i]
  $bw.Write([byte]$(if ($sz -ge 256) { 0 } else { $sz }))
  $bw.Write([byte]$(if ($sz -ge 256) { 0 } else { $sz }))
  $bw.Write([byte]0); $bw.Write([byte]0)
  $bw.Write([uint16]1); $bw.Write([uint16]32)
  $bw.Write([uint32]$pngs[$i].Length); $bw.Write([uint32]$offset)
  $offset += $pngs[$i].Length
}
foreach ($p in $pngs) { $bw.Write($p) }
$bw.Flush()
$ico = Join-Path $winDir "icon.ico"
[System.IO.File]::WriteAllBytes($ico, $out.ToArray())
$bw.Dispose()

$test = New-Object System.Drawing.Icon($ico)
Write-Output ("icon.ico creata: {0}x{1}" -f $test.Width, $test.Height)
$test.Dispose()
