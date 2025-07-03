import os
import nbtlib
from nbtlib.tag import String

def change_leaves_to_persistent(nbt_path):
    try:
        structure = nbtlib.load(nbt_path)
        palette = structure["palette"]
        changed = False

        for block in palette:
            name = block.get("Name", "")
            if "leaves" in name:
                props = block.setdefault("Properties", {})
                updated = False

                # Set persistent to true
                if props.get("persistent") != "true":
                    props["persistent"] = String("true")
                    updated = True

                # Set decayable to false
                if props.get("decayable") != "false":
                    props["decayable"] = String("false")
                    updated = True

                if updated:
                    print(f"[UPDATED] {name} in {nbt_path}")
                    changed = True

        if changed:
            structure.save(nbt_path)
        else:
            print(f"[OK] No change in {nbt_path}")

    except Exception as e:
        print(f"[ERROR] {nbt_path} : {e}")

def search_nbt_and_change(folder):
    for root, _, files in os.walk(folder):
        for file in files:
            if file.endswith(".nbt"):
                full_path = os.path.join(root, file)
                change_leaves_to_persistent(full_path)

if __name__ == "__main__":
    target_folder = input("folder path where .nbt files are : ").strip()
    if os.path.isdir(target_folder):
        search_nbt_and_change(target_folder)
    else:
        print("Invalid folder.")
