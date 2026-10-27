#!/usr/bin/env python3
"""Собирает app/src/main/assets/index.html из web/index.html.

web/index.html — исходник прототипа (тот же файл, что публикуется как артефакт).
Для APK он превращается в самостоятельную страницу:
  * полноценный <!doctype html> с viewport-fit=cover и базовым сбросом стилей;
  * шрифты Onest и Press Start 2P из assets/fonts вместо Google Fonts (работает офлайн);
  * env(safe-area-inset-*) заменяются на CSS-переменные --sa-*, которые
    выставляет MainActivity по реальным отступам системных панелей.
Запуск: python3 tools/make_assets.py
"""
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC = ROOT / "web" / "index.html"
DST = ROOT / "app" / "src" / "main" / "assets" / "index.html"

CYR = "U+0301,U+0400-045F,U+0490-0491,U+04B0-04B1,U+2116"
LAT = ("U+0000-00FF,U+0131,U+0152-0153,U+02BB-02BC,U+02C6,U+02DA,U+02DC,U+0304,U+0308,"
       "U+0329,U+2000-206F,U+20AC,U+2122,U+2191,U+2193,U+2212,U+2215,U+FEFF,U+FFFD")


def face(family, file, weight, rng):
    return (f"@font-face{{font-family:'{family}';font-style:normal;font-display:swap;"
            f"font-weight:{weight};src:url(fonts/{file}) format('woff2');unicode-range:{rng}}}")


def fonts_css():
    rules = []
    for w in (400, 500, 600, 700):
        rules.append(face("Onest", f"onest-cyrillic-{w}-normal.woff2", w, CYR))
        rules.append(face("Onest", f"onest-latin-{w}-normal.woff2", w, LAT))
    rules.append(face("Press Start 2P", "press-start-2p-cyrillic-400-normal.woff2", 400, CYR))
    rules.append(face("Press Start 2P", "press-start-2p-latin-400-normal.woff2", 400, LAT))
    return "\n".join(rules)


def main():
    html = SRC.read_text(encoding="utf-8")

    # убрать подключение Google Fonts и служебный meta артефакта
    html = re.sub(r'<link rel="preconnect"[^>]*>\n?', "", html)
    html = re.sub(r'<link rel="stylesheet" href="https://fonts\.googleapis\.com[^>]*>\n?', "", html)
    html = re.sub(r'<meta name="claude:[^>]*>\n?', "", html)

    # отступы системных панелей приходят из Android как --sa-top/right/bottom/left
    html = re.sub(r"env\(safe-area-inset-(top|right|bottom|left),\s*0px\)", r"var(--sa-\1,0px)", html)

    title = re.search(r"<title>.*?</title>", html).group(0)
    body = html.replace(title, "", 1)

    head = f"""<!doctype html>
<html lang="ru">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
{title}
<style>
{fonts_css()}
:root{{color-scheme:light;padding-top:var(--sa-top,0px);padding-bottom:var(--sa-bottom,0px);-webkit-tap-highlight-color:transparent}}
body{{margin:0;font-size:14px;overscroll-behavior:none}}
img{{max-width:100%}}
[hidden]{{display:none!important}}
</style>
</head>
<body>
"""
    out = head + body.strip() + "\n</body>\n</html>\n"
    DST.parent.mkdir(parents=True, exist_ok=True)
    DST.write_text(out, encoding="utf-8")
    print(f"{DST.relative_to(ROOT)}: {len(out.encode('utf-8')):,} байт")


if __name__ == "__main__":
    main()
