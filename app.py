"""
Secure File Vault API
A Flask-based REST API for secure file upload, download, and management with encryption.
"""

import os
import uuid
from flask import Flask, request, jsonify, send_file
from werkzeug.utils import secure_filename
from crypto_utils import CryptoManager
import io

app = Flask(__name__)

# Configuration
UPLOAD_FOLDER = 'uploads'
MAX_FILE_SIZE = 16 * 1024 * 1024  # 16 MB
ALLOWED_EXTENSIONS = {'txt', 'pdf', 'png', 'jpg', 'jpeg', 'gif', 'doc', 'docx', 'zip'}

# Configure Flask to reject files larger than MAX_FILE_SIZE
app.config['MAX_CONTENT_LENGTH'] = MAX_FILE_SIZE

# Ensure upload directory exists
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

# Initialize crypto manager with a master key
# In production, load this from a secure environment variable or key management service
MASTER_KEY = os.environ.get('VAULT_MASTER_KEY')
if MASTER_KEY:
    MASTER_KEY = bytes.fromhex(MASTER_KEY)
else:
    # Generate a new key for development (not for production use)
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM
    MASTER_KEY = AESGCM.generate_key(bit_length=256)
    # Only log the key in development mode
    if os.environ.get('FLASK_ENV') == 'development' or not os.environ.get('FLASK_ENV'):
        import sys
        print(f"WARNING: Using auto-generated master key. Save this for persistence: {MASTER_KEY.hex()}", file=sys.stderr)

crypto_manager = CryptoManager(MASTER_KEY)

# In-memory metadata store (in production, use a database)
file_metadata = {}


def allowed_file(filename):
    """Check if file extension is allowed."""
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS


@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint."""
    return jsonify({
        'status': 'healthy',
        'message': 'Secure Vault API is running'
    }), 200


@app.route('/api/upload', methods=['POST'])
def upload_file():
    """
    Upload and encrypt a file.
    
    Returns:
        JSON response with file_id and metadata
    """
    # Check if file is in request
    if 'file' not in request.files:
        return jsonify({'error': 'No file provided'}), 400
    
    file = request.files['file']
    
    # Check if file is selected
    if file.filename == '':
        return jsonify({'error': 'No file selected'}), 400
    
    # Validate file
    if not allowed_file(file.filename):
        return jsonify({'error': 'File type not allowed'}), 400
    
    # Check file size
    file.seek(0, os.SEEK_END)
    file_size = file.tell()
    file.seek(0)
    
    if file_size > MAX_FILE_SIZE:
        return jsonify({'error': f'File too large. Maximum size is {MAX_FILE_SIZE} bytes'}), 400
    
    if file_size == 0:
        return jsonify({'error': 'File is empty'}), 400
    
    try:
        # Read file data
        file_data = file.read()
        
        # Encrypt the file
        encrypted_data = crypto_manager.encrypt_file(file_data)
        
        # Generate unique file ID
        file_id = str(uuid.uuid4())
        
        # Save encrypted file
        encrypted_filename = f"{file_id}.enc"
        encrypted_path = os.path.join(UPLOAD_FOLDER, encrypted_filename)
        
        with open(encrypted_path, 'wb') as f:
            f.write(encrypted_data)
        
        # Store metadata
        original_filename = secure_filename(file.filename)
        file_metadata[file_id] = {
            'id': file_id,
            'original_filename': original_filename,
            'encrypted_filename': encrypted_filename,
            'size': file_size,
            'encrypted_size': len(encrypted_data)
        }
        
        return jsonify({
            'message': 'File uploaded and encrypted successfully',
            'file_id': file_id,
            'original_filename': original_filename,
            'size': file_size
        }), 201
        
    except Exception as e:
        return jsonify({'error': f'Failed to upload file: {str(e)}'}), 500


@app.route('/api/download/<file_id>', methods=['GET'])
def download_file(file_id):
    """
    Download and decrypt a file.
    
    Args:
        file_id: The unique file identifier
        
    Returns:
        Decrypted file content
    """
    # Check if file exists in metadata
    if file_id not in file_metadata:
        return jsonify({'error': 'File not found'}), 404
    
    metadata = file_metadata[file_id]
    encrypted_path = os.path.join(UPLOAD_FOLDER, metadata['encrypted_filename'])
    
    # Check if encrypted file exists
    if not os.path.exists(encrypted_path):
        return jsonify({'error': 'Encrypted file not found on disk'}), 404
    
    try:
        # Read encrypted file
        with open(encrypted_path, 'rb') as f:
            encrypted_data = f.read()
        
        # Decrypt the file
        decrypted_data = crypto_manager.decrypt_file(encrypted_data)
        
        # Return decrypted file
        return send_file(
            io.BytesIO(decrypted_data),
            download_name=metadata['original_filename'],
            as_attachment=True
        )
        
    except Exception as e:
        return jsonify({'error': f'Failed to download file: {str(e)}'}), 500


@app.route('/api/files', methods=['GET'])
def list_files():
    """
    List all uploaded files.
    
    Returns:
        JSON array of file metadata
    """
    files = [
        {
            'file_id': metadata['id'],
            'filename': metadata['original_filename'],
            'size': metadata['size']
        }
        for metadata in file_metadata.values()
    ]
    
    return jsonify({
        'count': len(files),
        'files': files
    }), 200


@app.route('/api/files/<file_id>', methods=['DELETE'])
def delete_file(file_id):
    """
    Delete a file from the vault.
    
    Args:
        file_id: The unique file identifier
        
    Returns:
        JSON response confirming deletion
    """
    # Check if file exists in metadata
    if file_id not in file_metadata:
        return jsonify({'error': 'File not found'}), 404
    
    metadata = file_metadata[file_id]
    encrypted_path = os.path.join(UPLOAD_FOLDER, metadata['encrypted_filename'])
    
    try:
        # Delete encrypted file from disk
        if os.path.exists(encrypted_path):
            os.remove(encrypted_path)
        
        # Remove from metadata
        del file_metadata[file_id]
        
        return jsonify({
            'message': 'File deleted successfully',
            'file_id': file_id
        }), 200
        
    except Exception as e:
        return jsonify({'error': f'Failed to delete file: {str(e)}'}), 500


@app.errorhandler(413)
def request_entity_too_large(error):
    """Handle file too large error."""
    return jsonify({'error': 'File too large'}), 413


@app.errorhandler(500)
def internal_server_error(error):
    """Handle internal server errors."""
    return jsonify({'error': 'Internal server error'}), 500


if __name__ == '__main__':
    # Get configuration from environment variables
    debug_mode = os.environ.get('FLASK_DEBUG', 'False').lower() == 'true'
    host = os.environ.get('FLASK_HOST', '0.0.0.0')
    port = int(os.environ.get('FLASK_PORT', '5000'))
    
    if debug_mode:
        import sys
        print("WARNING: Running in debug mode. Do not use in production!", file=sys.stderr)
    
    app.run(debug=debug_mode, host=host, port=port)
