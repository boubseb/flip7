import { Injectable } from '@angular/core';
import { Subject, BehaviorSubject } from 'rxjs';
import SockJS from 'sockjs-client';
import { Client, StompSubscription, IMessage } from '@stomp/stompjs';
import { Room, TurnUpdate } from '../../models/room/room.model';
import { 
  GameStateResponse, 
  RoundEndData, 
  GameOverData, 
  CardDistributedEvent 
} from '../../models/game/game.model';
import { environment } from '../../../environments/environment';

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

  // Observables for game updates
  private gameStateUpdates = new Subject<GameStateResponse>();
  private cardDistributed = new Subject<CardDistributedEvent>();
  private roundReady = new Subject<any>();
  private roundEnd = new Subject<RoundEndData>();
  private gameOver = new Subject<GameOverData>();
  private gameAbandoned = new Subject<{ message: string }>();
  private gameRestarted = new Subject<{ message: string; roomStatus: string }>();

  public roomUpdates$ = this.roomUpdates.asObservable();
  public gameStarts$ = this.gameStarts.asObservable();
  public turnUpdates$ = this.turnUpdates.asObservable();
  public connectionStatus$ = this.connectionStatus.asObservable();
  
  // Game observables
  public gameStateUpdates$ = this.gameStateUpdates.asObservable();
  public cardDistributed$ = this.cardDistributed.asObservable();
  public roundReady$ = this.roundReady.asObservable();
  public roundEnd$ = this.roundEnd.asObservable();
  public gameOver$ = this.gameOver.asObservable();
  public gameAbandoned$ = this.gameAbandoned.asObservable();
  public gameRestarted$ = this.gameRestarted.asObservable();

  connect(): void {
    if (this.stompClient?.connected) {
      console.log('WebSocket already connected');
      return;
    }

    // Create WebSocket connection using SockJS
    // Utilise l'URL de l'API depuis l'environment
    const socket = new SockJS(`${environment.apiUrl}/ws`);
    
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

    // Vérifier si on est déjà souscrit à cette room
    const roomSubKey = `room-${roomId}`;
    if (this.subscriptions.has(roomSubKey)) {
      console.log('⚠️ Already subscribed to room:', roomId);
      return;
    }

    console.log('📡 Creating new subscription for room:', roomId);

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

    // Subscribe to game state updates
    const gameStateSub = this.stompClient.subscribe(
      `/topic/rooms/${roomId}/game`,
      (message: IMessage) => {
        const state: GameStateResponse = JSON.parse(message.body);
        this.gameStateUpdates.next(state);
      }
    );
    this.subscriptions.set(`game-${roomId}`, gameStateSub);

    // Subscribe to card distribution events
    const cardDistSub = this.stompClient.subscribe(
      `/topic/rooms/${roomId}/card-distributed`,
      (message: IMessage) => {
        const event: CardDistributedEvent = JSON.parse(message.body);
        this.cardDistributed.next(event);
      }
    );
    this.subscriptions.set(`card-dist-${roomId}`, cardDistSub);

    // Subscribe to round ready
    const roundReadySub = this.stompClient.subscribe(
      `/topic/rooms/${roomId}/round-ready`,
      (message: IMessage) => {
        const data = JSON.parse(message.body);
        this.roundReady.next(data);
      }
    );
    this.subscriptions.set(`round-ready-${roomId}`, roundReadySub);

    // Subscribe to round end
    const roundEndSub = this.stompClient.subscribe(
      `/topic/rooms/${roomId}/round-end`,
      (message: IMessage) => {
        const data: RoundEndData = JSON.parse(message.body);
        this.roundEnd.next(data);
      }
    );
    this.subscriptions.set(`round-end-${roomId}`, roundEndSub);

    // Subscribe to game over
    const gameOverSub = this.stompClient.subscribe(
      `/topic/rooms/${roomId}/game-over`,
      (message: IMessage) => {
        const data: GameOverData = JSON.parse(message.body);
        this.gameOver.next(data);
      }
    );
    this.subscriptions.set(`game-over-${roomId}`, gameOverSub);

    // Subscribe to game abandoned
    const abandonedSub = this.stompClient.subscribe(
      `/topic/rooms/${roomId}/game-abandoned`,
      (message: IMessage) => {
        const data = JSON.parse(message.body);
        this.gameAbandoned.next(data);
      }
    );
    this.subscriptions.set(`abandoned-${roomId}`, abandonedSub);

    // Subscribe to game restarted
    const restartedSub = this.stompClient.subscribe(
      `/topic/rooms/${roomId}/game-restarted`,
      (message: IMessage) => {
        const data = JSON.parse(message.body);
        this.gameRestarted.next(data);
      }
    );
    this.subscriptions.set(`restarted-${roomId}`, restartedSub);

    console.log(`Subscribed to room: ${roomId}`);
  }

  unsubscribeFromRoom(roomId: string): void {
    const keys = [
      `room-${roomId}`, 
      `start-${roomId}`, 
      `turn-${roomId}`,
      `game-${roomId}`,
      `card-dist-${roomId}`,
      `round-ready-${roomId}`,
      `round-end-${roomId}`,
      `game-over-${roomId}`,
      `abandoned-${roomId}`,
      `restarted-${roomId}`
    ];
    
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
