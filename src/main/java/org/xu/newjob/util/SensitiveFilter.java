package org.xu.newjob.util;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    // 替换符
    private static final String REPLACEMENT = "***";
    // 根节点
    private TrieNode root = new TrieNode();

    @PostConstruct
    public void init() {
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))
        ) {
            String keyword;
            while ((keyword = reader.readLine()) != null) {
                // 添加到前缀树
                this.addKeyword(keyword);
            }
        } catch (IOException e) {
            logger.error("加载敏感词文件错误: " + e.getMessage());
        }
    }

    /**
     * 将一个敏感词添加到前缀树中
     * @param keyword 敏感词
     */
    private void addKeyword(String keyword) {
        TrieNode tempNode = root;
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);
            if (subNode == null) { // 如果没有就添加，有了就不用再添加了，
                subNode = new TrieNode();
                tempNode.setSubNode(c, subNode);
            }
            tempNode = subNode;
            // 判断是否结束
            if (i == keyword.length() - 1) {
                tempNode.setKeywordEnd(true);
            }
        }
    }

    /**
     * 过滤敏感词
     * @param text 原始文本
     * @return 过滤后的文本
     */
    public String filter(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        TrieNode tempNode = root;
        int start = 0;
        int end = 0;
        StringBuilder sb = new StringBuilder();

        while (end < text.length()) {
            char c = text.charAt(end);
            // 跳过符号
            if (isSymbol(c)) {
                // 若指针1处于根节点，将此符号计入结果，让指针2向下走一步
                if (tempNode == root) {
                    sb.append(c);
                    start++;
                }
                // 否则指针1不动，end跳过特殊符号
                end++;
                continue;
            }
            // 检查下级节点
            tempNode = tempNode.getSubNode(c);
            if (tempNode == null) {
                // 说明start - end 不是敏感词
                sb.append(text.charAt(start));
                end = ++start;
                tempNode = root;
            } else if (tempNode.isKeywordEnd()) {
                // 说明 start - end 是敏感词，已经检查结束
                sb.append(REPLACEMENT);
                start = ++end;
                tempNode = root;
            } else {
                // 说明正在检查过程中，start - end有可能是敏感词，需要继续往后检查
                end++;
            }
        }
        // 将最后的一个 start - end 加入结果
        sb.append(text.substring(start));

        return sb.toString();
    }

    // 判断是否为特殊符号
    private boolean isSymbol(Character c) {
        // 0x2E80 - 0x9FFF 是东亚的文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    // 前缀树
    private class TrieNode {

        // 关键词结束的标识（是否时叶子节点）
        private boolean isKeywordEnd = false;
        // 子节点（key时下级字符，value是下级节点）
        private Map<Character, TrieNode> subNode = new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        public TrieNode getSubNode(Character c) {
            return subNode.get(c);
        }

        public void setSubNode(Character c, TrieNode node) {
            subNode.put(c, node);
        }
    }

}
