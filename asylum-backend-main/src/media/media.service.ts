import { Injectable } from '@nestjs/common';
import { v2 as cloudinary, UploadApiResponse } from 'cloudinary';
import * as streamifier from 'streamifier';

@Injectable()
export class MediaService {
  uploadImage(file: Express.Multer.File): Promise<string> {
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
    return Promise.all(files.map((file) => this.uploadImage(file)));
  }
}
