import {
  Injectable,
  NotFoundException,
  BadRequestException,
} from '@nestjs/common';
import * as bcrypt from 'bcrypt';
import { Prisma } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { UpdateSettingsDto } from './dto/update-settings.dto';

@Injectable()
export class UsersService {
  constructor(private readonly prisma: PrismaService) {}

  async findById(id: number) {
    const user = await this.prisma.user.findUnique({
      where: { id },
      select: {
        id: true,
        username: true,
        email: true,
        displayName: true,
        bio: true,
        avatarUrl: true,
        notifyOnMessages: true,
        notifyOnGroups: true,
        notifyOnFollows: true,
        notifyOnLikes: true,
        createdAt: true,
        lastActiveAt: true,
      },
    });

    if (!user) {
      throw new NotFoundException('User not found');
    }

    return user;
  }

  async updateSettings(userId: number, dto: UpdateSettingsDto) {
    const data: Prisma.UserUpdateInput = {};
    if (dto.username !== undefined) {
      data.username = dto.username;
    }
    if (dto.email !== undefined) {
      data.email = dto.email;
    }
    if (dto.displayName !== undefined) {
      data.displayName = dto.displayName;
    }
    if (dto.bio !== undefined) {
      data.bio = dto.bio;
    }
    if (dto.avatarUrl !== undefined) {
      data.avatarUrl = dto.avatarUrl;
    }
    if (dto.notifyOnMessages !== undefined) {
      data.notifyOnMessages = dto.notifyOnMessages;
    }
    if (dto.notifyOnGroups !== undefined) {
      data.notifyOnGroups = dto.notifyOnGroups;
    }
    if (dto.notifyOnFollows !== undefined) {
      data.notifyOnFollows = dto.notifyOnFollows;
    }
    if (dto.notifyOnLikes !== undefined) {
      data.notifyOnLikes = dto.notifyOnLikes;
    }
    if (dto.password !== undefined) {
      data.passwordHash = await bcrypt.hash(dto.password, 10);
    }

    return this.prisma.user.update({
      where: { id: userId },
      data,
      select: {
        id: true,
        username: true,
        email: true,
        displayName: true,
        bio: true,
        avatarUrl: true,
        notifyOnMessages: true,
        notifyOnGroups: true,
        notifyOnFollows: true,
        notifyOnLikes: true,
        createdAt: true,
        lastActiveAt: true,
      },
    });
  }

  async search(q?: string) {
    if (!q) {
      return [];
    }

    return this.prisma.user.findMany({
      where: {
        username: {
          contains: q,
        },
      },
      select: {
        id: true,
        username: true,
        lastActiveAt: true,
      },
    });
  }

  async findByIdWithProfile(id: number, currentUserId?: number) {
    const postsWhere = currentUserId
      ? {
          OR: [
            { visibility: 'PUBLIC' },
            { userId: currentUserId },
            { allowedUsers: { some: { userId: currentUserId } } },
          ],
        }
      : {
          visibility: 'PUBLIC',
        };

    const user = await this.prisma.user.findUnique({
      where: { id },
      select: {
        id: true,
        username: true,
        email: true,
        displayName: true,
        bio: true,
        avatarUrl: true,
        lastActiveAt: true,
        createdAt: true,
        _count: {
          select: {
            followers: true,
            following: true,
          },
        },
        posts: {
          where: postsWhere,
          orderBy: { createdAt: 'desc' as const },
          include: {
            images: true,
            user: {
              select: {
                id: true,
                username: true,
                displayName: true,
                bio: true,
                avatarUrl: true,
              },
            },
            likes: currentUserId ? { where: { userId: currentUserId } } : false,
            _count: { select: { likes: true } },
          },
        },
      },
    });

    if (!user) {
      throw new NotFoundException('User not found');
    }

    const posts = user.posts.map(
      ({ likes, _count, user: postUser, ...post }) => {
        const isLiked = likes ? likes.length > 0 : false;
        const isPostAnonymous =
          post.isAnonymous && post.userId !== currentUserId;
        const maskedUser = isPostAnonymous
          ? {
              id: 0,
              username: 'anonymous',
              displayName: 'Анонимус',
              avatarUrl: null,
            }
          : postUser;

        return {
          ...post,
          likesCount: _count.likes,
          isLiked,
          user: maskedUser,
        };
      },
    );

    return {
      id: user.id,
      username: user.username,
      email: user.email,
      displayName: user.displayName,
      bio: user.bio,
      avatarUrl: user.avatarUrl,
      lastActiveAt: user.lastActiveAt,
      createdAt: user.createdAt,
      _count: user._count,
      posts,
    };
  }

  async follow(userId: number, targetId: number) {
    if (userId === targetId) {
      throw new BadRequestException('You cannot follow yourself');
    }

    const targetUser = await this.prisma.user.findUnique({
      where: { id: targetId },
    });
    if (!targetUser) {
      throw new NotFoundException('User not found');
    }

    const existing = await this.prisma.friendship.findUnique({
      where: {
        followerId_followingId: {
          followerId: userId,
          followingId: targetId,
        },
      },
    });

    if (existing) {
      await this.prisma.friendship.delete({
        where: { id: existing.id },
      });
      return { followed: false };
    }

    await this.prisma.friendship.create({
      data: {
        followerId: userId,
        followingId: targetId,
      },
    });

    return { followed: true };
  }

  async getFollowing(userId: number) {
    const friendships = await this.prisma.friendship.findMany({
      where: { followerId: userId },
      include: {
        following: {
          select: {
            id: true,
            username: true,
            displayName: true,
            avatarUrl: true,
            lastActiveAt: true,
          },
        },
      },
    });
    return friendships.map((f) => f.following);
  }

  async getFollowers(userId: number) {
    const friendships = await this.prisma.friendship.findMany({
      where: { followingId: userId },
      include: {
        follower: {
          select: {
            id: true,
            username: true,
            displayName: true,
            avatarUrl: true,
            lastActiveAt: true,
          },
        },
      },
    });
    return friendships.map((f) => f.follower);
  }
}
