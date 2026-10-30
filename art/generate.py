#!/usr/bin/env python3
"""Generate Station illustrations with a local ComfyUI (Z-Image Turbo). See docs/ART.md.

Usage: python3 art/generate.py [station ...] [--seeds N] [--seed S] [--url http://127.0.0.1:8188]
Writes art/raw/<station>_<seed>.png (git-ignored). Pick one, then run art/process.py.
"""
import argparse
import json
import random
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

HERE = Path(__file__).resolve().parent
RAW = HERE / "raw"


def workflow(prompt, width, height, seed, init=None, denoise=1.0):
    wf = {
        "1": {"class_type": "UNETLoader", "inputs": {"unet_name": "z_image_turbo_nvfp4.safetensors", "weight_dtype": "default"}},
        "2": {"class_type": "CLIPLoader", "inputs": {"clip_name": "qwen_3_4b_fp8_mixed.safetensors", "type": "lumina2"}},
        "3": {"class_type": "VAELoader", "inputs": {"vae_name": "z_image_ae.safetensors"}},
        "4": {"class_type": "CLIPTextEncode", "inputs": {"text": prompt, "clip": ["2", 0]}},
        "5": {"class_type": "ConditioningZeroOut", "inputs": {"conditioning": ["4", 0]}},
        "6": {"class_type": "EmptySD3LatentImage", "inputs": {"width": width, "height": height, "batch_size": 1}},
        "7": {"class_type": "ModelSamplingAuraFlow", "inputs": {"model": ["1", 0], "shift": 3}},
        "8": {"class_type": "KSampler", "inputs": {
            "model": ["7", 0], "positive": ["4", 0], "negative": ["5", 0], "latent_image": ["6", 0],
            "seed": seed, "steps": 8, "cfg": 1.0, "sampler_name": "res_multistep", "scheduler": "simple", "denoise": 1.0}},
        "9": {"class_type": "VAEDecode", "inputs": {"samples": ["8", 0], "vae": ["3", 0]}},
        "10": {"class_type": "SaveImage", "inputs": {"images": ["9", 0], "filename_prefix": "crst"}},
    }
    if init:
        # img2img: start from an uploaded picture instead of noise (the banner starts from the Onslaught sprite).
        wf["11"] = {"class_type": "LoadImage", "inputs": {"image": init}}
        wf["12"] = {"class_type": "VAEEncode", "inputs": {"pixels": ["11", 0], "vae": ["3", 0]}}
        wf["8"]["inputs"]["latent_image"] = ["12", 0]
        wf["8"]["inputs"]["denoise"] = denoise
    return wf


def upload(url, path):
    """Uploads an image to ComfyUI's input folder; returns the name LoadImage takes."""
    boundary = "----crst" + str(random.randrange(10**12))
    body = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"image\"; filename=\"{path.name}\"\r\n"
            f"Content-Type: image/png\r\n\r\n").encode() + path.read_bytes() + \
           f"\r\n--{boundary}\r\nContent-Disposition: form-data; name=\"overwrite\"\r\n\r\ntrue\r\n--{boundary}--\r\n".encode()
    req = urllib.request.Request(url + "/upload/image", data=body,
                                 headers={"Content-Type": f"multipart/form-data; boundary={boundary}"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.loads(r.read())["name"]


def call(url, path, data=None):
    req = urllib.request.Request(url + path, data=json.dumps(data).encode() if data else None,
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read()


def run(url, wf):
    pid = json.loads(call(url, "/prompt", {"prompt": wf}))["prompt_id"]
    while True:
        hist = json.loads(call(url, "/history/" + pid))
        if pid in hist:
            entry = hist[pid]
            if entry.get("status", {}).get("status_str") == "error":
                raise RuntimeError(json.dumps(entry["status"])[:800])
            img = entry["outputs"]["10"]["images"][0]
            q = urllib.parse.urlencode({"filename": img["filename"], "subfolder": img["subfolder"], "type": img["type"]})
            return call(url, "/view?" + q)
        time.sleep(1)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("stations", nargs="*")
    ap.add_argument("--seeds", type=int, default=1, help="variants per station")
    ap.add_argument("--seed", type=int, help="first seed (default random)")
    ap.add_argument("--url", default="http://127.0.0.1:8188")
    ap.add_argument("--denoise", type=float, help="img2img strength for entries with an init image")
    a = ap.parse_args()
    cfg = json.loads((HERE / "prompts.json").read_text())
    names = a.stations or list(cfg["stations"])
    RAW.mkdir(exist_ok=True)
    for name in names:
        st = cfg["stations"].get(name) or cfg.get("icons", {}).get(name)
        if st is None:
            sys.exit(f"unknown station {name}")
        # Entries with their own "prompt" (the forum banner) skip the shared Station style.
        prompt = st.get("prompt") or cfg["style"].format(accent=st["accent"]) + " " + st["subject"]
        width, height = st.get("w", cfg["size"]), st.get("h", cfg["size"])
        init = upload(a.url, HERE / st["init"]) if st.get("init") else None
        denoise = a.denoise if a.denoise is not None else st.get("denoise", 1.0)
        for i in range(a.seeds):
            seed = (a.seed + i) if a.seed is not None else random.randrange(2**31)
            t = time.time()
            png = run(a.url, workflow(prompt, width, height, seed, init, denoise))
            out = RAW / (f"{name}_{seed}.png" if init is None else f"{name}_{seed}_d{round(denoise * 100)}.png")
            out.write_bytes(png)
            print(f"{out.relative_to(HERE.parent)}  {time.time() - t:.1f}s", flush=True)


if __name__ == "__main__":
    main()
