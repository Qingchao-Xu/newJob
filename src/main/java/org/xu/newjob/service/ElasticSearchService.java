package org.xu.newjob.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.stereotype.Service;
import org.xu.newjob.dao.elasticsearch.DiscussPostRepository;
import org.xu.newjob.entity.DiscussPost;

import java.util.ArrayList;
import java.util.List;

@Service
public class ElasticSearchService {

    @Autowired
    private ElasticsearchTemplate template;
    @Autowired
    private DiscussPostRepository discussPostRepository;

    public void saveDiscussPost(DiscussPost post) {
        discussPostRepository.save(post);
    }

    public void deleteDiscussPost(int id) {
        discussPostRepository.deleteById(id);
    }

    public Page<DiscussPost> searchDiscussPost(String keyword, int current, int limit) {
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

        return new PageImpl<>(list, query.getPageable(), search.getTotalHits());
    }



}
