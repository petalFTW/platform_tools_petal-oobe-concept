#!/usr/bin/env python3
"""Check native source wiring without invoking an Android build."""

from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
ANDROID = "{http://schemas.android.com/apk/res/android}"
WIZARD = "{http://schemas.android.com/apk/res/com.google.android.setupwizard}"
MODULES = [ROOT / "packages/apps/SetupWizard", ROOT / "vendor/petal/apps/PetalBackup"]


def definitions(directory):
    names = set()
    if not directory.exists():
        return names
    for file in directory.rglob("*"):
        if not file.is_file():
            continue
        kind = file.parent.name.split("-")[0]
        if kind != "values":
            names.add((kind, file.name.split(".")[0]))
        if file.suffix != ".xml":
            continue
        tree = ET.parse(file)
        if kind == "values":
            for item in tree.getroot():
                name = item.get("name")
                if not name:
                    continue
                tag = item.get("type", item.tag)
                names.add((tag, name))
                if tag == "declare-styleable":
                    names.add(("styleable", name))
                    for attr in item:
                        names.add(("styleable", name + "_" + attr.get("name", "").replace(":", "_")))
        for element in tree.iter():
            for value in element.attrib.values():
                if value.startswith("@+id/"):
                    names.add(("id", value.split("/", 1)[1]))
    return names


def main():
    errors = []
    library = set()
    for base in (ROOT / "external/setupdesign", ROOT / "external/setupcompat"):
        for directory in base.rglob("res"):
            library |= definitions(directory)
    for module in MODULES:
        manifest = ET.parse(module / "AndroidManifest.xml").getroot()
        package = manifest.get("package")
        if package not in ("org.petal.setupwizard", "org.petal.backup"):
            archive = ROOT / "tools/petal-oobe-concept/native-port"
            print(f"The archived identity-changing port is not applied: {package} in {module}")
            print(f"Archived copy lives in {archive}")
            print("For the current presentation-only implementation, run check-presentation.py")
            raise SystemExit(0)
        names = definitions(module / "res") | library
        java = list((module / "src").rglob("*.java"))
        for file in java:
            text = file.read_text()
            declared = re.search(r"^package ([\w.]+);", text, re.M).group(1)
            expected = module / "src" / Path(*declared.split(".")) / file.name
            if file != expected:
                errors.append(f"Package/path mismatch: {file}")
            for kind, name in re.findall(r"(?<![\w.])R\.(\w+)\.(\w+)", text):
                if (kind, name) not in names:
                    errors.append(f"Missing R.{kind}.{name}: {file}")
        for file in (module / "res").rglob("*.xml"):
            tree = ET.parse(file)
            for element in tree.iter():
                if element.tag.startswith("org.petal."):
                    target = module / "src" / Path(*element.tag.split("."))
                    if not target.with_suffix(".java").exists():
                        errors.append(f"Missing custom view {element.tag}: {file}")
        for activity in manifest.findall("application/activity"):
            name = activity.get(ANDROID + "name")
            full = package + name if name.startswith(".") else name
            path = module / "src" / Path(*full.split("."))
            if not path.with_suffix(".java").exists():
                errors.append(f"Missing manifest activity: {full}")
        for file in module.rglob("*"):
            if file.suffix not in (".java", ".xml", ".sh", ".bp", ".md") or ".git" in file.parts:
                continue
            text = file.read_text()
            if "org.lineageos.setupwizard" in text or "org.petalos.backup" in text:
                errors.append(f"Old package reference: {file}")
        print(f"Checked {package}: {len(java)} Java source files and native resources")

    wizard = MODULES[0]
    declared_actions = {action.get(ANDROID + "name") for action in ET.parse(
        wizard / "AndroidManifest.xml").iter("action")}
    for file in (wizard / "res").rglob("*wizard_script*.xml"):
        tree = ET.parse(file).getroot()
        actions = {element.get("id"): element for element in tree.findall("WizardAction")}
        for action in actions.values():
            uri = action.get(WIZARD + "uri", "")
            for name in re.findall(r"action=(org\.petal\.setupwizard\.[^;]+)", uri):
                if name not in declared_actions:
                    errors.append(f"Undeclared wizard action {name}: {file}")
            for result in action.findall("result"):
                following = result.get(WIZARD + "action")
                if following and following not in actions:
                    errors.append(f"Missing next step {following}: {file}")
        if file.name.startswith("lineage_wizard_script"):
            current = tree.get(WIZARD + "firstAction")
            route = []
            while current:
                if current in route:
                    errors.append(f"Wizard cycle {route + [current]}: {file}")
                    break
                route.append(current)
                current = next((result.get(WIZARD + "action")
                                for result in actions[current].findall("result")
                                if result.get(WIZARD + "name") is None), None)
            if "biometric_settings" in route:
                if "lockscreen_settings" not in route or route.index("lockscreen_settings") > route.index("biometric_settings"):
                    errors.append(f"Lock must precede biometrics: {file}")
            print(f"Wizard path {file.parent.name}/{file.name}: {' -> '.join(route)}")
    allowlist = ET.parse(wizard / "org.petal.setupwizard.xml").getroot()
    assert allowlist.find("privapp-permissions").get("package") == "org.petal.setupwizard"
    if errors:
        raise SystemExit("\n".join(sorted(set(errors))))
    print("PASS: resource IDs, Java package paths, manifest classes, namespace migration, and wizard graph checks")


if __name__ == "__main__":
    main()
