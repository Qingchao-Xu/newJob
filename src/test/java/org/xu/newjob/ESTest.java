package org.xu.newjob;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.test.context.ContextConfiguration;
import org.xu.newjob.dao.DiscussPostMapper;
import org.xu.newjob.dao.elasticsearch.DiscussPostRepository;
import org.xu.newjob.entity.DiscussPost;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@ContextConfiguration(classes = NewJobApplication.class)
public class ESTest {

    @Autowired
    private ElasticsearchTemplate template;
    @Autowired
    private DiscussPostRepository discussPostRepository;
    @Autowired
    private DiscussPostMapper discussPostMapper;

    /**
     * 是否存在索引
     */
    @Test
    public void exitsIndex() {
        IndexOperations indexOperations = template.indexOps(DiscussPost.class);
        boolean exists = indexOperations.exists();
        System.out.println(exists);
    }

    /**
     * 删除索引
     */
    @Test
    public void deleteIndex() {
        IndexOperations indexOperations = template.indexOps(DiscussPost.class);
        boolean delete = indexOperations.delete();
        System.out.println(delete);
        boolean exists = indexOperations.exists();
        System.out.println(exists);
    }

    // 测试插入
    @Test
    public void testInsert() {
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(241));
        System.out.println(discussPostMapper.selectDiscussPostById(241));
    }

    // 测试删除
    @Test
    public void testDelete() {
        discussPostRepository.deleteById(241);
    }

    // 测试查询
    @Test
    public void testQuery() {
        String keyword = "华人";
        int current = 0;
        int limit = 10;

        HighlightField title = new HighlightField("title",
                new HighlightFieldParameters.HighlightFieldParametersBuilder()
                        .withPreTags("<em>")
                        .withPostTags("</em>").build());
        HighlightField content = new HighlightField("content",
                new HighlightFieldParameters.HighlightFieldParametersBuilder()
                        .withPreTags("<em>")
                        .withPostTags("</em>").build());

        ArrayList<HighlightField> fields = new ArrayList<>();
        fields.add(title);
        fields.add(content);
        Highlight highlight = new Highlight(fields);


        NativeQuery query = NativeQuery.builder().withQuery(q -> q.multiMatch(m -> m.fields("title", "content").query(keyword)))
                .withSort(Sort.by("type").descending())
                .withSort(Sort.by("score").descending())
                .withSort(Sort.by("createTime").descending())
                .withPageable(Pageable.ofSize(limit).withPage(current))
                .withHighlightQuery(new HighlightQuery(highlight, DiscussPost.class)).build();

        SearchHits<DiscussPost> search = template.search(query, DiscussPost.class);
        SearchPage<DiscussPost> page = SearchHitSupport.searchPageFor(search, query.getPageable());
        System.out.println(page.getTotalElements());
        System.out.println(page.getTotalPages());
        System.out.println(page.getNumber());
        System.out.println(page.getSize());
        for (SearchHit<DiscussPost> discussPostSearchHit : page) {
            System.out.println(discussPostSearchHit.getHighlightFields()); // 高亮的内容
            System.out.println(discussPostSearchHit.getContent()); // 原始的DiscussPost
        }
        // 将带有高亮内容的DiscussPost封装到page对象中
        List<DiscussPost> list = new ArrayList<>();
        for (SearchHit<DiscussPost> discussPostSearchHit : page) {
            DiscussPost discussPost = discussPostSearchHit.getContent();
            if (discussPostSearchHit.getHighlightFields().get("title") != null) {
                discussPost.setTitle(discussPostSearchHit.getHighlightFields().get("title").get(0));
            }
            if (discussPostSearchHit.getHighlightFields().get("content") != null) {
                discussPost.setContent(discussPostSearchHit.getHighlightFields().get("content").get(0));
            }
            list.add(discussPost);
        }

        Page<DiscussPost> pageInfo = new PageImpl<>(list, query.getPageable(), search.getTotalHits());
        System.out.println(pageInfo.getTotalElements());
        System.out.println(pageInfo.getTotalPages());
        System.out.println(pageInfo.getNumber());
        System.out.println(pageInfo.getSize());
        for (DiscussPost discussPost : pageInfo) {
            System.out.println(discussPost);
        }
    }


}
