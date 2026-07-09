import os
from PIL import Image

image_path = "/Users/lilhuohuo/Downloads/App/VEGGO-1/app/src/main/res/drawable/ic_veggopay.png"
if os.path.exists(image_path):
    img = Image.open(image_path).convert("RGBA")
    datas = img.getdata()
    
    newData = []
    for item in datas:
        # If pixel is white or very close to white, make it transparent
        # White is (255, 255, 255). We can check if all R, G, B are > 240
        if item[0] > 240 and item[1] > 240 and item[2] > 240:
            newData.append((255, 255, 255, 0)) # transparent
        else:
            newData.append(item)
            
    img.putdata(newData)
    img.save(image_path, "PNG")
    print("Successfully processed transparency for ic_veggopay.png")
else:
    print("File not found:", image_path)
