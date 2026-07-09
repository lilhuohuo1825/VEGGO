import os
import urllib.request
import ssl

logos = {
    "vcb": "https://api.vietqr.io/img/VCB.png",
    "tcb": "https://api.vietqr.io/img/TCB.png",
    "mb": "https://api.vietqr.io/img/MB.png",
    "bidv": "https://api.vietqr.io/img/BIDV.png",
    "ctg": "https://api.vietqr.io/img/CTG.png"
}

output_dir = "/Users/lilhuohuo/Downloads/App/VEGGO-1/app/src/main/res/drawable"

if not os.path.exists(output_dir):
    os.makedirs(output_dir)

context = ssl._create_unverified_context()

for bank, url in logos.items():
    dest_path = os.path.join(output_dir, f"logo_{bank}.png")
    try:
        print(f"Downloading {bank} logo from {url}...")
        # Use urlopen and copy to avoid deprecation warning
        with urllib.request.urlopen(url, context=context) as response:
            with open(dest_path, "wb") as out_file:
                out_file.write(response.read())
        print(f"Successfully saved to {dest_path}")
    except Exception as e:
        print(f"Error downloading {bank} logo: {e}")
