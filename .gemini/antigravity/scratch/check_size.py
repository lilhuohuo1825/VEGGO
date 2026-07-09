import os
from PIL import Image

original_path = "/Users/lilhuohuo/.gemini/antigravity/brain/56068765-d4f4-44ec-87ae-e1451ec275bf/media__1783360222161.jpg"
if os.path.exists(original_path):
    img = Image.open(original_path)
    print("Dimensions:", img.size)
else:
    print("Not found")
