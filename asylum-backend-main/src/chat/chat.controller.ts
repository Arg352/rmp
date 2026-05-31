import {
  Controller,
  Get,
  Post,
  Patch,
  Delete,
  Body,
  Param,
  Query,
  ParseIntPipe,
  UseGuards,
} from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { ApiTags, ApiBearerAuth } from '@nestjs/swagger';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { ChatService } from './chat.service';
import { CreateGroupDto } from './dto/create-group.dto';

@ApiTags('Chat')
@ApiBearerAuth()
@Controller('chats')
@UseGuards(AuthGuard('jwt'))
export class ChatController {
  constructor(private readonly chatService: ChatService) {}

  @Get()
  getChats(@CurrentUser() userId: number) {
    return this.chatService.getChats(userId);
  }

  @Get('groups/search')
  searchGroups(@Query('q') q?: string) {
    return this.chatService.searchGroups(q || '');
  }

  @Post('groups')
  createGroup(@CurrentUser() userId: number, @Body() dto: CreateGroupDto) {
    return this.chatService.createGroup(
      userId,
      dto.name,
      dto.description,
      dto.tags,
      dto.visibility,
    );
  }

  @Post('groups/:id/join')
  joinGroup(
    @CurrentUser() userId: number,
    @Param('id', ParseIntPipe) groupId: number,
  ) {
    return this.chatService.joinGroup(userId, groupId);
  }

  @Get('groups/:id/history')
  getGroupHistory(@Param('id', ParseIntPipe) groupId: number) {
    return this.chatService.getGroupHistory(groupId);
  }

  @Get('groups/:id/requests')
  getPendingRequests(
    @Param('id', ParseIntPipe) groupId: number,
    @CurrentUser() adminId: number,
  ) {
    return this.chatService.getPendingRequests(groupId, adminId);
  }

  @Patch('groups/:id/approve/:userId')
  approveMember(
    @Param('id', ParseIntPipe) groupId: number,
    @Param('userId', ParseIntPipe) userId: number,
    @CurrentUser() adminId: number,
  ) {
    return this.chatService.approveMember(groupId, userId, adminId);
  }

  @Delete('groups/:id/reject/:userId')
  rejectMember(
    @Param('id', ParseIntPipe) groupId: number,
    @Param('userId', ParseIntPipe) userId: number,
    @CurrentUser() adminId: number,
  ) {
    return this.chatService.rejectMember(groupId, userId, adminId);
  }

  @Get(':userId')
  getHistory(
    @CurrentUser() userId: number,
    @Param('userId', ParseIntPipe) otherUserId: number,
  ) {
    return this.chatService.getHistory(userId, otherUserId);
  }
}
