package com.ctuconnect.service;

import com.ctuconnect.client.UserServiceClient;
import com.ctuconnect.dto.AuthorInfo;
import com.ctuconnect.dto.request.CommentRequest;
import com.ctuconnect.dto.response.CommentResponse;
import com.ctuconnect.entity.CommentEntity;
import com.ctuconnect.entity.PostEntity;
import com.ctuconnect.repository.CommentRepository;
import com.ctuconnect.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock CommentRepository commentRepository;
    @Mock PostRepository postRepository;
    @Mock EventService eventService;
    @Mock UserServiceClient userServiceClient;

    @InjectMocks CommentService commentService;

    private static final String POST_ID    = "post-1";
    private static final String COMMENT_ID = "comment-1";
    private static final String AUTHOR_ID  = "author-1";

    private AuthorInfo author() {
        return AuthorInfo.builder().id(AUTHOR_ID).name("Test User").build();
    }

    private PostEntity post() {
        PostEntity p = new PostEntity();
        p.setId(POST_ID);
        return p;
    }

    private CommentEntity rootComment() {
        return new CommentEntity(POST_ID, "Root comment text", author());
    }

    // ── createComment ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createComment: saves root comment, increments post comment count, publishes event")
    void createComment_root_savesAndPublishes() {
        CommentRequest req = new CommentRequest();
        req.setContent("Hello world");
        // no parentCommentId → root comment

        PostEntity p = post();
        CommentEntity saved = new CommentEntity(POST_ID, "Hello world", author());
        saved.setId(COMMENT_ID);

        when(userServiceClient.getAuthorInfo(AUTHOR_ID)).thenReturn(author());
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(p));
        when(commentRepository.save(any())).thenReturn(saved);
        when(postRepository.save(any())).thenReturn(p);

        CommentResponse response = commentService.createComment(POST_ID, req, AUTHOR_ID);

        assertThat(response).isNotNull();
        verify(commentRepository).save(any(CommentEntity.class));
        verify(postRepository).save(any(PostEntity.class));
        verify(eventService).publishCommentEvent(eq("COMMENT_CREATED"), eq(POST_ID), anyString(), eq(AUTHOR_ID));
    }

    @Test
    @DisplayName("createComment: throws RuntimeException when post does not exist")
    void createComment_postNotFound_throws() {
        CommentRequest req = new CommentRequest();
        req.setContent("text");

        when(userServiceClient.getAuthorInfo(AUTHOR_ID)).thenReturn(author());
        when(postRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment("missing-post", req, AUTHOR_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Post not found");

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createComment: throws RuntimeException when parent comment does not exist")
    void createComment_parentNotFound_throws() {
        CommentRequest req = new CommentRequest();
        req.setContent("reply text");
        req.setParentCommentId("nonexistent-parent");

        PostEntity p = post();
        when(userServiceClient.getAuthorInfo(AUTHOR_ID)).thenReturn(author());
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(p));
        when(commentRepository.findById("nonexistent-parent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(POST_ID, req, AUTHOR_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Parent comment not found");

        verify(commentRepository, never()).save(any());
    }

    // ── deleteComment ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteComment: deletes comment and publishes event when caller is the author")
    void deleteComment_byAuthor_deletesAndPublishes() {
        CommentEntity comment = rootComment();
        comment.setId(COMMENT_ID);
        PostEntity p = post();

        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(commentRepository.findByParentCommentId(COMMENT_ID)).thenReturn(Collections.emptyList());
        when(commentRepository.findFlattenedReplies(anyString(), anyInt())).thenReturn(Collections.emptyList());
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(p));
        when(postRepository.save(any())).thenReturn(p);

        commentService.deleteComment(COMMENT_ID, AUTHOR_ID);

        verify(commentRepository).deleteById(COMMENT_ID);
        verify(eventService).publishCommentEvent(eq("COMMENT_DELETED"), eq(POST_ID), eq(COMMENT_ID), eq(AUTHOR_ID));
    }

    @Test
    @DisplayName("deleteComment: throws RuntimeException when caller is not the author")
    void deleteComment_byNonAuthor_throws() {
        CommentEntity comment = rootComment();
        comment.setId(COMMENT_ID);

        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteComment(COMMENT_ID, "intruder"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only the author");

        verify(commentRepository, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("deleteComment: throws RuntimeException when comment does not exist")
    void deleteComment_notFound_throws() {
        when(commentRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment("missing", AUTHOR_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Comment not found");
    }
}
