# Java RTSP recorder

> **🚀 Multi-Architecture Support:** This application supports Docker images for the 6 most common platforms:
> - **linux/amd64** - Standard x86_64 (Intel/AMD processors)
> - **linux/arm64** - ARM 64-bit (Orange Pi 5 Max, Raspberry Pi 4/5, Apple Silicon)
> - **linux/arm64/v8** - ARMv8 64-bit (explicit ARMv8 support for maximum compatibility)
> - **linux/arm/v7** - ARM 32-bit v7 (Raspberry Pi 3/4 in 32-bit mode)
> - **linux/arm/v6** - ARM 32-bit v6 (Raspberry Pi Zero/1)
> - **linux/386** - x86 32-bit (legacy systems)

## docker-compose.yaml

run `mkdir -p java-rtsp-recorder/{config,data/{tmp,records}}` before running docker compose.

`rclone.conf` and `config.yaml` must be placed in `java-rtsp-recorder/config` folder.

The image now ships with an ffmpeg build that can run in **copy**, **CPU**, **NVIDIA**, or **Radeon/VAAPI** modes. All supported modes keep the existing segment-based file rotation.

```yaml
version: "3.9"
services:
  java-rtsp-recorder:
    container_name: java-rtsp-recorder
    build:
      context: .
      dockerfile: dockerfile
    image: java-rtsp-recorder:local
    restart: always
    network_mode: host
    environment:
      TZ: America/Bahia
    volumes:
      - ./java-rtsp-recorder/config:/app/config
      - ./java-rtsp-recorder/data:/app/data
```

### Optional runtime additions

Use one of the following only when you want GPU access inside the container:

| Mode | Extra compose settings |
| --- | --- |
| CPU only | No extra settings |
| NVIDIA | `runtime: nvidia` plus `NVIDIA_VISIBLE_DEVICES=all` and `NVIDIA_DRIVER_CAPABILITIES=compute,video,utility` |
| Radeon / VAAPI | `devices: - /dev/dri:/dev/dri` |

## Docker run

```bash
docker build -f dockerfile -t java-rtsp-recorder:local .

docker run -d \
  --name java-rtsp-recorder \
  --restart always \
  --network host \
  -e TZ=America/Bahia \
  -v ./java-rtsp-recorder/config:/app/config \
  -v ./java-rtsp-recorder/data:/app/data \
  java-rtsp-recorder:local
```

### GPU-aware `docker run` examples

```bash
# NVIDIA
docker run -d \
  --name java-rtsp-recorder \
  --restart always \
  --network host \
  --runtime nvidia \
  -e TZ=America/Bahia \
  -e NVIDIA_VISIBLE_DEVICES=all \
  -e NVIDIA_DRIVER_CAPABILITIES=compute,video,utility \
  -v ./java-rtsp-recorder/config:/app/config \
  -v ./java-rtsp-recorder/data:/app/data \
  java-rtsp-recorder:local

# Radeon / VAAPI
docker run -d \
  --name java-rtsp-recorder \
  --restart always \
  --network host \
  --device /dev/dri:/dev/dri \
  -e TZ=America/Bahia \
  -v ./java-rtsp-recorder/config:/app/config \
  -v ./java-rtsp-recorder/data:/app/data \
  java-rtsp-recorder:local
```

## config.yaml
```yaml
general:
  timezone: "America/Bahia" # default value is container locale
  locale: 'pt-BR' # default value is 'en-GB'

telegram:
  chat_id: '-1000000000000' # mandatory
  bot_token: '0000000000:AAAbO1-dysu3daUTpjVEXX4Pwq-M9wrPT3' # mandatory

rclone:
  config-location: '/app/config/rclone.conf' # default value is '/app/config/rclone.conf'
  delete-cron: '0 0 1 * * *' # default value is '0 0 0 * * *' (midnight)
  rmdirs-cron: '0 10 1 * * *' # default value is '0 10 0 * * *' (10 minutes past 1am)
  dedupe-cron: '0 20 1 * * *' # default value is '0 20 0 * * *' (20 minutes past 1am)
  sync-cron: '0 */10 * * * *' # default value is '0 */10 * * * *' (every 10 minutes)
  transfer-method: 'copy' # default value is 'copy'
  destination-folder: 'drive-name:/destination/folder' # mandatory
  exclude-patterns:
    - '*.index' # List of patterns to exclude from sync
  ignore-existing: true # Ignore sync if destination file exists

storage:
  delete-old-files-cron: '0 30 1 * * *' # default value is '0 20 0 * * *' (30 minutes past 1am)
  max-records-folder-size: 20GB # default value is 10GB
  max-age-remote-video-files: 20d # default value is 20 days
  file-mover-interval: 5m # default value is 5 minutes
  tmp-folder: '/app/data/tmp' # default value is '/app/data/tmp' (container tmp folder)
  records-folder: '/app/data/records' # default value is '/app/data/records' (container records folder)

rtsp:
  timeout: 30s # default value is 30 seconds
  video-duration: 5m # default value is 5 minutes
  hardware-acceleration: 'copy' # valid values: 'copy', 'cpu', 'nvidia', 'radeon' ('none' is still accepted as a legacy alias for 'copy')
  vaapi-device: '/dev/dri/renderD128' # default Radeon/VAAPI render node
  cameras: # mandatory. List of cameras to record
    - name: 'hallway'
      host: '192.168.0.1'
      protocol: 'udp' # default value is 'tcp'
      port: 554
      format: 'onvif1'
      username: 'admin'
      password: 'password'
    - name: 'front-door'
      host: '192.168.0.2'
      protocol: 'udp' # default value is 'tcp'
      port: 554
      format: 'onvif1'
      username: 'admin'
      password: 'password'
    - name: 'backyard'
      url: 'rtsp://admin:password@192.168.0.3:554/onvif1' # if camera is configured with URL all other fields are ignored
```

`rtsp.hardware-acceleration` controls the encoder ffmpeg uses:

| Value | ffmpeg path |
| --- | --- |
| `copy` | legacy `-c copy` remuxing |
| `cpu` | `libx264` on CPU |
| `nvidia` | `h264_nvenc` on NVIDIA GPU |
| `radeon` | `h264_vaapi` on `/dev/dri/renderD128` |

If the field is omitted, the application falls back to `copy`. The older value `none` is also accepted and behaves the same as `copy`.

For your current Frigate deployment, set `hardware-acceleration: nvidia` in `java-rtsp-recorder/config/config.yaml`.
