from PIL import Image
import os

img_path = "/Users/lilhuohuo/.gemini/antigravity/brain/56068765-d4f4-44ec-87ae-e1451ec275bf/media__1783373006539.jpg"
if not os.path.exists(img_path):
    # Try the other one if the latest is missing for some reason
    img_path = "/Users/lilhuohuo/.gemini/antigravity/brain/56068765-d4f4-44ec-87ae-e1451ec275bf/media__1783372522296.jpg"

if not os.path.exists(img_path):
    print("File not found")
    exit()

img = Image.open(img_path)
width, height = img.size
print(f"Image dimensions: {width}x{height}")

# Split horizontally into 5 parts
part_width = width // 5
drawable_dir = "/Users/lilhuohuo/Downloads/App/VEGGO-1/app/src/main/res/drawable"

for i in range(5):
    left = i * part_width
    right = (i + 1) * part_width
    box = (left, 0, right, height)
    part = img.crop(box)
    
    # Save as ic_tree_stage_1.png to ic_tree_stage_5.png
    dest_path = os.path.join(drawable_dir, f"ic_tree_stage_{i+1}.png")
    part.save(dest_path)
    print(f"Saved stage {i+1} to {dest_path}")
