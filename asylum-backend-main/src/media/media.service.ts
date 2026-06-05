import { Injectable, InternalServerErrorException } from '@nestjs/common';
import { v2 as cloudinary, UploadApiResponse } from 'cloudinary';
import * as streamifier from 'streamifier';

@Injectable()
export class MediaService {
  private checkCloudinaryConfig() {
    const cfg = cloudinary.config();
    if (!cfg.cloud_name || !cfg.api_key || !cfg.api_secret) {
      throw new InternalServerErrorException(
        'Cloudinary не настроен. Заполните CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY и CLOUDINARY_API_SECRET в .env',
      );
    }
  }

  uploadImage(file: Express.Multer.File): Promise<string> {
    this.checkCloudinaryConfig();
    return new Promise<string>((resolve, reject) => {
      const uploadStream = cloudinary.uploader.upload_stream(
        { folder: 'asylum' },
        (error, result: UploadApiResponse | undefined) => {
          if (error || !result) {
            return reject(error ?? new Error('Upload failed'));
          }
          resolve(result.secure_url);
        },
      );

      streamifier.createReadStream(file.buffer).pipe(uploadStream);
    });
  }

  uploadMultipleImages(files: Express.Multer.File[]): Promise<string[]> {
    if (!files || files.length === 0) {
      return Promise.resolve([]);
    }
    return Promise.all(files.map((file) => this.uploadImage(file)));
  }
}
