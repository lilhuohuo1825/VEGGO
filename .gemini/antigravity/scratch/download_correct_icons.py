import os
import urllib.request
import ssl

images = {
    "ic_donate_outline": "https://cdn-icons-png.flaticon.com/512/5761/5761921.png",
    "ic_qr_receive_outline": "https://cdn-icons-png.flaticon.com/512/714/714067.png",
    "ic_seed": "https://cdn-icons-png.flaticon.com/512/3076/3076416.png",
    "ic_small_tree": "https://cdn-icons-png.flaticon.com/512/2880/2880894.png",
    "ic_big_tree": "https://cdn-icons-png.flaticon.com/512/3591/3591456.png"
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
