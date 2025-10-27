/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.
*/
package uga.menik.csx370.services;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uga.menik.csx370.models.Post;
import uga.menik.csx370.models.User;

@Service
public class PostService {

    @Autowired
    private JdbcTemplate jdbc;

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm a");

    // CREATE POST + HASHTAGS
    @Transactional
    public void createPost(int userId, String content) {
        System.out.println("PostService.createPost called with userId: " + userId + ", content: " + content); // Debug

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Post cannot be empty");
        }

        String trimmedContent = content.trim();

        String sql = "INSERT INTO post (userId, content) VALUES (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updated = jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, userId);
            ps.setString(2, trimmedContent);
            return ps;
        }, keyHolder);

        if (updated == 0) {
            throw new RuntimeException("No rows affected — post not inserted");
        }

        int postId = keyHolder.getKey().intValue();
        System.out.println("Post created with ID: " + postId); // Debug

        Set<String> hashtags = extractHashtags(trimmedContent);
        for (String tag : hashtags) {
            int tagId = getOrCreateHashtag(tag);
            jdbc.update("INSERT IGNORE INTO post_hashtag (postId, tagId) VALUES (?, ?)", postId, tagId);
        }
    }

    private Set<String> extractHashtags(String content) {
        Set<String> tags = new HashSet<>();
        Matcher m = Pattern.compile("#(\\w+)").matcher(content);
        while (m.find()) {
            tags.add(m.group(1).toLowerCase());
        }
        return tags;
    }

    private int getOrCreateHashtag(String tagText) {
        final String finalTagText = tagText.toLowerCase();

        String sql = "SELECT tagId FROM hashtag WHERE tagText = ?";
        List<Integer> ids = jdbc.query(sql, (rs, row) -> rs.getInt(1), finalTagText);
        if (!ids.isEmpty()) return ids.get(0);

        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO hashtag (tagText) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, finalTagText);
            return ps;
        }, kh);
        return kh.getKey().intValue();
    }

    // HOME FEED
    public List<Post> getHomeFeed(int currentUserId) {
        String sql = """
            SELECT p.postId, p.content, p.createdAt, u.userId, u.firstName, u.lastName,
                   COUNT(DISTINCT l.userId) as heartsCount,
                   COUNT(DISTINCT c.commentId) as commentsCount,
                   EXISTS(SELECT 1 FROM likes l2 WHERE l2.postId = p.postId AND l2.userId = ?) as isHearted,
                   EXISTS(SELECT 1 FROM bookmarks b WHERE b.postId = p.postId AND b.userId = ?) as isBookmarked
            FROM post p
            JOIN user u ON p.userId = u.userId
            LEFT JOIN likes l ON l.postId = p.postId
            LEFT JOIN comments c ON c.postId = p.postId
            WHERE p.userId = ? OR p.userId IN (
                SELECT followedId FROM follow WHERE followerId = ?
            )
            GROUP BY p.postId, u.userId, u.firstName, u.lastName
            ORDER BY p.createdAt DESC
            """;

        return jdbc.query(sql, this::mapPost, currentUserId, currentUserId, currentUserId, currentUserId);
    }

    // HASHTAG SEARCH
    public List<Post> searchByHashtags(String query, int currentUserId) {
        String[] tags = Arrays.stream(query.split("\\s+"))
                .filter(s -> s.startsWith("#"))
                .map(s -> s.substring(1).toLowerCase())
                .toArray(String[]::new);

        if (tags.length == 0) return List.of();

        String placeholders = String.join(",", Collections.nCopies(tags.length, "?"));
        String sql = "SELECT p.postId, p.content, p.createdAt, u.userId, u.firstName, u.lastName, " +
                     "COUNT(DISTINCT l.userId) as heartsCount, " +
                     "COUNT(DISTINCT c.commentId) as commentsCount, " +
                     "EXISTS(SELECT 1 FROM likes l2 WHERE l2.postId = p.postId AND l2.userId = ?) as isHearted, " +
                     "EXISTS(SELECT 1 FROM bookmarks b WHERE b.postId = p.postId AND b.userId = ?) as isBookmarked " +
                     "FROM post p " +
                     "JOIN user u ON p.userId = u.userId " +
                     "JOIN post_hashtag ph ON ph.postId = p.postId " +
                     "JOIN hashtag h ON h.tagId = ph.tagId " +
                     "LEFT JOIN likes l ON l.postId = p.postId " +
                     "LEFT JOIN comments c ON c.postId = p.postId " +
                     "WHERE h.tagText IN (" + placeholders + ") " +
                     "GROUP BY p.postId, u.userId, u.firstName, u.lastName " +
                     "HAVING COUNT(DISTINCT h.tagText) = ? " +
                     "ORDER BY p.createdAt DESC";

        List<Object> params = new ArrayList<>(Arrays.asList(tags));
        params.add(currentUserId);
        params.add(currentUserId);
        params.add(tags.length);

        return jdbc.query(sql, this::mapPost, params.toArray());
    }

    // FIXED: Uses your actual User constructor
    private Post mapPost(ResultSet rs, int row) throws SQLException {
        String postId = String.valueOf(rs.getInt("postId"));
        String content = rs.getString("content");
        Timestamp ts = rs.getTimestamp("createdAt");
        String postDate = ts.toLocalDateTime().format(DISPLAY_FORMAT);
        String userId = String.valueOf(rs.getInt("userId"));
        String firstName = rs.getString("firstName");
        String lastName = rs.getString("lastName");

        // Uses your existing User(String, String, String) constructor
        User user = new User(userId, firstName, lastName);

        return new Post(
            postId,
            content,
            postDate,
            user,
            rs.getInt("heartsCount"),
            rs.getInt("commentsCount"),
            rs.getBoolean("isHearted"),
            rs.getBoolean("isBookmarked")
        );
    }
}