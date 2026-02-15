# paperless-hook-bot

This a simple [Ktor](https://ktor.io/) server that receives document webhooks from [Paperless](https://github.com/paperless-ngx/paperless-ngx) and then patches that document via REST.

The primary purpose of this piece of software is to extract metadata from the filename via regular expressions and use them to patch the document metadata.

## Quick start

### Configuration

First, you have to create a `config.yaml` file (the name itself does not matter, but the yaml content does).

```yaml
authentication:
  basic:
    - username: user # pick some credentials you like
      password: pass
paperless:
  - url: https://your-paperless-instance-url
    token: your-paperless-api-token # create that in your Paperless profile tab
    rules:
      - when:
          filename: "(\\d+)_(\\d{4})_No\\.(\\d+)_bank_statement_(\\d{4})\\.(\\d{2})\\.(\\d{2})_\\d+\\.pdf"
        set:
          title: "$filename1 bank statement $filename2-$filename3"
          correspondent: "My Bank"
          documentType: "Bank Statement"
          created: "$filename4-$filename5-$filename6"
          custom:
            account: "My Bank $filename1"
          tags:
            - "banking"
```

This example config will match file names like `12345_2026_No.01_bank_statement_2026-01-28.pdf` (if the bank uses that filename pattern with the bank account number in the beginning), and set the title, correspondent, document type, created date, custom account, and tags. You can use placeholders in the values referencing the groups in the regex (`$filename1` is the first match group in the regular expression for the filename).

### Start the server

The easiest way to start the server is via Docker:
```bash
docker run \
  -v config.yaml:/config.yaml \
  -e CONFIG_FILE=/config.yaml \
  -p 8080:8080 \
  ghcr.io/norganos/paperless-hook-bot:main
```

or docker-compose:
```yaml
services:
  redis:
    image: ghcr.io/norganos/paperless-hook-bot:main
    ports:
      - "8080:8080"
    volumes:
      - ./config.yaml:/config.yaml
    environment:
      CONFIG_FILE: "/config.yaml"
```

### Configure Paperless

Typically, you create a workflow (preferably with the "document-created" trigger) and a webhook action. 

In the webhook action you enter a URL that points to the hook-server endpoint `/document-added` (e.g. `http://your-host:8080/document-added`).

The hook server expects a JSON payload with only the document URL in it, like this:
```json
{"document": "https://your-paperless-instance-url/documents/12345"}
```

To achieve this, you can enable both switches "Use parameters for webhook body" and "Send webhook payload as JSON" and add a parameter "document" with content ```{{doc_url}}```.

Alternatively, you can disable the switches "Use parameters for webhook body" and "Send webhook payload as JSON" and enter the JSON string directly in "Webhook body" field like ```{"document": "{{doc_url}}"}```. With this approach, you also have to add the Header "Content-Type: application/json" below.

Either way, you have to add a header "Authorization" with the correct Basic-Auth Value containing the credentials you defined in the config.yaml. (in our example it's `Basic dXNlcjpwYXNz` as we used `user:pass` as credentials).
