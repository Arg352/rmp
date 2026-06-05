import {
  WebSocketGateway,
  WebSocketServer,
  SubscribeMessage,
  MessageBody,
  ConnectedSocket,
  OnGatewayConnection,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { JwtService } from '@nestjs/jwt';
import { ChatService } from './chat.service';

@WebSocketGateway({ cors: { origin: '*' } })
export class ChatGateway implements OnGatewayConnection {
  @WebSocketServer()
  server: Server;

  constructor(
    private readonly jwtService: JwtService,
    private readonly chatService: ChatService,
  ) {}

  async handleConnection(client: Socket) {
    try {
      const auth = client.handshake.auth as Record<string, any> | undefined;
      const headers = client.handshake.headers as
        | Record<string, any>
        | undefined;
      let token = (auth?.token || headers?.authorization) as string | undefined;

      if (!token) {
        client.disconnect();
        return;
      }
      if (token.startsWith('Bearer ')) {
        token = token.substring(7);
      }
      const verified = this.jwtService.verify(token, {
        secret: process.env.JWT_SECRET ?? 'jwt_secret',
      }) as unknown;
      const payload = verified as { sub: number };
      const userId = payload.sub;

      const clientData = client.data as unknown as Record<string, any>;
      clientData.userId = userId;
      await client.join(`user_${userId}`);

      const groups = await this.chatService.getUserGroups(userId);
      for (const member of groups) {
        await client.join(`group_${member.groupId}`);
      }
    } catch {
      client.disconnect();
    }
  }

  @SubscribeMessage('sendMessage')
  async handleSendMessage(
    @ConnectedSocket() client: Socket,
    @MessageBody()
    payload: {
      receiverId: number;
      text: string;
      imageUrls?: string[];       // поле от Android-клиента
      attachmentUrls?: string[];  // поле для совместимости
    },
  ) {
    const clientData = client.data as unknown as Record<string, any>;
    const senderId = clientData.userId as number | undefined;
    if (!senderId) {
      return;
    }

    // Android отправляет imageUrls, принимаем оба варианта
    const attachmentUrls = payload.imageUrls ?? payload.attachmentUrls;

    const receiverId = Number(payload.receiverId);
    const savedMessage = await this.chatService.saveMessage(
      senderId,
      receiverId,
      payload.text,
      attachmentUrls,
    );

    this.server.to(`user_${receiverId}`).emit('newMessage', savedMessage);
    this.server.to(`user_${senderId}`).emit('newMessage', savedMessage);
  }

  @SubscribeMessage('sendGroupMessage')
  async handleSendGroupMessage(
    @ConnectedSocket() client: Socket,
    @MessageBody()
    payload: {
      groupId: number;
      text: string;
      attachmentUrls?: string[];
    },
  ) {
    const clientData = client.data as unknown as Record<string, any>;
    const senderId = clientData.userId as number | undefined;
    if (!senderId) {
      return;
    }

    const groupId = Number(payload.groupId);
    const savedMessage = await this.chatService.saveGroupMessage(
      senderId,
      groupId,
      payload.text,
      payload.attachmentUrls,
    );

    this.server.to(`group_${groupId}`).emit('newGroupMessage', savedMessage);
  }
}
