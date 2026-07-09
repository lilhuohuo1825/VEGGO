import os
import urllib.request
import ssl

url = "https://cdn-icons-png.flaticon.com/512/95/95460.png"
dest_path = "/Users/lilhuohuo/Downloads/App/VEGGO-1/app/src/main/res/drawable/ic_barcode.png"
context = ssl._create_unverified_context()

try:
    print("Downloading barcode icon...")
    with urllib.request.urlopen(url, context=context) as response:
        with open(dest_path, "wb") as out_file:
            out_file.write(response.read())
    print("Successfully saved barcode icon")
except Exception as e:
    print(f"Error downloading barcode: {e}")
