#!/usr/bin/env python3
"""Check the selectable presentation's source wiring, without an Android build."""

from pathlib import Path
import hashlib
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
MODULE = ROOT / "packages/apps/SetupWizard"
ANDROID = "{http://schemas.android.com/apk/res/android}"
WIZARD = "{http://schemas.android.com/apk/res/com.google.android.setupwizard}"
PACKAGE = "org.lineageos.setupwizard"


def definitions(directory):
    names = set()
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
                names.add((tag, name.replace(".", "_")))
                if tag == "declare-styleable":
                    names.add(("styleable", name))
                    for attr in item:
                        names.add(("styleable", name + "_" + attr.get("name", "").replace(":", "_")))
        for element in tree.iter():
            for value in element.attrib.values():
                if value.startswith("@+id/"):
                    names.add(("id", value.split("/", 1)[1]))
    return names


def layout_ids(name):
    tree = ET.parse(MODULE / "res/layout" / (name + ".xml"))
    return {value.split("/", 1)[1] for element in tree.iter()
            for key, value in element.attrib.items()
            if key == ANDROID + "id" and re.match(r"@\+?id/", value)}


def main():
    errors = []

    def require(condition, message):
        if not condition:
            errors.append(message)

    manifest = ET.parse(MODULE / "AndroidManifest.xml").getroot()
    require(manifest.get("package") == PACKAGE, "SetupWizard application identity changed")
    require(manifest.get(ANDROID + "sharedUserId") == "android.uid.system", "Shared UID changed")
    homes = [activity.get(ANDROID + "name") for activity in manifest.findall("application/activity")
             if any(category.get(ANDROID + "name") == "android.intent.category.HOME"
                    for category in activity.iter("category"))]
    require(homes == [".SetupWizardActivity"], f"Unexpected HOME components: {homes}")
    allowlist = ET.parse(MODULE / "org.lineageos.setupwizard.xml").getroot()
    require(allowlist.find("privapp-permissions").get("package") == PACKAGE,
            "Allowlist/package mismatch")
    values = ET.parse(MODULE / "res/values/petal_setup.xml").getroot()
    require(values.find("bool[@name='config_use_petal_setup']").text == "true",
            "Petal must be the default presentation")
    for product in (ROOT / "device/oneplus").glob("*/petal_*.mk"):
        require(not re.search(r"^\s*PetalBackup\s*(?:\\|$)", product.read_text(), re.M),
                f"Backup was bundled into a ROM product before independent validation: {product}")
    for build_file in (ROOT / "tools/petal-oobe-concept/native-port").rglob("Android.*"):
        require(build_file.name not in ("Android.bp", "Android.mk"),
                f"Active build declaration in the old archive: {build_file}")

    names = definitions(MODULE / "res")
    # Supplied by the AppCompat static dependency, not setupdesign's resource tree.
    names.add(("style", "Theme_AppCompat"))
    for library in ("external/setupcompat", "external/setupdesign"):
        for directory in (ROOT / library).rglob("res"):
            names |= definitions(directory)
    java = list((MODULE / "src").rglob("*.java"))
    for file in java:
        text = file.read_text()
        declared = re.search(r"^package ([\w.]+);", text, re.M).group(1)
        require(file == MODULE / "src" / Path(*declared.split(".")) / file.name,
                f"Java package/path mismatch: {file}")
        for kind, name in re.findall(r"(?<![\w.])R\.(\w+)\.(\w+)", text):
            require((kind, name) in names, f"Undefined R.{kind}.{name}: {file}")

    for file in (MODULE / "res").rglob("*.xml"):
        for element in ET.parse(file).iter():
            require(element.tag != "org.petal.setupwizard.PetalSetupLayout",
                    f"Old custom template reintroduced: {file}")
            for value in element.attrib.values():
                match = re.fullmatch(r"@(\w+)/([\w.]+)", value)
                if match:
                    kind, name = match.groups()
                    require((kind, name.replace(".", "_")) in names,
                            f"Undefined {value}: {file}")

    for selected in ("welcome_activity", "petal_welcome_activity"):
        require({"start", "skip", "emerg_dialer", "launch_accessibility", "welcome_title"}
                <= layout_ids(selected), f"Welcome controller IDs missing: {selected}")
    for selected in ("finish_activity", "petal_finish_activity"):
        require({"root", "linear_layout", "brand_logo", "navigation_bar"}
                <= layout_ids(selected), f"Finish controller IDs missing: {selected}")
    for selected in ("navigation_layout", "petal_navigation_layout"):
        require({"navbar_next", "navbar_skip"} <= layout_ids(selected),
                f"Navigation controller IDs missing: {selected}")

    actions = {element.get(ANDROID + "name") for element in manifest.iter("action")}
    for file in (MODULE / "res").rglob("lineage_wizard_script*.xml"):
        tree = ET.parse(file).getroot()
        steps = tree.findall("WizardAction")
        nodes = {step.get("id"): step for step in steps}
        require(len(nodes) == len(steps), f"Duplicate wizard node: {file}")
        require(tree.get(WIZARD + "firstAction") in nodes, f"Missing first action: {file}")
        for node in steps:
            for action in re.findall(r"action=([^;]+)", node.get(WIZARD + "uri", "")):
                if action.startswith(PACKAGE + "."):
                    require(action in actions, f"Undeclared script action {action}: {file}")
            for result in node.findall("result"):
                following = result.get(WIZARD + "action")
                require(not following or following in nodes, f"Dangling next step {following}: {file}")

    logo = MODULE / "res/drawable-nodpi/petal_setup_logo.png"
    source = ROOT / "vendor/petal/apps/PetalHub/res/drawable-nodpi/petal_logo.png"
    require(logo.exists(), "Missing Petal source logo")
    if logo.exists() and source.exists():
        require(hashlib.sha256(logo.read_bytes()).digest() == hashlib.sha256(source.read_bytes()).digest(),
                "Logo differs from the existing PetalHub asset")

    if errors:
        raise SystemExit("\n".join(sorted(set(errors))))
    print(f"PASS: {len(java)} Java source files; identity, HOME, default selection, resources, "
          "stock/Petal controller IDs, script wiring, archive isolation and logo match")
    print("Source wiring only: no Android type/resource compilation or device verification.")


if __name__ == "__main__":
    main()
