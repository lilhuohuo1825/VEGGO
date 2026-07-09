import os
from PIL import Image

original_path = "/Users/lilhuohuo/.gemini/antigravity/brain/56068765-d4f4-44ec-87ae-e1451ec275bf/media__1783360222161.jpg"
output_full_path = "/Users/lilhuohuo/Downloads/App/VEGGO-1/app/src/main/res/drawable/ic_veggopay.png"
output_utility_path = "/Users/lilhuohuo/Downloads/App/VEGGO-1/app/src/main/res/drawable/ic_veggopay_utility.png"

if os.path.exists(original_path):
    # Load image and convert to RGBA
    img = Image.open(original_path).convert("RGBA")
    width, height = img.size
    
    # We want to identify the green background.
    # The green background color is roughly (58, 155, 52) or (76, 162, 54).
    # Let's write a helper to check if a pixel is green.
    def is_green(pixel):
        r, g, b, a = pixel
        # Green is dominant
        return g > 110 and g > r + 20 and g > b + 20

    # Let's perform a flood fill from the corners to find the background
    visited = set()
    queue = [(0, 0), (width - 1, 0), (0, height - 1), (width - 1, height - 1)]
    
    # Initialize corners
    for x, y in queue:
        visited.add((x, y))
        
    pixels = img.load()
    
    while queue:
        cx, cy = queue.pop(0)
        # Make background transparent
        pixels[cx, cy] = (0, 0, 0, 0)
        
        # Check 4-neighbors
        for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = cx + dx, cy + dy
            if 0 <= nx < width and 0 <= ny < height:
                if (nx, ny) not in visited:
                    if is_green(pixels[nx, ny]):
                        visited.add((nx, ny))
                        queue.append((nx, ny))

    # Save the full logo with "Pay" (and transparent background)
    img.save(output_full_path, "PNG")
    print("Saved full transparent logo to:", output_full_path)
    
    # Now create the utility version (no "Pay" text)
    # The "Pay" text is in the bottom area. Let's crop the wallet area.
    # Wallet is roughly in the top/middle. Let's crop from y=80 to y=640 and x=180 to x=840.
    # This keeps a 660x560 box. Let's center it in a square.
    wallet_box = img.crop((180, 80, 840, 640))
    
    # Create a new square transparent image of size 660x660
    utility_img = Image.new("RGBA", (660, 660), (0, 0, 0, 0))
    # Paste the cropped wallet centered vertically
    utility_img.paste(wallet_box, (0, 50))
    
    utility_img.save(output_utility_path, "PNG")
    print("Saved utility logo (wallet only) to:", output_utility_path)

else:
    print("Original media not found")
