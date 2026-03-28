import React, { useState } from 'react';
import { Message } from '../../services/chatService';
import { useChat } from '../../contexts/ChatContext';
import { useAuth } from '../../contexts/AuthContext';
import { formatDistanceToNow } from 'date-fns';
import { vi } from 'date-fns/locale';
import {
  EllipsisHorizontalIcon,
  ArrowUturnLeftIcon,
  HeartIcon,
  FaceSmileIcon
} from '@heroicons/react/24/outline';
import { HeartIcon as HeartIconSolid } from '@heroicons/react/24/solid';

interface MessageBubbleProps {
  message: Message;
  showSenderInfo: boolean;
  showTimestamp: boolean;
  onReply: () => void;
  isMobile: boolean;
}

const MessageBubble: React.FC<MessageBubbleProps> = ({
  message,
  showSenderInfo,
  showTimestamp,
  onReply,
  isMobile
}) => {
  const { addReaction, removeReaction } = useChat();
  const { user } = useAuth();
  const [showActions, setShowActions] = useState(false);
  const [showReactions, setShowReactions] = useState(false);

  const isOwnMessage = !!user && message.senderId === user.id;

  const handleReaction = async (emoji: string) => {
    const existingReaction = message.reactions?.find((r: { userId: string; emoji: string }) => r.userId === user?.id && r.emoji === emoji);

    if (existingReaction) {
      await removeReaction(message.id);
    } else {
      await addReaction(message.id, emoji);
    }
    setShowReactions(false);
  };

  const getMessageTime = () => {
    try {
      return formatDistanceToNow(new Date(message.createdAt), {
        addSuffix: true,
        locale: vi
      });
    } catch {
      return '';
    }
  };

  const popularEmojis = ['👍', '❤️', '😂', '😮', '😢', '😡'];

  const renderReactions = () => {
    if (!message.reactions || message.reactions.length === 0) return null;

    // Group reactions by emoji
    const reactionGroups = message.reactions.reduce((groups: Record<string, typeof message.reactions>, reaction: { emoji: string; userId: string }) => {
      if (!groups[reaction.emoji]) {
        groups[reaction.emoji] = [];
      }
      groups[reaction.emoji].push(reaction);
      return groups;
    }, {});

    return (
      <div className="flex flex-wrap gap-1 mt-1">
        {Object.entries(reactionGroups).map(([emoji, reactions]: [string, any]) => (
          <button
            key={emoji}
            onClick={() => handleReaction(emoji)}
            className={`inline-flex items-center px-2 py-1 rounded-full text-xs border ${
              (reactions as any[]).some((r: { userId: string }) => r.userId === user?.id)
                ? 'bg-blue-100 border-blue-300 text-blue-700'
                : 'bg-gray-100 border-gray-300 text-gray-700 hover:bg-gray-200'
            }`}
          >
            <span className="mr-1">{emoji}</span>
            <span>{reactions.length}</span>
          </button>
        ))}
      </div>
    );
  };

  const renderReplyToMessage = () => {
    if (!message.replyToMessage) return null;

    return (
      <div className="mb-2 p-2 bg-gray-100 rounded-lg border-l-2 border-blue-500">
        <p className="text-xs text-gray-600 font-medium">
          {message.replyToMessage.senderName}
        </p>
        <p className="text-sm text-gray-800 truncate">
          {message.replyToMessage.content}
        </p>
      </div>
    );
  };

  const renderAttachment = () => {
    if (!message.attachment) return null;

    if (message.attachment.fileType.startsWith('image/')) {
      return (
        <div className="mt-2">
          <img
            src={message.attachment.fileUrl}
            alt={message.attachment.fileName}
            className="max-w-xs rounded-lg cursor-pointer hover:opacity-90"
            onClick={() => window.open(message.attachment!.fileUrl, '_blank')}
          />
        </div>
      );
    }

    return (
      <div className="mt-2 p-3 bg-gray-100 rounded-lg">
        <div className="flex items-center space-x-2">
          <div className="w-8 h-8 bg-blue-500 rounded flex items-center justify-center">
            <span className="text-white text-xs font-bold">📎</span>
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-gray-900 truncate">
              {message.attachment.fileName}
            </p>
            <p className="text-xs text-gray-500">
              {(message.attachment.fileSize / 1024 / 1024).toFixed(2)} MB
            </p>
          </div>
          <a
            href={message.attachment.fileUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-blue-600 hover:text-blue-800 text-sm font-medium"
          >
            Tải xuống
          </a>
        </div>
      </div>
    );
  };

  return (
    <div className={`flex ${isOwnMessage ? 'justify-end' : 'justify-start'} px-2 sm:px-0`}>
      <div className={`max-w-[85%] sm:max-w-xs lg:max-w-md ${isOwnMessage ? 'order-2' : 'order-1'}`}>
        {/* Sender Info */}
        {showSenderInfo && !isOwnMessage && (
          <div className="flex items-center space-x-1.5 sm:space-x-2 mb-1">
            {message.senderAvatar ? (
              <img
                src={message.senderAvatar}
                alt={message.senderName}
                className="w-5 h-5 sm:w-6 sm:h-6 rounded-full"
              />
            ) : (
              <div className="w-5 h-5 sm:w-6 sm:h-6 rounded-full bg-gray-300 flex items-center justify-center">
                <span className="text-xs font-medium text-gray-600">
                  {message.senderName.charAt(0).toUpperCase()}
                </span>
              </div>
            )}
            <span className="text-xs sm:text-sm font-medium text-gray-700">
              {message.senderName}
            </span>
          </div>
        )}

        {/* Message Container */}
        <div
          className="relative group"
          onMouseEnter={() => !isMobile && setShowActions(true)}
          onMouseLeave={() => !isMobile && setShowActions(false)}
        >
          {/* Message Bubble */}
          <div
            className={`relative px-3 sm:px-4 py-1.5 sm:py-2 rounded-2xl ${
              isOwnMessage
                ? 'bg-blue-500 text-white'
                : 'bg-gray-200 text-gray-900'
            } ${message.isDeleted ? 'opacity-60 italic' : ''}`}
          >
            {/* Reply Preview */}
            {renderReplyToMessage()}

            {/* Message Content */}
            <div className="break-words">
              {message.isDeleted ? (
                <span className="text-xs sm:text-sm">Tin nhắn đã được xóa</span>
              ) : (
                <>
                  <p className="text-xs sm:text-sm">{message.content}</p>
                  {message.isEdited && (
                    <span className="text-xs opacity-75 ml-2">(đã chỉnh sửa)</span>
                  )}
                </>
              )}
            </div>

            {/* Attachment */}
            {renderAttachment()}

            {/* Message Status (for own messages) */}
            {isOwnMessage && (
              <div className="text-xs opacity-75 mt-1 text-right">
                {message.status === 'SENT' && '✓'}
                {message.status === 'DELIVERED' && '✓✓'}
                {message.status === 'READ' && (
                  <span className="text-blue-200">✓✓</span>
                )}
              </div>
            )}
          </div>

          {/* Reactions */}
          {renderReactions()}

          {/* Action Buttons - Desktop only */}
          {!isMobile && showActions && !message.isDeleted && (
            <div
              className={`absolute top-0 ${
                isOwnMessage ? 'left-0 -translate-x-full' : 'right-0 translate-x-full'
              } flex items-center space-x-1 bg-white shadow-lg rounded-full px-2 py-1 border`}
            >
              {/* Quick reaction */}
              <button
                onClick={() => handleReaction('👍')}
                className="p-1 hover:bg-gray-100 rounded-full text-sm"
                title="Thích"
              >
                👍
              </button>

              {/* More reactions */}
              <div className="relative">
                <button
                  onClick={() => setShowReactions(!showReactions)}
                  className="p-1 hover:bg-gray-100 rounded-full"
                  title="Thêm phản ứng"
                >
                  <FaceSmileIcon className="w-4 h-4 text-gray-600" />
                </button>

                {/* Reactions Picker */}
                {showReactions && (
                  <div className="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 bg-white shadow-lg rounded-lg border p-2 flex space-x-1 z-10">
                    {popularEmojis.map((emoji) => (
                      <button
                        key={emoji}
                        onClick={() => handleReaction(emoji)}
                        className="p-1 hover:bg-gray-100 rounded text-lg"
                      >
                        {emoji}
                      </button>
                    ))}
                  </div>
                )}
              </div>

              {/* Reply */}
              <button
                onClick={onReply}
                className="p-1 hover:bg-gray-100 rounded-full"
                title="Trả lời"
              >
                <ArrowUturnLeftIcon className="w-4 h-4 text-gray-600" />
              </button>

              {/* More options */}
              <button
                className="p-1 hover:bg-gray-100 rounded-full"
                title="Thêm tùy chọn"
              >
                <EllipsisHorizontalIcon className="w-4 h-4 text-gray-600" />
              </button>
            </div>
          )}
        </div>

        {/* Timestamp */}
        {showTimestamp && (
          <div className={`text-xs text-gray-500 mt-1 ${
            isOwnMessage ? 'text-right' : 'text-left'
          }`}>
            {getMessageTime()}
          </div>
        )}
      </div>
    </div>
  );
};

export default MessageBubble;
