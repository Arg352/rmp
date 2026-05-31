import {
  IsEmail,
  IsOptional,
  IsString,
  MinLength,
  IsBoolean,
} from 'class-validator';

export class UpdateSettingsDto {
  @IsOptional()
  @IsString()
  @MinLength(3)
  username?: string;

  @IsOptional()
  @IsEmail()
  email?: string;

  @IsOptional()
  @IsString()
  @MinLength(6)
  password?: string;

  @IsOptional()
  @IsString()
  displayName?: string;

  @IsOptional()
  @IsString()
  bio?: string;

  @IsOptional()
  @IsString()
  avatarUrl?: string;

  @IsOptional()
  @IsBoolean()
  notifyOnMessages?: boolean;

  @IsOptional()
  @IsBoolean()
  notifyOnGroups?: boolean;

  @IsOptional()
  @IsBoolean()
  notifyOnFollows?: boolean;

  @IsOptional()
  @IsBoolean()
  notifyOnLikes?: boolean;
}
