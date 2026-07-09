import os
import urllib.request
import ssl

images = {
    "ic_seed": "https://cdn-icons-png.flaticon.com/512/3655/3655607.png",
    "ic_small_tree": "https://cdn-icons-png.flaticon.com/512/489/489969.png",
    "ic_big_tree": "https://cdn-icons-png.flaticon.com/512/3105/3105026.png",
    "ic_watering_can": "https://cdn-icons-png.flaticon.com/512/4148/4148425.png"
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
