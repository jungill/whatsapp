import { ChangeDetectorRef, Component, OnInit, OnDestroy } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Chat, Envelope, ConversationSummary } from './chat';
import {
  Subject,
  Subscription,
  debounceTime,
  distinctUntilChanged,
  switchMap,
  of,
  catchError,
} from 'rxjs';

interface DisplayedMessage {
  id: string;
  from: string;
  content: string;
  status?: string;
  sequence?: number;
  mine: boolean;
  createdAt?: string;
}

type View = 'login' | 'register' | 'chat';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit, OnDestroy {
  view: View = 'login';

  username = '';
  password = '';
  passwordConfirm = '';
  recipient = '';
  draft = '';
  error = '';
  messages: DisplayedMessage[] = [];

  // NOUVEAU : état du menu
  menuOpen = false;
  conversations: ConversationSummary[] = [];
  newPeer = '';

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
          sequence: e.sequence,
          createdAt: e.createdAt,
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
    // NOUVEAU
    this.menuOpen = false;
    this.conversations = [];
    this.newPeer = '';
    this.recipientChanged.next(''); // réinitialise distinctUntilChanged
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
      // NOUVEAU : ignorer les messages d'une autre conversation
      if (e.from !== this.recipient) return;
      this.insert({
        id: e.id!,
        from: e.from!,
        content: e.content!,
        sequence: e.sequence,
        mine: false,
        createdAt: e.createdAt,
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
      createdAt: new Date().toISOString(),
      mine: true,
    });
    this.draft = '';
  }

  private insert(m: DisplayedMessage) {
    this.messages.push(m);
    this.messages.sort(
      (a, b) => (a.sequence ?? Infinity) - (b.sequence ?? Infinity),
    );
  }

  tick(status?: string): string {
    if (status === 'DELIVERED' || status === 'DELIVERY_NOTIFIED') return '✓✓';
    if (status === 'SENT') return '✓';
    return '';
  }

  // NOUVEAU : menu

  toggleMenu() {
    this.menuOpen = !this.menuOpen;
    if (this.menuOpen) this.loadConversations();
  }

  private loadConversations() {
    this.chat.conversations().subscribe({
      next: (list) => {
        this.conversations = list;
        this.cdr.markForCheck();
      },
      error: () => (this.conversations = []),
    });
  }

  // REMPLACE l'ancien openConversation()
  openConversation(peer: string) {
    this.menuOpen = false;
    if (peer === this.recipient) return; // déjà ouverte : rien à recharger
    this.recipient = peer;
    this.onRecipientChange(peer);
  }

  startConversation() {
    const peer = this.newPeer.trim();
    if (!peer || peer === this.username) return;
    this.newPeer = '';
    this.openConversation(peer);
  }
}