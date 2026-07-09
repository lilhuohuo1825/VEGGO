import os
import urllib.request
import ssl

images = {
    "ic_tree_border_only": "https://cdn-icons-png.flaticon.com/512/3043/3043511.png",
    "ic_qr_receive_border_only": "https://cdn-icons-png.flaticon.com/512/3126/3126504.png",
    "ic_tx_send": "https://cdn-icons-png.flaticon.com/512/109/109617.png",
    "ic_tx_receive": "https://cdn-icons-png.flaticon.com/512/109/109626.png"
}

output_dir = "/Users/lilhuohuo/Downloads/App/VEGGO-1/app/src/main/res/drawable"
context = ssl._create_unverified_context()

for name, url in images.items():
    dest_path = os.path.join(output_dir, f"{name}.png")
    try:
        print(f"Downloading {name} from {url}...")
        with urllib.request.urlopen(url, context=context) as response:
            with open(dest_path, "wb") as out_file:
                out_file.write(response.read())
        print(f"Successfully saved to {dest_path}")
    except Exception as e:
        print(f"Error downloading {name}: {e}")
