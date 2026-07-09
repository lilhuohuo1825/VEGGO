import os
from PIL import Image

img_path = "/Users/lilhuohuo/.gemini/antigravity/brain/56068765-d4f4-44ec-87ae-e1451ec275bf/media__1783373006539.jpg"
if not os.path.exists(img_path):
    img_path = "/Users/lilhuohuo/.gemini/antigravity/brain/56068765-d4f4-44ec-87ae-e1451ec275bf/media__1783372522296.jpg"

if not os.path.exists(img_path):
    print("File not found")
    exit()

img = Image.open(img_path).convert("RGBA")
width, height = img.size

# Process pixels to make the grey/white checkerboard transparent
pixdata = img.load()
for y in range(height):
    for x in range(width):
        r, g, b, a = pixdata[x, y]
        # Checkerboard squares are white/light-grey.
        # Let's check if R, G, B are close to each other (greyish/white) and bright.
        is_white_or_grey = (r > 200 and g > 200 and b > 200) and (abs(r - g) < 15 and abs(g - b) < 15 and abs(r - b) < 15)
        # Also catch pure white or very light grays that might be slightly off
        if is_white_or_grey or (r > 230 and g > 230 and b > 230):
            pixdata[x, y] = (255, 255, 255, 0) # transparent

# Now crop the transparent image into 5 equal parts
part_width = width // 5
drawable_dir = "/Users/lilhuohuo/Downloads/App/VEGGO-1/app/src/main/res/drawable"

for i in range(5):
    left = i * part_width
    right = (i + 1) * part_width
    box = (left, 0, right, height)
    part = img.crop(box)
    
    # Save as ic_tree_stage_1.png to ic_tree_stage_5.png
    dest_path = os.path.join(drawable_dir, f"ic_tree_stage_{i+1}.png")
    part.save(dest_path, "PNG")
    print(f"Saved stage {i+1} to {dest_path}")
