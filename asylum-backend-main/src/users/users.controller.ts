import {
  Controller,
  Get,
  Post,
  Patch,
  Body,
  Param,
  Query,
  UseGuards,
  ParseIntPipe,
} from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { ApiTags, ApiBearerAuth } from '@nestjs/swagger';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { OptionalJwtAuthGuard } from '../auth/guards/optional-jwt-auth.guard';
import { UsersService } from './users.service';
import { UpdateSettingsDto } from './dto/update-settings.dto';

@ApiTags('Users')
@Controller('users')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Get('me')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'))
  getMe(@CurrentUser() userId: number) {
    return this.usersService.findById(userId);
  }

  @Patch('settings')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'))
  updateSettings(
    @CurrentUser() userId: number,
    @Body() dto: UpdateSettingsDto,
  ) {
    return this.usersService.updateSettings(userId, dto);
  }

  @Get('search')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'))
  search(@Query('q') q?: string) {
    return this.usersService.search(q);
  }

  @Get('me/following')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'))
  getFollowing(@CurrentUser() userId: number) {
    return this.usersService.getFollowing(userId);
  }

  @Get('me/followers')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'))
  getFollowers(@CurrentUser() userId: number) {
    return this.usersService.getFollowers(userId);
  }

  @Get(':id')
  @ApiBearerAuth()
  @UseGuards(OptionalJwtAuthGuard)
  findOne(
    @Param('id', ParseIntPipe) id: number,
    @CurrentUser() currentUserId?: number,
  ) {
    return this.usersService.findByIdWithProfile(id, currentUserId);
  }

  @Post(':id/follow')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'))
  follow(@Param('id', ParseIntPipe) id: number, @CurrentUser() userId: number) {
    return this.usersService.follow(userId, id);
  }
}
