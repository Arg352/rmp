import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreatePostDto } from './dto/create-post.dto';

@Injectable()
export class PostsService {
  constructor(private readonly prisma: PrismaService) {}

  async create(dto: CreatePostDto, userId: number) {
    const allowedUsersData =
      dto.visibility === 'SELECTED' && dto.allowedUserIds?.length
        ? {
            create: dto.allowedUserIds.map((id) => ({
              userId: id,
            })),
          }
        : undefined;

    return this.prisma.post.create({
      data: {
        title: dto.title,
        tags: dto.tags,
        text: dto.text,
        userId,
        isAnonymous: dto.isAnonymous ?? false,
        visibility: dto.visibility ?? 'PUBLIC',
        images: dto.images?.length
          ? { create: dto.images.map((url) => ({ url })) }
          : undefined,
        allowedUsers: allowedUsersData,
      },
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
      },
    });
  }

  async feed(userId?: number, tag?: string) {
    const tagWhere = tag
      ? {
          OR: [{ tags: { contains: tag } }, { text: { contains: tag } }],
        }
      : {};

    const postsWhere = userId
      ? {
          AND: [
            tagWhere,
            {
              OR: [
                { visibility: 'PUBLIC' },
                { userId: userId },
                { allowedUsers: { some: { userId: userId } } },
              ],
            },
          ],
        }
      : {
          AND: [tagWhere, { visibility: 'PUBLIC' }],
        };

    const posts = await this.prisma.post.findMany({
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
        likes: userId ? { where: { userId } } : false,
        _count: { select: { likes: true } },
      },
    });

    return posts.map(({ likes, _count, user, ...post }) => {
      const isLiked = likes ? likes.length > 0 : false;
      const isPostAnonymous = post.isAnonymous && post.userId !== userId;
      const maskedUser = isPostAnonymous
        ? {
            id: 0,
            username: 'anonymous',
            displayName: 'Анонимус',
            avatarUrl: null,
          }
        : user;

      return {
        ...post,
        likesCount: _count.likes,
        isLiked,
        user: maskedUser,
      };
    });
  }

  async toggleLike(
    userId: number,
    postId: number,
  ): Promise<{ liked: boolean }> {
    const existing = await this.prisma.like.findUnique({
      where: { userId_postId: { userId, postId } },
    });

    if (existing) {
      await this.prisma.like.delete({
        where: { userId_postId: { userId, postId } },
      });
      return { liked: false };
    }

    await this.prisma.like.create({
      data: { userId, postId },
    });
    return { liked: true };
  }

  async deletePost(postId: number, userId: number): Promise<void> {
    const post = await this.prisma.post.findUnique({ where: { id: postId } });
    if (!post) {
      throw new NotFoundException('Пост не найден');
    }
    if (post.userId !== userId) {
      throw new ForbiddenException('Нельзя удалить чужой пост');
    }
    // Удаляем связанные данные вручную (если нет каскадного удаления в schema)
    await this.prisma.postImage.deleteMany({ where: { postId } });
    await this.prisma.like.deleteMany({ where: { postId } });
    await this.prisma.post.delete({ where: { id: postId } });
  }
}
