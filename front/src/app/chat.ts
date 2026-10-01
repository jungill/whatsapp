import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subject } from 'rxjs';

export interface Envelope {
  type: 'message' | 'ack';
  id?: string;
  from?: string;
  to?: string;
  content?: string;
  messageId?: string;
  status?: 'SENT' | 'DELIVERED' | 'READ';
}

@Injectable({ providedIn: 'root' })
export class Chat {
  private socket?: WebSocket;
  readonly incoming = new Subject<Envelope>();

  constructor(private http: HttpClient) {}

  login(username: string, password: string) {
    return this.http.post<{ token: string }>(
      'http://localhost:8080/auth/login',
      { username, password }
    );
  }

  register(username: string, password: string) {
    return this.http.post<{ token: string }>(
      'http://localhost:8080/auth/register',
      { username, password }
    );
  }

  connect(token: string) {
    this.disconnect();
    this.socket = new WebSocket(`ws://localhost:8080/ws?token=${token}`);
    this.socket.onmessage = (event) =>
      this.incoming.next(JSON.parse(event.data) as Envelope);
    this.socket.onclose = () => console.warn('WebSocket ferme');
  }

  send(to: string, content: string): string {
    const id = crypto.randomUUID();
    this.socket?.send(JSON.stringify({ id, to, content }));
    return id;
  }

  disconnect() {
    this.socket?.close();
    this.socket = undefined;
  }
}