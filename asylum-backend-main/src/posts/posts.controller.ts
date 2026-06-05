import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  HttpStatus,
  Param,
  ParseIntPipe,
  Post,
  Query,
  UseGuards,
} from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { ApiTags, ApiBearerAuth } from '@nestjs/swagger';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { OptionalJwtAuthGuard } from '../auth/guards/optional-jwt-auth.guard';
import { PostsService } from './posts.service';
import { CreatePostDto } from './dto/create-post.dto';

@ApiTags('Posts')
@Controller('posts')
export class PostsController {
  constructor(private readonly postsService: PostsService) {}

  @Post()
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'))
  create(@Body() dto: CreatePostDto, @CurrentUser() userId: number) {
    return this.postsService.create(dto, userId);
  }

  @Get('feed')
  @ApiBearerAuth()
  @UseGuards(OptionalJwtAuthGuard)
  feed(@CurrentUser() userId?: number, @Query('tag') tag?: string) {
    return this.postsService.feed(userId, tag);
  }

  @Post(':id/like')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'))
  toggleLike(
    @Param('id', ParseIntPipe) postId: number,
    @CurrentUser() userId: number,
  ) {
    return this.postsService.toggleLike(userId, postId);
  }

  @Delete(':id')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'))
  @HttpCode(HttpStatus.NO_CONTENT)
  deletePost(
    @Param('id', ParseIntPipe) postId: number,
    @CurrentUser() userId: number,
  ) {
    return this.postsService.deletePost(postId, userId);
  }
}
