# SSL / TLS Certificate Setup

This guide explains how to set up HTTPS for the ecom platform on both local development machines and production VPS.

The nginx-proxy is already configured to serve HTTPS on port 443 and redirect HTTP → HTTPS. You only need to provide the certificate files.

## Certificate Files

The nginx-proxy expects two files mounted at `./certs/`:

| File | Description |
|------|-------------|
| `certs/cert.pem` | The public certificate (or fullchain) |
| `certs/key.pem` | The private key |

> [!CAUTION]
> The `certs/` directory is git-ignored. **Never commit private keys to version control.** Each environment generates its own certificates.

---

## Local Development (mkcert)

`mkcert` creates locally-trusted certificates. The browser will show a green padlock, but **only on the machine where you ran the setup**.

### 1. Install mkcert

```bash
# Option A: via apt
sudo apt install mkcert

# Option B: direct binary (if apt is broken)
curl -sL https://dl.filippo.io/mkcert/latest?for=linux/amd64 -o /tmp/mkcert
chmod +x /tmp/mkcert
sudo cp /tmp/mkcert /usr/local/bin/mkcert
```

### 2. Install the local CA (one-time per machine)

```bash
mkcert -install
```

This installs a local Certificate Authority into your browser's trust store. You only need to do this once per device.

### 3. Generate certificates

```bash
cd /path/to/ecom
mkdir -p certs
mkcert -cert-file ./certs/cert.pem -key-file ./certs/key.pem \
  localhost 127.0.0.1 ::1 \
  tafsir.therapdev.net
```

Add any other hostnames from `/etc/hosts` that you want to access via HTTPS.

### 4. Run the app

```bash
docker compose up -d
```

- `http://localhost` → redirects to `https://localhost` 🔒
- `https://tafsir.therapdev.net` → works with green padlock 🔒

> [!NOTE]
> mkcert certificates expire after ~3 years. If your cert expires, just regenerate it with the same command.

---

## Production VPS (Let's Encrypt)

Let's Encrypt provides **free, publicly-trusted** certificates that work in all browsers worldwide. They auto-renew every 90 days.

### Prerequisites

- A **domain name** pointing to the VPS's public IP (A record in DNS)
- Ports **80** and **443** open on the VPS firewall
- No other service using port 80 during cert generation

### 1. Install Certbot

```bash
sudo apt update
sudo apt install certbot
```

### 2. Generate the certificate

Stop the app first (certbot needs port 80):

```bash
docker compose down

sudo certbot certonly --standalone -d yourdomain.com
```

Certs are saved to `/etc/letsencrypt/live/yourdomain.com/`.

### 3. Copy or symlink certs to the project

```bash
mkdir -p certs
sudo cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem ./certs/cert.pem
sudo cp /etc/letsencrypt/live/yourdomain.com/privkey.pem ./certs/key.pem
sudo chown $USER:$USER ./certs/*.pem
```

### 4. Start the app

```bash
docker compose up -d
```

### 5. Set up auto-renewal

Let's Encrypt certs expire every 90 days. Add a cron job to renew and copy:

```bash
sudo crontab -e
```

Add this line:

```cron
0 3 1 */2 * certbot renew --pre-hook "cd /path/to/ecom && docker compose stop nginx-proxy" --post-hook "cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem /path/to/ecom/certs/cert.pem && cp /etc/letsencrypt/live/yourdomain.com/privkey.pem /path/to/ecom/certs/key.pem && cd /path/to/ecom && docker compose start nginx-proxy"
```

> [!TIP]
> Alternatively, mount the Let's Encrypt directory directly in `docker-compose.yml` instead of copying:
> ```yaml
> volumes:
>   - /etc/letsencrypt/live/yourdomain.com/fullchain.pem:/etc/nginx/certs/cert.pem:ro
>   - /etc/letsencrypt/live/yourdomain.com/privkey.pem:/etc/nginx/certs/key.pem:ro
> ```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Browser shows "Not Secure" on localhost | Run `mkcert -install` and restart browser |
| `NET::ERR_CERT_AUTHORITY_INVALID` | The local CA isn't installed — run `mkcert -install` |
| Certificate doesn't match domain | Regenerate cert with the correct domain in the `mkcert` command |
| Port 443 already in use | Stop the other service using port 443 (`docker ps` to check) |
| Certbot fails on VPS | Ensure DNS A record points to the VPS IP and port 80 is open |
