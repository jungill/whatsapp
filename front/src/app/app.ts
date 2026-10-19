import { ChangeDetectorRef, Component } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { Chat, Envelope } from './chat';
import {
  Subject,
  Subscription,
  debounceTime,
  distinctUntilChanged,
  switchMap,
  of,
  catchError,
} from 'rxjs';
import { OnInit, OnDestroy } from '@angular/core';

interface DisplayedMessage {
  id: string;
  from: string;
  content: string;
  status?: string;
  sequence?: number;
  mine: boolean;
}

type View = 'login' | 'register' | 'chat';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  view: View = 'login';

  username = '';
  password = '';
  passwordConfirm = '';
  recipient = '';
  draft = '';
  error = '';
  messages: DisplayedMessage[] = [];

  private subscription?: Subscription;

  private recipientChanged = new Subject<string>();
  private historySubscription?: Subscription;

  constructor(
    private chat: Chat,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.historySubscription = this.recipientChanged
      .pipe(
        debounceTime(100),
        distinctUntilChanged(),
        switchMap((peer) => {
          if (!peer.trim()) return of([]);
          return this.chat.history(peer).pipe(catchError(() => of([])));
        }),
      )
      .subscribe((list) => {
        this.messages = list.map((e) => ({
          id: e.id!,
          from: e.from!,
          content: e.content!,
          status: e.status,
          mine: e.from === this.username,
        }));
        this.cdr.markForCheck();
      });
  }

  ngOnDestroy() {
    this.historySubscription?.unsubscribe();
    this.subscription?.unsubscribe();
  }

  onRecipientChange(value: string) {
    this.messages = [];
    this.recipientChanged.next(value);
  }

  goTo(view: View) {
    this.view = view;
    this.error = '';
    this.password = '';
    this.passwordConfirm = '';
  }

  doLogin() {
    this.error = '';
    if (!this.username.trim() || !this.password) {
      this.error = 'Identifiant et mot de passe requis';
      return;
    }
    this.chat.login(this.username, this.password).subscribe({
      next: (res) => this.onToken(res.token),
      error: () => {
        this.error = 'Identifiants invalides';
        this.cdr.markForCheck();
      },
    });
  }

  doRegister() {
    this.error = '';
    if (!this.username.trim() || !this.password) {
      this.error = 'Identifiant et mot de passe requis';
      return;
    }
    if (this.password !== this.passwordConfirm) {
      this.error = 'Les mots de passe ne correspondent pas';
      return;
    }
    this.chat.register(this.username, this.password).subscribe({
      next: (res) => this.onToken(res.token),
      error: (err) => {
        this.error =
          err.status === 409 ? 'Cet identifiant est deja pris' : 'Inscription impossible';
        this.cdr.markForCheck();
      },
    });
  }

  logout() {
    this.subscription?.unsubscribe();
    this.subscription = undefined;
    this.chat.disconnect();

    this.messages = [];
    this.recipient = '';
    this.draft = '';
    this.password = '';
    this.passwordConfirm = '';
    this.error = '';
    this.view = 'login';
  }

  private onToken(token: string) {
    this.chat.connect(token);
    this.subscription = this.chat.incoming.subscribe((e) => this.handle(e));
    this.password = '';
    this.passwordConfirm = '';
    this.view = 'chat';
    this.cdr.markForCheck();
  }

  private handle(e: Envelope) {
    if (e.type === 'message') {
      this.insert({
        id: e.id!,
        from: e.from!,
        content: e.content!,
        sequence: e.sequence,
        mine: false,
      });
    } else if (e.type === 'ack') {
      const target = this.messages.find((m) => m.id === e.messageId);
      if (target) target.status = e.status;
    }
    this.cdr.markForCheck();
  }

  send() {
    if (!this.draft.trim() || !this.recipient.trim()) return;
    const id = this.chat.send(this.recipient, this.draft);
    this.insert({
      id,
      from: this.username,
      content: this.draft,
      mine: true,
    });
    this.draft = '';
  }

  private insert(m: DisplayedMessage) {
    this.messages.push(m);
    this.messages.sort((a, b) => (a.sequence ?? 0) - (b.sequence ?? 0));
  }

  tick(status?: string): string {
    if (status === 'DELIVERED' || status === 'DELIVERY_NOTIFIED') return '✓✓';
    if (status === 'SENT') return '✓';
    return '';
  }

  openConversation() {
    if (!this.recipient.trim()) return;
    this.messages = [];
    this.chat.history(this.recipient).subscribe({
      next: (list) =>
        (this.messages = list.map((e) => ({
          id: e.id!,
          from: e.from!,
          content: e.content!,
          status: e.status,
          mine: e.from === this.username,
        }))),
      error: () => (this.error = 'Impossible de charger la conversation'),
    });
  }
}
