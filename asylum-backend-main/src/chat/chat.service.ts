import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

export interface ChatEntry {
  user: {
    id: number;
    username: string;
    email: string;
    displayName?: string | null;
    bio?: string | null;
    avatarUrl?: string | null;
    lastActiveAt: Date;
  };
  lastMessage: {
    id: number;
    text: string;
    createdAt: Date;
    isRead: boolean;
    senderId: number;
    receiverId: number;
    attachments: { id: number; url: string; messageId: number }[];
  };
  unreadCount: number;
}

@Injectable()
export class ChatService {
  constructor(private readonly prisma: PrismaService) {}

  async getChats(userId: number): Promise<ChatEntry[]> {
    const messages = await this.prisma.message.findMany({
      where: {
        OR: [{ senderId: userId }, { receiverId: userId }],
      },
      orderBy: { createdAt: 'desc' as const },
      include: {
        sender: {
          select: {
            id: true,
            username: true,
            email: true,
            displayName: true,
            bio: true,
            avatarUrl: true,
            lastActiveAt: true,
          },
        },
        receiver: {
          select: {
            id: true,
            username: true,
            email: true,
            displayName: true,
            bio: true,
            avatarUrl: true,
            lastActiveAt: true,
          },
        },
        attachments: true,
      },
    });

    const unreadCounts = await this.prisma.message.groupBy({
      by: ['senderId'],
      where: {
        receiverId: userId,
        isRead: false,
      },
      _count: {
        id: true,
      },
    });

    const unreadMap = new Map<number, number>();
    for (const item of unreadCounts) {
      unreadMap.set(item.senderId, item._count.id);
    }

    const chatsMap = new Map<number, ChatEntry>();

    for (const msg of messages) {
      const otherUser = msg.senderId === userId ? msg.receiver : msg.sender;
      if (!chatsMap.has(otherUser.id)) {
        chatsMap.set(otherUser.id, {
          user: otherUser,
          lastMessage: {
            id: msg.id,
            text: msg.text,
            createdAt: msg.createdAt,
            isRead: msg.isRead,
            senderId: msg.senderId,
            receiverId: msg.receiverId,
            attachments: msg.attachments,
          },
          unreadCount: unreadMap.get(otherUser.id) ?? 0,
        });
      }
    }

    return Array.from(chatsMap.values()).sort(
      (a, b) =>
        b.lastMessage.createdAt.getTime() - a.lastMessage.createdAt.getTime(),
    );
  }

  async getHistory(userId: number, otherUserId: number) {
    return this.prisma.message.findMany({
      where: {
        OR: [
          { senderId: userId, receiverId: otherUserId },
          { senderId: otherUserId, receiverId: userId },
        ],
      },
      orderBy: { createdAt: 'asc' as const },
      include: {
        attachments: true,
      },
    });
  }

  async saveMessage(
    senderId: number,
    receiverId: number,
    text: string,
    attachmentUrls?: string[],
  ) {
    return this.prisma.message.create({
      data: {
        senderId,
        receiverId,
        text,
        attachments: attachmentUrls?.length
          ? { create: attachmentUrls.map((url) => ({ url })) }
          : undefined,
      },
      include: {
        attachments: true,
      },
    });
  }

  async createGroup(
    ownerId: number,
    name: string,
    description?: string,
    tags?: string,
    visibility?: string,
  ) {
    return this.prisma.group.create({
      data: {
        name,
        description,
        ownerId,
        tags,
        visibility: visibility ?? 'OPEN',
        members: {
          create: {
            userId: ownerId,
            role: 'ADMIN',
            status: 'APPROVED',
          },
        },
      },
      include: {
        members: true,
      },
    });
  }

  async joinGroup(userId: number, groupId: number) {
    const group = await this.prisma.group.findUnique({
      where: { id: groupId },
    });
    if (!group) {
      throw new NotFoundException('Group not found');
    }

    const existing = await this.prisma.groupMember.findUnique({
      where: {
        userId_groupId: { userId, groupId },
      },
    });

    if (existing) {
      return existing;
    }

    const status = group.visibility === 'REQUEST' ? 'PENDING' : 'APPROVED';

    return this.prisma.groupMember.create({
      data: {
        userId,
        groupId,
        role: 'MEMBER',
        status,
      },
    });
  }

  async getGroupHistory(groupId: number) {
    const group = await this.prisma.group.findUnique({
      where: { id: groupId },
    });
    if (!group) {
      throw new NotFoundException('Group not found');
    }

    return this.prisma.groupMessage.findMany({
      where: { groupId },
      include: {
        sender: {
          select: {
            id: true,
            username: true,
            displayName: true,
            bio: true,
            avatarUrl: true,
          },
        },
        attachments: true,
      },
      orderBy: {
        createdAt: 'asc' as const,
      },
    });
  }

  async saveGroupMessage(
    senderId: number,
    groupId: number,
    text: string,
    attachmentUrls?: string[],
  ) {
    return this.prisma.groupMessage.create({
      data: {
        senderId,
        groupId,
        text,
        attachments: attachmentUrls?.length
          ? { create: attachmentUrls.map((url) => ({ url })) }
          : undefined,
      },
      include: {
        sender: {
          select: {
            id: true,
            username: true,
            displayName: true,
            bio: true,
            avatarUrl: true,
          },
        },
        attachments: true,
      },
    });
  }

  async getUserGroups(userId: number) {
    return this.prisma.groupMember.findMany({
      where: { userId },
    });
  }

  async searchGroups(q: string) {
    return this.prisma.group.findMany({
      where: {
        visibility: {
          not: 'PRIVATE',
        },
        OR: [{ name: { contains: q } }, { tags: { contains: q } }],
      },
      select: {
        id: true,
        name: true,
        tags: true,
        avatarUrl: true,
        _count: {
          select: {
            members: {
              where: {
                status: 'APPROVED',
              },
            },
          },
        },
      },
    });
  }

  async getPendingRequests(groupId: number, adminId: number) {
    const group = await this.prisma.group.findUnique({
      where: { id: groupId },
    });
    if (!group) {
      throw new NotFoundException('Group not found');
    }
    if (group.ownerId !== adminId) {
      throw new ForbiddenException(
        'You are not the administrator of this group',
      );
    }

    return this.prisma.groupMember.findMany({
      where: {
        groupId,
        status: 'PENDING',
      },
      include: {
        user: {
          select: {
            id: true,
            username: true,
            displayName: true,
            avatarUrl: true,
          },
        },
      },
    });
  }

  async approveMember(groupId: number, userId: number, adminId: number) {
    const group = await this.prisma.group.findUnique({
      where: { id: groupId },
    });
    if (!group) {
      throw new NotFoundException('Group not found');
    }
    if (group.ownerId !== adminId) {
      throw new ForbiddenException(
        'You are not the administrator of this group',
      );
    }

    return this.prisma.groupMember.update({
      where: {
        userId_groupId: { userId, groupId },
      },
      data: {
        status: 'APPROVED',
      },
    });
  }

  async rejectMember(groupId: number, userId: number, adminId: number) {
    const group = await this.prisma.group.findUnique({
      where: { id: groupId },
    });
    if (!group) {
      throw new NotFoundException('Group not found');
    }
    if (group.ownerId !== adminId) {
      throw new ForbiddenException(
        'You are not the administrator of this group',
      );
    }

    return this.prisma.groupMember.delete({
      where: {
        userId_groupId: { userId, groupId },
      },
    });
  }
}
