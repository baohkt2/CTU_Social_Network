import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import MessageBubble from '../MessageBubble';
import { ChatMessage, MessageType } from '../../../shared/types/chat';
import { User } from '../../../shared/types/user';

// ── Mocks ────────────────────────────────────────────────────────────────────

const CURRENT_USER_ID = 'user-123';
const OTHER_USER_ID = 'user-456';

jest.mock('../../../contexts/AuthContext', () => ({
  useAuth: () => ({
    user: { id: CURRENT_USER_ID, createdAt: '', updatedAt: '' } as User,
  }),
}));

jest.mock('../../../contexts/ChatContext', () => ({
  useChat: () => ({
    addReaction: jest.fn(),
    removeReaction: jest.fn(),
  }),
}));

// ── Helpers ──────────────────────────────────────────────────────────────────

function makeMessage(overrides: Partial<ChatMessage> = {}): ChatMessage {
  return {
    id: 'msg-1',
    content: 'Hello world',
    type: MessageType.TEXT,
    roomId: 'room-1',
    senderId: OTHER_USER_ID,
    sender: { id: OTHER_USER_ID, createdAt: '', updatedAt: '' } as User,
    senderName: 'Other User',
    senderAvatar: '',
    isRead: false,
    isEdited: false,
    isDeleted: false,
    reactions: [],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    ...overrides,
  } as unknown as ChatMessage;
}

const defaultProps = {
  showSenderInfo: true,
  showTimestamp: false,
  onReply: jest.fn(),
  isMobile: false,
};

// ── Tests ────────────────────────────────────────────────────────────────────

describe('MessageBubble — isOwnMessage', () => {
  it('aligns to the right when the message belongs to the current user', () => {
    const message = makeMessage({ senderId: CURRENT_USER_ID });
    const { container } = render(
      <MessageBubble message={message} {...defaultProps} />
    );
    // Own messages use justify-end on the outer flex container
    const outerDiv = container.firstChild as HTMLElement;
    expect(outerDiv).toHaveClass('justify-end');
  });

  it('aligns to the left when the message belongs to another user', () => {
    const message = makeMessage({ senderId: OTHER_USER_ID });
    const { container } = render(
      <MessageBubble message={message} {...defaultProps} />
    );
    const outerDiv = container.firstChild as HTMLElement;
    expect(outerDiv).toHaveClass('justify-start');
  });

  it('renders the bubble with blue background for own messages', () => {
    const message = makeMessage({ senderId: CURRENT_USER_ID });
    const { container } = render(
      <MessageBubble message={message} {...defaultProps} />
    );
    // The bubble div carries bg-blue-500 for own messages
    const bubble = container.querySelector('.bg-blue-500');
    expect(bubble).toBeInTheDocument();
  });

  it('renders the bubble with gray background for other users\' messages', () => {
    const message = makeMessage({ senderId: OTHER_USER_ID });
    const { container } = render(
      <MessageBubble message={message} {...defaultProps} />
    );
    const bubble = container.querySelector('.bg-gray-200');
    expect(bubble).toBeInTheDocument();
  });

  it('hides sender info for own messages', () => {
    const message = makeMessage({ senderId: CURRENT_USER_ID, senderName: 'Me' } as any);
    render(<MessageBubble message={message} {...defaultProps} />);
    expect(screen.queryByText('Me')).not.toBeInTheDocument();
  });

  it('shows sender info for other users\' messages', () => {
    const message = makeMessage({ senderId: OTHER_USER_ID } as any);
    // senderName is on the legacy Message shape; set it via cast
    (message as any).senderName = 'Other User';
    render(<MessageBubble message={message} {...defaultProps} />);
    expect(screen.getByText('Other User')).toBeInTheDocument();
  });
});
