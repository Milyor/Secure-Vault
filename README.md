# Secure Vault API

A secure file vault REST API built with Flask that provides encrypted file storage for uploading, downloading, and managing files securely.

## Features

- **🔒 AES-256 Encryption**: Files are encrypted using AES-256-GCM (Galois/Counter Mode) for authenticated encryption
- **📤 File Upload**: Securely upload files with automatic encryption
- **📥 File Download**: Download files with automatic decryption
- **📋 File Management**: List and delete files in the vault
- **✅ File Validation**: File type and size validation
- **🔐 Secure Key Management**: Support for environment-based master key configuration

## Security Features

- **AES-256-GCM Encryption**: Industry-standard encryption with authenticated encryption
- **Unique Nonces**: Each file uses a unique nonce for encryption
- **Secure File Storage**: Encrypted files stored separately from metadata
- **File Type Validation**: Only allowed file types can be uploaded
- **Size Limits**: Configurable maximum file size (default: 16 MB)

## Installation

1. Clone the repository:
```bash
git clone https://github.com/Milyor/Secure-Vault.git
cd Secure-Vault
```

2. Create a virtual environment and activate it:
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

3. Install dependencies:
```bash
pip install -r requirements.txt
```

4. (Optional) Set a master encryption key:
```bash
export VAULT_MASTER_KEY=<64-character-hex-string>
```

If not set, a new key will be generated automatically (printed to console for development).

## Usage

### Start the Server

```bash
python app.py
```

The API will be available at `http://localhost:5000`

### API Endpoints

#### Health Check
```http
GET /health
```

**Response:**
```json
{
  "status": "healthy",
  "message": "Secure Vault API is running"
}
```

#### Upload a File
```http
POST /api/upload
Content-Type: multipart/form-data

file: <file-data>
```

**Response:**
```json
{
  "message": "File uploaded and encrypted successfully",
  "file_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "original_filename": "document.pdf",
  "size": 12345
}
```

#### Download a File
```http
GET /api/download/{file_id}
```

**Response:** Binary file data with proper content disposition headers

#### List All Files
```http
GET /api/files
```

**Response:**
```json
{
  "count": 2,
  "files": [
    {
      "file_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "filename": "document.pdf",
      "size": 12345
    },
    {
      "file_id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
      "filename": "image.png",
      "size": 54321
    }
  ]
}
```

#### Delete a File
```http
DELETE /api/files/{file_id}
```

**Response:**
```json
{
  "message": "File deleted successfully",
  "file_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

## Examples

### Using cURL

**Upload a file:**
```bash
curl -X POST -F "file=@/path/to/file.txt" http://localhost:5000/api/upload
```

**Download a file:**
```bash
curl -O -J http://localhost:5000/api/download/{file_id}
```

**List all files:**
```bash
curl http://localhost:5000/api/files
```

**Delete a file:**
```bash
curl -X DELETE http://localhost:5000/api/files/{file_id}
```

### Using Python

```python
import requests

# Upload a file
with open('document.pdf', 'rb') as f:
    response = requests.post('http://localhost:5000/api/upload', files={'file': f})
    file_id = response.json()['file_id']
    print(f"File uploaded with ID: {file_id}")

# Download the file
response = requests.get(f'http://localhost:5000/api/download/{file_id}')
with open('downloaded_document.pdf', 'wb') as f:
    f.write(response.content)

# List all files
response = requests.get('http://localhost:5000/api/files')
print(response.json())

# Delete the file
response = requests.delete(f'http://localhost:5000/api/files/{file_id}')
print(response.json())
```

## Configuration

### Allowed File Extensions

By default, the following file extensions are allowed:
- Documents: txt, pdf, doc, docx
- Images: png, jpg, jpeg, gif
- Archives: zip

To modify, update the `ALLOWED_EXTENSIONS` set in `app.py`.

### Maximum File Size

Default: 16 MB

To modify, update the `MAX_FILE_SIZE` constant in `app.py`.

### Upload Directory

Default: `uploads/`

To modify, update the `UPLOAD_FOLDER` constant in `app.py`.

## Testing

Run the test suite:

```bash
pytest test_app.py -v
```

Run tests with coverage:

```bash
pytest test_app.py --cov=app --cov-report=html
```

## Architecture

### Components

1. **app.py**: Main Flask application with API endpoints
2. **crypto_utils.py**: Encryption/decryption utilities using AES-256-GCM
3. **test_app.py**: Comprehensive test suite

### Encryption Flow

**Upload:**
1. Client uploads file via POST /api/upload
2. Server validates file type and size
3. File content is read into memory
4. AES-256-GCM encryption is applied with a random nonce
5. Encrypted file is saved to disk with .enc extension
6. Metadata is stored (in-memory for demo, use database in production)
7. Unique file ID is returned to client

**Download:**
1. Client requests file via GET /api/download/{file_id}
2. Server retrieves encrypted file from disk
3. AES-256-GCM decryption is applied
4. Decrypted content is streamed to client

## Production Deployment

For production use, consider the following improvements:

1. **Database**: Replace in-memory metadata store with a proper database (PostgreSQL, MySQL, etc.)
2. **Key Management**: Use a secure key management service (AWS KMS, Azure Key Vault, HashiCorp Vault)
3. **Authentication**: Add user authentication and authorization
4. **HTTPS**: Deploy behind a reverse proxy with TLS/SSL
5. **Storage**: Consider cloud storage (S3, Azure Blob Storage) for encrypted files
6. **Rate Limiting**: Implement rate limiting to prevent abuse
7. **Logging**: Add comprehensive logging for audit trails
8. **Monitoring**: Set up monitoring and alerting
9. **Backup**: Implement backup strategies for encrypted files

## Security Considerations

- **Master Key**: Keep the master encryption key secure and never commit it to version control
- **Key Rotation**: Implement key rotation strategy for production
- **Access Control**: Add authentication and authorization for file access
- **HTTPS Only**: Always use HTTPS in production to protect data in transit
- **Input Validation**: All inputs are validated to prevent injection attacks
- **File Scanning**: Consider adding malware scanning for uploaded files

## License

MIT License

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.