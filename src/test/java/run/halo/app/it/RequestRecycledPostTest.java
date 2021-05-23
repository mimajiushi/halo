package run.halo.app.it;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import run.halo.app.exception.NotFoundException;
import run.halo.app.model.entity.Post;
import run.halo.app.model.enums.PostEditorType;
import run.halo.app.model.enums.PostStatus;
import run.halo.app.service.PostService;
import run.halo.app.utils.JsonUtils;

/**
 * request recycled post Test
 *
 * @author chenwenjie.star
 */
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = "halo.auth-enabled=false")
@Slf4j
@AutoConfigureMockMvc
public class RequestRecycledPostTest extends BaseApiTest {

    @Autowired
    private PostService postService;

    @Autowired
    private MockMvc mvc;

    private static final String ADMIN_POST_URI = "/api/admin/posts";
    private static final String ARCHIVES_POST_URI = "/archives/";

    @LocalServerPort
    int port;

    String slug;

    @BeforeEach
    void setUp() {
        slug = "recycle-post";
        MockitoAnnotations.initMocks(this);
        Assertions.assertNotNull(postService);
    }

    @Test
    public void requestRecycledPost() throws Exception {
        installBlog();
        // create test post
        Post post = createPost();
        Assertions.assertNotNull(post, "post must not be null");
        // update post status to recycle
        updatePostStatus(post.getId(), PostStatus.RECYCLE);
        post = postService.getBy(PostStatus.RECYCLE, post.getSlug());
        Assertions.assertNotNull(post, "post must not be null");
        // request post
        String errMsg = "the post status is recycle, but it can still be accessed";
        try {
            mvc.perform(get(ARCHIVES_POST_URI + slug))
                .andDo(print());
        } catch (Exception e) {
            Assertions.assertThrows(NotFoundException.class, () -> {
                throw e.getCause();
            }, errMsg);
            return;
        }
        Assertions.fail(errMsg);
    }

    private void updatePostStatus(Integer postId, PostStatus postStatus) throws Exception {
        mvc.perform(
            put(ADMIN_POST_URI + String.format("/%d/status/%s", postId, postStatus))
        ).andDo(print())
            .andExpect(status().isOk());
    }

    private Post createPost() throws Exception {
        Post post = new Post();
        post.setTitle("回收站文章");
        post.setOriginalContent("回收站文章");
        post.setFormatContent("<p>回收站文章</p>");
        post.setEditorType(PostEditorType.MARKDOWN);
        post.setStatus(PostStatus.PUBLISHED);
        post.setSummary("回收站文章");
        post.setSlug(slug);
        post.setTopPriority(0);
        post.setLikes(0L);
        post.setWordCount(5L);
        post.setCreateTime(new Date());
        post.setUpdateTime(new Date());
        post.setEditTime(new Date());
        MvcResult mvcResult = mvc.perform(
            post(ADMIN_POST_URI)
                .content(JsonUtils.objectToJson(post))
                .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        String response = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        int id = JsonPath.read(response, "$.data.id");
        post.setId(id);
        return post;
    }
}
