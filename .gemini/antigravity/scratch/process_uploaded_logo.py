import os
from PIL import Image

uploaded_path = "/Users/lilhuohuo/.gemini/antigravity/brain/56068765-d4f4-44ec-87ae-e1451ec275bf/media__1783363794645.png"
output_full_path = "/Users/lilhuohuo/Downloads/App/VEGGO-1/app/src/main/res/drawable/ic_veggopay.png"
output_utility_path = "/Users/lilhuohuo/Downloads/App/VEGGO-1/app/src/main/res/drawable/ic_veggopay_utility.png"

if os.path.exists(uploaded_path):
    # Load the uploaded transparent image
    img = Image.open(uploaded_path).convert("RGBA")
    width, height = img.size
    print("Dimensions of uploaded image:", width, height)
    
    # 1. Save directly as the full veggopay logo (ic_veggopay.png)
    img.save(output_full_path, "PNG")
    print("Saved transparent full logo to:", output_full_path)
    
    # 2. Crop the wallet portion only (ic_veggopay_utility.png)
    # The image is 1020x1020 or similar.
    # Wallet is in the top section. Let's crop from y=80 to y=640 and x=180 to x=840 (for 1024x1024).
    # Since dimensions might vary slightly, let's calculate ratios.
    # Let's crop:
    # x: 18% to 82%
    # y: 8% to 62%
    x0 = int(width * 0.18)
    y0 = int(height * 0.08)
    x1 = int(width * 0.82)
    y1 = int(height * 0.62)
    
    wallet_box = img.crop((x0, y0, x1, y1))
    
    # Create square container
    box_w = x1 - x0
    box_h = y1 - y0
    container_size = max(box_w, box_h)
    
    utility_img = Image.new("RGBA", (container_size, container_size), (0, 0, 0, 0))
    # Center the wallet box in the square
    paste_x = (container_size - box_w) // 2
    paste_y = (container_size - box_h) // 2
    utility_img.paste(wallet_box, (paste_x, paste_y))
    
    utility_img.save(output_utility_path, "PNG")
    print("Saved utility wallet logo to:", output_utility_path)
else:
    print("Uploaded image not found at path:", uploaded_path)
