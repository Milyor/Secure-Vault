"""
Tests for the Secure File Vault API
"""

import os
import pytest
import tempfile
from app import app, file_metadata, UPLOAD_FOLDER


@pytest.fixture
def client():
    """Create a test client for the app."""
    app.config['TESTING'] = True
    
    with app.test_client() as client:
        yield client
    
    # Cleanup after tests
    file_metadata.clear()
    if os.path.exists(UPLOAD_FOLDER):
        for file in os.listdir(UPLOAD_FOLDER):
            os.remove(os.path.join(UPLOAD_FOLDER, file))


@pytest.fixture
def sample_file():
    """Create a temporary sample file for testing."""
    content = b"This is a test file for secure vault API"
    temp_file = tempfile.NamedTemporaryFile(delete=False, suffix='.txt')
    temp_file.write(content)
    temp_file.close()
    
    yield temp_file.name, content
    
    # Cleanup
    if os.path.exists(temp_file.name):
        os.remove(temp_file.name)


def test_health_check(client):
    """Test the health check endpoint."""
    response = client.get('/health')
    assert response.status_code == 200
    data = response.get_json()
    assert data['status'] == 'healthy'


def test_upload_file_success(client, sample_file):
    """Test successful file upload."""
    file_path, content = sample_file
    
    with open(file_path, 'rb') as f:
        response = client.post(
            '/api/upload',
            data={'file': (f, 'test.txt')},
            content_type='multipart/form-data'
        )
    
    assert response.status_code == 201
    data = response.get_json()
    assert 'file_id' in data
    assert data['original_filename'] == 'test.txt'
    assert data['size'] == len(content)


def test_upload_no_file(client):
    """Test upload without file."""
    response = client.post('/api/upload')
    assert response.status_code == 400
    data = response.get_json()
    assert 'error' in data


def test_upload_empty_filename(client):
    """Test upload with empty filename."""
    response = client.post(
        '/api/upload',
        data={'file': (b'', '')},
        content_type='multipart/form-data'
    )
    assert response.status_code == 400


def test_upload_invalid_extension(client):
    """Test upload with invalid file extension."""
    from io import BytesIO
    response = client.post(
        '/api/upload',
        data={'file': (BytesIO(b'test'), 'test.exe')},
        content_type='multipart/form-data'
    )
    assert response.status_code == 400
    data = response.get_json()
    assert 'not allowed' in data['error'].lower()


def test_download_file_success(client, sample_file):
    """Test successful file download."""
    file_path, content = sample_file
    
    # First upload a file
    with open(file_path, 'rb') as f:
        upload_response = client.post(
            '/api/upload',
            data={'file': (f, 'test.txt')},
            content_type='multipart/form-data'
        )
    
    file_id = upload_response.get_json()['file_id']
    
    # Now download it
    download_response = client.get(f'/api/download/{file_id}')
    assert download_response.status_code == 200
    assert download_response.data == content


def test_download_nonexistent_file(client):
    """Test downloading a file that doesn't exist."""
    response = client.get('/api/download/nonexistent-id')
    assert response.status_code == 404


def test_list_files_empty(client):
    """Test listing files when vault is empty."""
    response = client.get('/api/files')
    assert response.status_code == 200
    data = response.get_json()
    assert data['count'] == 0
    assert len(data['files']) == 0


def test_list_files_with_uploads(client, sample_file):
    """Test listing files after uploads."""
    file_path, content = sample_file
    
    # Upload a file
    with open(file_path, 'rb') as f:
        client.post(
            '/api/upload',
            data={'file': (f, 'test.txt')},
            content_type='multipart/form-data'
        )
    
    # List files
    response = client.get('/api/files')
    assert response.status_code == 200
    data = response.get_json()
    assert data['count'] == 1
    assert len(data['files']) == 1
    assert data['files'][0]['filename'] == 'test.txt'


def test_delete_file_success(client, sample_file):
    """Test successful file deletion."""
    file_path, content = sample_file
    
    # Upload a file
    with open(file_path, 'rb') as f:
        upload_response = client.post(
            '/api/upload',
            data={'file': (f, 'test.txt')},
            content_type='multipart/form-data'
        )
    
    file_id = upload_response.get_json()['file_id']
    
    # Delete the file
    delete_response = client.delete(f'/api/files/{file_id}')
    assert delete_response.status_code == 200
    
    # Verify file is gone
    download_response = client.get(f'/api/download/{file_id}')
    assert download_response.status_code == 404


def test_delete_nonexistent_file(client):
    """Test deleting a file that doesn't exist."""
    response = client.delete('/api/files/nonexistent-id')
    assert response.status_code == 404


def test_encryption_decryption_cycle(client, sample_file):
    """Test that uploaded files are properly encrypted and decrypted."""
    file_path, original_content = sample_file
    
    # Upload file
    with open(file_path, 'rb') as f:
        upload_response = client.post(
            '/api/upload',
            data={'file': (f, 'test.txt')},
            content_type='multipart/form-data'
        )
    
    file_id = upload_response.get_json()['file_id']
    
    # Read encrypted file from disk
    encrypted_filename = file_metadata[file_id]['encrypted_filename']
    encrypted_path = os.path.join(UPLOAD_FOLDER, encrypted_filename)
    
    with open(encrypted_path, 'rb') as f:
        encrypted_content = f.read()
    
    # Verify encrypted content is different from original
    assert encrypted_content != original_content
    
    # Download and verify decryption
    download_response = client.get(f'/api/download/{file_id}')
    assert download_response.data == original_content
