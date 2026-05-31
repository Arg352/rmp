import { Module } from '@nestjs/common';
import { CloudinaryProvider } from './cloudinary.provider';
import { MediaService } from './media.service';
import { MediaController } from './media.controller';

@Module({
  controllers: [MediaController],
  providers: [CloudinaryProvider, MediaService],
  exports: [MediaService],
})
export class MediaModule {}
