import {
  IsString,
  IsOptional,
  IsArray,
  IsBoolean,
  IsInt,
} from 'class-validator';

export class CreatePostDto {
  @IsString()
  title: string;

  @IsString()
  @IsOptional()
  tags?: string;

  @IsString()
  text: string;

  @IsArray()
  @IsString({ each: true })
  @IsOptional()
  images?: string[];

  @IsOptional()
  @IsBoolean()
  isAnonymous?: boolean;

  @IsOptional()
  @IsString()
  visibility?: string;

  @IsOptional()
  @IsArray()
  @IsInt({ each: true })
  allowedUserIds?: number[];
}
