import os
from PIL import Image

dir_path = "/Users/lilhuohuo/.gemini/antigravity/brain/56068765-d4f4-44ec-87ae-e1451ec275bf"
files = [f for f in os.listdir(dir_path) if f.startswith("media__") and f.endswith(".jpg")]

for f in files:
    full_path = os.path.join(dir_path, f)
    try:
        img = Image.open(full_path)
        print(f"File: {f}, size: {img.size}, aspect_ratio: {img.size[0]/img.size[1]:.2f}")
    except Exception as e:
        print(f"Error reading {f}: {e}")
