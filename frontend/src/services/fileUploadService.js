import axiosClient from './axiosClient';

class FileUploadService {

  // Upload book image
  async uploadBookImage(file) {
    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await axiosClient.post('/files/upload/book-image', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      return response.data;
    } catch (error) {
      console.error('Error uploading book image:', error);
      throw error;
    }
  }

  // Upload avatar
  async uploadAvatar(file) {
    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await axiosClient.post('/files/upload/avatar', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      return response.data;
    } catch (error) {
      console.error('Error uploading avatar:', error);
      throw error;
    }
  }

  // Upload document
  async uploadDocument(file) {
    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await axiosClient.post('/files/upload/document', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      return response.data;
    } catch (error) {
      console.error('Error uploading document:', error);
      throw error;
    }
  }

  // Delete file
  async deleteFile(filePath) {
    try {
      const response = await axiosClient.delete('/files/delete', {
        params: { filePath }
      });

      return response.data;
    } catch (error) {
      console.error('Error deleting file:', error);
      throw error;
    }
  }

  // Check if file exists
  async checkFileExists(filePath) {
    try {
      const response = await axiosClient.get('/files/exists', {
        params: { filePath }
      });

      return response.data;
    } catch (error) {
      console.error('Error checking file existence:', error);
      throw error;
    }
  }

  // Validate file before upload
  validateImageFile(file, maxSizeMB = 5) {
    const errors = [];

    // Check file type
    if (!file.type.startsWith('image/')) {
      errors.push('Chỉ được upload file ảnh');
    }

    // Check file size
    const maxSizeBytes = maxSizeMB * 1024 * 1024;
    if (file.size > maxSizeBytes) {
      errors.push(`Kích thước file không được vượt quá ${maxSizeMB}MB`);
    }

    return {
      isValid: errors.length === 0,
      errors
    };
  }

  // Validate document file
  validateDocumentFile(file, maxSizeMB = 10) {
    const errors = [];

    // Check file type
    const allowedTypes = [
      'application/pdf',
      'application/msword',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    ];

    if (!allowedTypes.includes(file.type)) {
      errors.push('Chỉ được upload file PDF hoặc Word');
    }

    // Check file size
    const maxSizeBytes = maxSizeMB * 1024 * 1024;
    if (file.size > maxSizeBytes) {
      errors.push(`Kích thước file không được vượt quá ${maxSizeMB}MB`);
    }

    return {
      isValid: errors.length === 0,
      errors
    };
  }
}

const fileUploadService = new FileUploadService();
export default fileUploadService;
