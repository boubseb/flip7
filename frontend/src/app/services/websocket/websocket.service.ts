import { Injectable } from '@angular/core';
import { Subject, BehaviorSubject } from 'rxjs';
import SockJS from 'sockjs-client';
import { Client, StompSubscription, IMessage } from '@stomp/stompjs';
import { Room, TurnUpdate } from '../../models/room/room.model';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private stompClient: Client | null = null;
  private connectionStatus = new BehaviorSubject<boolean>(false);
  private subscriptions: Map<string, StompSubscription> = new Map();

  // Observables for room updates
  private roomUpdates = new Subject<Room>();
  private gameStarts = new Subject<Room>();
  private turnUpdates = new Subject<TurnUpdate>();

  public roomUpdates$ = this.roomUpdates.asObservable();
  public gameStarts$ = this.gameStarts.asObservable();
  public turnUpdates$ = this.turnUpdates.asObservable();
  public connectionStatus$ = this.connectionStatus.asObservable();

  connect(): void {
    if (this.stompClient?.connected) {
      console.log('WebSocket already connected');
      return;
    }

    // Create WebSocket connection using SockJS
    const socket = new SockJS('http://localhost:3200/ws');
    
    this.stompClient = new Client({
      webSocketFactory: () => socket as any,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str: string) => {
        console.log('STOMP: ' + str);
      }
    });

    this.stompClient.onConnect = (frame: any) => {
      console.log('WebSocket connected:', frame);
      this.connectionStatus.next(true);
    };

    this.stompClient.onStompError = (frame: any) => {
      console.error('STOMP error:', frame);
      this.connectionStatus.next(false);
    };

    this.stompClient.onWebSocketClose = (event: any) => {
      console.log('WebSocket closed:', event);
      this.connectionStatus.next(false);
    };

    this.stompClient.activate();
  }

  disconnect(): void {
    if (this.stompClient) {
      // Unsubscribe from all topics
      this.subscriptions.forEach((subscription) => {
        subscription.unsubscribe();
      });
      this.subscriptions.clear();

      this.stompClient.deactivate();
      this.stompClient = null;
      this.connectionStatus.next(false);
      console.log('WebSocket disconnected');
    }
  }

  subscribeToRoom(roomId: string): void {
    if (!this.stompClient?.connected) {
      console.error('WebSocket not connected');
      return;
    }

    // Subscribe to room updates
    const roomSub = this.stompClient.subscribe(
      `/topic/rooms/${roomId}`,
      (message: IMessage) => {
        const room: Room = JSON.parse(message.body);
        this.roomUpdates.next(room);
      }
    );
    this.subscriptions.set(`room-${roomId}`, roomSub);

    // Subscribe to game start
    const startSub = this.stompClient.subscribe(
      `/topic/rooms/${roomId}/start`,
      (message: IMessage) => {
        const room: Room = JSON.parse(message.body);
        this.gameStarts.next(room);
      }
    );
    this.subscriptions.set(`start-${roomId}`, startSub);

    // Subscribe to turn updates
    const turnSub = this.stompClient.subscribe(
      `/topic/rooms/${roomId}/turn`,
      (message: IMessage) => {
        const update: TurnUpdate = JSON.parse(message.body);
        this.turnUpdates.next(update);
      }
    );
    this.subscriptions.set(`turn-${roomId}`, turnSub);

    console.log(`Subscribed to room: ${roomId}`);
  }

  unsubscribeFromRoom(roomId: string): void {
    const keys = [`room-${roomId}`, `start-${roomId}`, `turn-${roomId}`];
    
    keys.forEach(key => {
      const subscription = this.subscriptions.get(key);
      if (subscription) {
        subscription.unsubscribe();
        this.subscriptions.delete(key);
      }
    });

    console.log(`Unsubscribed from room: ${roomId}`);
  }

  isConnected(): boolean {
    return this.stompClient?.connected || false;
  }
}
