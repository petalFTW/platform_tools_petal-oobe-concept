#!/usr/bin/env python3
"""Package the browser concept into one offline HTML file."""

import base64
import re
from pathlib import Path


def main():
    root = Path(__file__).resolve().parent
    html = (root / "index.html").read_text()
    css = (root / "styles.css").read_text().replace("</style", "<\\/style")
    script = (root / "app.js").read_text().replace("</script", "<\\/script")
    logo = "data:image/png;base64," + base64.b64encode(
        (root / "assets/petal-logo.png").read_bytes()
    ).decode("ascii")
    html = re.sub(r'  <link rel="stylesheet" href="styles\.css\?v=\d+">',
                  lambda _: f"  <style>\n{css}\n  </style>", html)
    html = re.sub(r'  <script src="app\.js\?v=\d+" defer></script>\n', "", html)
    html = html.replace("</body>", f"  <script>\n{script}\n  </script>\n</body>")
    html = html.replace("assets/petal-logo.png", logo)
    output = root / "petalos-setup.html"
    output.write_text(html)
    print(output)


if __name__ == "__main__":
    main()
