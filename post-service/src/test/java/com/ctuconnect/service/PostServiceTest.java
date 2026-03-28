package com.ctuconnect.service;

import com.ctuconnect.client.MediaServiceClient;
import com.ctuconnect.client.UserServiceClient;
import com.ctuconnect.dto.AuthorInfo;
import com.ctuconnect.dto.request.PostRequest;
import com.ctuconnect.dto.response.PostResponse;
import com.ctuconnect.entity.InteractionEntity;
import com.ctuconnect.entity.PostEntity;
import com.ctuconnect.repository.CommentRepository;
import com.ctuconnect.repository.InteractionRepository;
import com.ctuconnect.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock PostRepository postRepository;
    @Mock CommentRepository commentRepository;
    @Mock InteractionRepository interactionRepository;
    @Mock MediaServiceClient mediaServiceClient;
    @Mock UserServiceClient userServiceClient;
    @Mock EventService eventService;

    @InjectMocks PostService postService;

    private static final String AUTHOR_ID = "author-uuid-1";
    private static final String POST_ID   = "post-mongo-id-1";

    private PostEntity samplePost() {
        PostEntity post = new PostEntity();
        post.setId(POST_ID);
        post.setTitle("Test Post");
        post.setContent("Some content");
        post.setAuthor(AuthorInfo.builder().id(AUTHOR_ID).name("Test Author").build());
        post.setVisibility("PUBLIC");
        return post;
    }

    // ── getPostById ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPostById: returns PostResponse when post exists")
    void getPostById_found_returnsResponse() {
        PostEntity post = samplePost();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(interactionRepository.countByPostIdAndType(anyString(), any())).thenReturn(0L);
        when(commentRepository.countByPostId(anyString())).thenReturn(0L);
        when(postRepository.save(any())).thenReturn(post);

        PostResponse response = postService.getPostById(POST_ID, AUTHOR_ID);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(POST_ID);
    }

    @Test
    @DisplayName("getPostById: throws RuntimeException when post does not exist")
    void getPostById_notFound_throws() {
        when(postRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostById("missing-id", AUTHOR_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Post not found");
    }

    // ── updatePost ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updatePost: updates title and content when caller is the author")
    void updatePost_byAuthor_updatesFields() {
        PostEntity post = samplePost();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(postRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PostRequest req = new PostRequest();
        req.setTitle("Updated Title");
        req.setContent("Updated content");

        PostResponse response = postService.updatePost(POST_ID, req, AUTHOR_ID);

        assertThat(response.getTitle()).isEqualTo("Updated Title");
        verify(postRepository).save(any(PostEntity.class));
        verify(eventService).publishPostEvent(eq("POST_UPDATED"), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("updatePost: throws RuntimeException when caller is not the author")
    void updatePost_byNonAuthor_throws() {
        PostEntity post = samplePost();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.updatePost(POST_ID, new PostRequest(), "other-user"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only the author");
    }

    @Test
    @DisplayName("updatePost: throws RuntimeException when post does not exist")
    void updatePost_notFound_throws() {
        when(postRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.updatePost("missing", new PostRequest(), AUTHOR_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Post not found");
    }

    // ── deletePost ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deletePost: deletes post, comments, interactions, and publishes event")
    void deletePost_byAuthor_deletesAll() {
        PostEntity post = samplePost();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        postService.deletePost(POST_ID, AUTHOR_ID);

        verify(commentRepository).deleteByPostId(POST_ID);
        verify(interactionRepository).deleteByPostId(POST_ID);
        verify(postRepository).deleteById(POST_ID);
        verify(eventService).publishPostEvent(eq("POST_DELETED"), eq(POST_ID), eq(AUTHOR_ID), any());
    }

    @Test
    @DisplayName("deletePost: throws RuntimeException when caller is not the author")
    void deletePost_byNonAuthor_throws() {
        PostEntity post = samplePost();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(POST_ID, "intruder"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only the author");

        verify(postRepository, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("deletePost: throws RuntimeException when post does not exist")
    void deletePost_notFound_throws() {
        when(postRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deletePost("missing", AUTHOR_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Post not found");
    }

    // ── getAllPosts ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllPosts: returns paged PostResponse list")
    void getAllPosts_returnsMappedPage() {
        PostEntity post = samplePost();
        Pageable pageable = PageRequest.of(0, 10);
        Page<PostEntity> page = new PageImpl<>(List.of(post), pageable, 1);

        when(postRepository.findAll(pageable)).thenReturn(page);
        when(interactionRepository.countByPostIdAndType(anyString(), any())).thenReturn(0L);
        when(commentRepository.countByPostId(anyString())).thenReturn(0L);
        when(postRepository.saveAll(anyList())).thenReturn(List.of(post));

        Page<PostResponse> result = postService.getAllPosts(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(POST_ID);
    }
}
