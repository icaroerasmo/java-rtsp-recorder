# GPU Acceleration

This document explains how to select the recording hardware acceleration,
covering **NVIDIA (NVENC)** and **AMD Radeon (VA-API)**, plus the CPU and
stream-copy modes. It describes what must be installed on the host, how the GPU
must be exposed to the container, and the relevant configuration.

## Image

```text
ghcr.io/icaroerasmo/java-rtsp-recorder:latest
```

The image is based on `archlinux` and bundles `ffmpeg` with the `h264_nvenc`,
`h264_vaapi` and `libx264` encoders compiled in.

## Acceleration modes

The recorder re-encodes (or copies) each camera stream according to
`rtsp.hardware-acceleration`:

| Value | Decode | Encode | GPU required |
|---|---|---|---|
| `copy` | none | stream copy (`-c copy`) | no |
| `cpu` | software | `libx264` | no |
| `nvidia` | CUDA (`-hwaccel cuda`) | `h264_nvenc` | NVIDIA |
| `radeon` | VA-API (`-hwaccel vaapi`) | `h264_vaapi` | AMD Radeon |
| `auto` | detected | detected | auto-detected |

`none` is accepted as a legacy alias for `copy`. In `auto` mode the recorder
detects `/dev/nvidia0` (NVIDIA), then `/dev/dri/renderD128` (VA-API/Radeon), and
falls back to CPU when neither is present.

## NVIDIA (NVENC)

### Host requirements

1. Install the NVIDIA driver.
2. Install the container toolkit (`nvidia-container-toolkit`) so the GPU is
   exposed through the CDI `nvidia.com/gpu` device.

### Exposing the GPU to the container

```yaml
services:
  recorder:
    image: ghcr.io/icaroerasmo/java-rtsp-recorder:latest
    devices:
      - nvidia.com/gpu=all
```

### Configuration

```yaml
rtsp:
  hardware-acceleration: nvidia
```

## AMD Radeon (VA-API)

### Host requirements

No vendor toolkit is required. The recorder needs the DRM render node
(`/dev/dri/renderD128` by default) and the VA-API driver inside the image.

### Exposing the GPU to the container

```yaml
services:
  recorder:
    image: ghcr.io/icaroerasmo/java-rtsp-recorder:latest
    devices:
      - /dev/dri/renderD128:/dev/dri/renderD128
```

### Configuration

```yaml
rtsp:
  hardware-acceleration: radeon
  vaapi-device: /dev/dri/renderD128
```

> The published image does **not** currently install the VA-API driver
> (`libva-mesa-driver`). Enabling `radeon` requires rebuilding the image with
> that package added; otherwise the VA-API encoder fails to initialize. Until
> then, use `copy` (stream copy) or `cpu` (software) on AMD machines.

## CPU / stream copy (no GPU)

Omit the `devices` entry and use one of the no-GPU modes:

```yaml
rtsp:
  hardware-acceleration: copy   # stream copy, preserves the source codec
  # or
  # hardware-acceleration: cpu  # software re-encode to H.264 (libx264)
```

`copy` is the lightest option: it remuxes the source stream without
re-encoding, so the recorded codec matches the camera source. `cpu` re-encodes
to H.264 with `libx264` and consumes CPU.
