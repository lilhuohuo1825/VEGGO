import os
import time

dir_path = "/Users/lilhuohuo/.gemini/antigravity/brain/56068765-d4f4-44ec-87ae-e1451ec275bf"
files = [os.path.join(dir_path, f) for f in os.listdir(dir_path) if f.startswith("media__")]
files.sort(key=os.path.getmtime, reverse=True)

print("Latest media files:")
for f in files[:5]:
    mtime = time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(os.path.getmtime(f)))
    print(f"{os.path.basename(f)} - Modified: {mtime} - Size: {os.path.getsize(f)} bytes")
