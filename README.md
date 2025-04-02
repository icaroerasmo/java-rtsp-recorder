# Java RTSP recorder

## docker-compose.yaml

run `mkdir -p java-rtsp-recorder/{config,data/{tmp,records}}` before running docker compose.

`rclone.conf` and `config.yaml` must be placed in `java-rtsp-recorder/config` folder.

```yaml
version: "3.9"
services:
  java-rtsp-recorder:
    container_name: java-rtsp-recorder
    image: ghcr.io/icaroerasmo/java-rtsp-recorder:latest
    restart: always
    network_mode: host
    environment:
        TZ: America/Bahia
    volumes:
      - ./java-rtsp-recorder/config:/app/config
      - ./java-rtsp-recorder/data:/app/data
```

## Docker run

```bash
docker run -d \
  --name java-rtsp-recorder \
  --restart always \
  --network host \
  -e TZ=America/Bahia \
  -v ./java-rtsp-recorder/config:/app/config \
  -v ./java-rtsp-recorder/data:/app/data \
  ghcr.io/icaroerasmo/java-rtsp-recorder:latest
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
  max-age-local-video-files: 3d # default value is 3 days
  file-mover-interval: 5m # default value is 5 minutes
  tmp-folder: '/app/data/tmp' # default value is '/app/data/tmp' (container tmp folder)
  records-folder: '/app/data/records' # default value is '/app/data/records' (container records folder)

rtsp:
  timeout: 30s # default value is 30 seconds
  video-duration: 5m # default value is 5 minutes
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
