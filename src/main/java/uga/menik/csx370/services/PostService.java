/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.
*/
package uga.menik.csx370.services;

import java.sql.Connection;
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

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uga.menik.csx370.models.Comment;
import uga.menik.csx370.models.ExpandedPost;
import uga.menik.csx370.models.Post;
import uga.menik.csx370.models.User;

@Service
public class PostService {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbc;

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm a");

    public PostService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private void sortPostsByDateDecending(List<Post> posts) {
        Collections.sort(posts, (a, b) -> b.getPostDate().compareTo(a.getPostDate()));
    }

    private User getUserById(int id) {
        User user = null;
        final String sql = "SELECT * FROM user WHERE userId = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                user = new User(
                    String.valueOf(rs.getInt("userId")),
                    rs.getString("firstName"),
                    rs.getString("lastName")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }

    public List<Post> getAllPosts() {
        List<Post> posts = new ArrayList<>();
        final String sql = "SELECT * FROM post";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int userId = rs.getInt("userId");
                User user = getUserById(userId);
                Post post = new Post(
                    String.valueOf(rs.getInt("postId")),
                    rs.getString("content"),
                    rs.getString("createdAt"),
                    user,
                    0, 0, false, false
                );
                posts.add(post);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        sortPostsByDateDecending(posts);
        return posts;
    }

    public List<Post> getPostsByUser(String userId) {
    List<Post> posts = new ArrayList<>();
    String sql = """
        SELECT p.postId, p.userId, p.content, p.createdAt,
               u.firstName, u.lastName,
               COUNT(DISTINCT l.userId) as heartsCount,
               COUNT(DISTINCT c.commentId) as commentsCount,
               EXISTS(SELECT 1 FROM likes l2 WHERE l2.postId = p.postId AND l2.userId = ?) as isHearted,
               EXISTS(SELECT 1 FROM bookmarks b WHERE b.postId = p.postId AND b.userId = ?) as isBookmarked
        FROM post p
        JOIN user u ON p.userId = u.userId
        LEFT JOIN likes l ON l.postId = p.postId
        LEFT JOIN comments c ON c.postId = p.postId
        WHERE p.userId = ?
        GROUP BY p.postId, u.userId, u.firstName, u.lastName
        ORDER BY p.createdAt DESC
        """;

    try (Connection conn = dataSource.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        int loggedInUserId = Integer.parseInt(userId);
        pstmt.setInt(1, loggedInUserId);
        pstmt.setInt(2, loggedInUserId);
        pstmt.setInt(3, Integer.parseInt(userId));

        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            String postId = String.valueOf(rs.getInt("postId"));
            String content = rs.getString("content");
            String createdAt = rs.getTimestamp("createdAt").toLocalDateTime().format(DISPLAY_FORMAT);
            String postUserId = String.valueOf(rs.getInt("userId"));
            String firstName = rs.getString("firstName");
            String lastName = rs.getString("lastName");
            int heartsCount = rs.getInt("heartsCount");
            int commentsCount = rs.getInt("commentsCount");  // ADDED
            boolean isHearted = rs.getBoolean("isHearted");
            boolean isBookmarked = rs.getBoolean("isBookmarked");

            User user = new User(postUserId, firstName, lastName);
            Post post = new Post(postId, content, createdAt, user, heartsCount, commentsCount, isHearted, isBookmarked);
            posts.add(post);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return posts;
}

    public void addPost(String userId, String content) {
        final String sql = "INSERT INTO post (userId, content, postDate, heartsCount, commentsCount, isBookmarked) VALUES (?, ?, NOW(), 0, 0, FALSE)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(userId));
            pstmt.setString(2, content);
            pstmt.executeUpdate();
            updateHashtagCounts(content);
            System.out.println("Post added to database by user " + userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean addBookmark(int userId, int postId) {
        final String sql = "INSERT INTO bookmarks (userId, postId) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, postId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeBookmark(int userId, int postId) {
        final String sql = "DELETE FROM bookmarks WHERE userId = ? AND postId = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, postId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Post> getBookmarkedPosts(String userId) {
        List<Post> posts = new ArrayList<>();
        String sql = """
            SELECT p.postId, p.userId, p.content, p.createdAt,
                   u.firstName, u.lastName,
                   COUNT(DISTINCT l.userId) as heartsCount,
                   EXISTS(SELECT 1 FROM likes l2 WHERE l2.postId = p.postId AND l2.userId = ?) as isHearted
            FROM post p
            JOIN user u ON p.userId = u.userId
            JOIN bookmarks b ON b.postId = p.postId
            LEFT JOIN likes l ON l.postId = p.postId
            WHERE b.userId = ?
            GROUP BY p.postId, u.userId, u.firstName, u.lastName
            ORDER BY p.createdAt DESC
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(userId));
            pstmt.setInt(2, Integer.parseInt(userId));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String postId = String.valueOf(rs.getInt("postId"));
                String content = rs.getString("content");
                String createdAt = rs.getTimestamp("createdAt").toString();
                String postUserId = String.valueOf(rs.getInt("userId"));
                String firstName = rs.getString("firstName");
                String lastName = rs.getString("lastName");
                int heartsCount = rs.getInt("heartsCount");
                boolean isHearted = rs.getBoolean("isHearted");
                boolean isBookmarked = true;
                User user = new User(postUserId, firstName, lastName);
                Post post = new Post(postId, content, createdAt, user, heartsCount, 0, isHearted, isBookmarked);
                posts.add(post);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }

    public String getLastPostTimeForUser(int userId) {
        String sql = "SELECT createdAt FROM post WHERE userId = ? ORDER BY createdAt DESC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("createdAt");
                if (ts != null) {
                    return ts.toLocalDateTime().format(DISPLAY_FORMAT);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "No posts yet";
    }

    @Transactional
    public void createPost(int userId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Post cannot be empty");
        }
        String trimmedContent = content.trim();
        String sql = "INSERT INTO post (userId, content) VALUES (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, userId);
            ps.setString(2, trimmedContent);
            return ps;
        }, keyHolder);
        int postId = keyHolder.getKey().intValue();
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

    String selectSql = "SELECT tag_id FROM hashtags WHERE tag_text = ?";
    List<Integer> ids = jdbc.query(selectSql, (rs, row) -> rs.getInt(1), finalTagText);

    if (!ids.isEmpty()) {
        jdbc.update("UPDATE hashtags SET usage_count = usage_count + 1 WHERE tag_id = ?", ids.get(0));
        return ids.get(0);
    }
    KeyHolder kh = new GeneratedKeyHolder();
    jdbc.update(connection -> {
        PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO hashtags (tag_text, usage_count) VALUES (?, 1)", 
            Statement.RETURN_GENERATED_KEYS
        );
        ps.setString(1, finalTagText);
        return ps;
    }, kh);

    return kh.getKey().intValue();
}

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
                SELECT followingId FROM follow WHERE followerId = ?
            )
            GROUP BY p.postId, u.userId, u.firstName, u.lastName
            ORDER BY p.createdAt DESC
            """;
        return jdbc.query(sql, this::mapPost, currentUserId, currentUserId, currentUserId, currentUserId);
    }

    public List<Post> searchByHashtags(String query, int currentUserId) {
        if (query == null || query.trim().isEmpty()) return List.of();
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
                     "ORDER BY p.createdAt DESC";
        List<Object> params = new ArrayList<>(Arrays.asList(tags));
        params.add(currentUserId);
        params.add(currentUserId);
        return jdbc.query(sql, this::mapPost, params.toArray());
    }

    private Post mapPost(ResultSet rs, int row) throws SQLException {
        String postId = String.valueOf(rs.getInt("postId"));
        String content = rs.getString("content");
        Timestamp ts = rs.getTimestamp("createdAt");
        String postDate = ts.toLocalDateTime().format(DISPLAY_FORMAT);
        String userId = String.valueOf(rs.getInt("userId"));
        String firstName = rs.getString("firstName");
        String lastName = rs.getString("lastName");
        User user = new User(userId, firstName, lastName);
        return new Post(postId, content, postDate, user,
            rs.getInt("heartsCount"), rs.getInt("commentsCount"),
            rs.getBoolean("isHearted"), rs.getBoolean("isBookmarked")
        );
    }

    public boolean addComment(int userId, int postId, String comment) {
        if (comment == null || comment.isBlank()) return false;
        String sql = "INSERT INTO comments (userId, postId, commentText, createdAt) VALUES (?, ?, ?, NOW())";
        try {
            jdbc.update(sql, userId, postId, comment.trim());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addLike(int userId, int postId) {
        String sql = "INSERT IGNORE INTO likes (userId, postId) VALUES (?, ?)";
        try {
            jdbc.update(sql, userId, postId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeLike(int userId, int postId) {
        String sql = "DELETE FROM likes WHERE userId = ? AND postId = ?";
        try {
            jdbc.update(sql, userId, postId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void updateHashtagCounts(String content) {
        final String insertOrUpdate = 
            "INSERT INTO hashtag (tagText) VALUES (?) " +
            "ON DUPLICATE KEY UPDATE usage_count = usage_count + 1";
        Pattern pattern = Pattern.compile("#(\\w+)");
        Matcher matcher = pattern.matcher(content);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertOrUpdate)) {
            while (matcher.find()) {
                String tag = matcher.group(1).toLowerCase();
                stmt.setString(1, tag);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ExpandedPost getPostWithComments(int postId, int currentUserId) {
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
        WHERE p.postId = ?
        GROUP BY p.postId, u.userId, u.firstName, u.lastName
        """;

    List<ExpandedPost> list = jdbc.query(sql, (rs, row) -> {
        User user = new User(
            String.valueOf(rs.getInt("userId")),
            rs.getString("firstName"),
            rs.getString("lastName")
        );

        List<Comment> comments = getCommentsForPost(postId);

        return new ExpandedPost(
            String.valueOf(rs.getInt("postId")),
            rs.getString("content"),
            rs.getTimestamp("createdAt").toLocalDateTime().format(DISPLAY_FORMAT),
            user,
            rs.getInt("heartsCount"),
            rs.getInt("commentsCount"),
            rs.getBoolean("isHearted"),
            rs.getBoolean("isBookmarked"),
            comments
        );
    }, currentUserId, currentUserId, postId);

    return list.isEmpty() ? null : list.get(0);
}

    private List<Comment> getCommentsForPost(int postId) {
        String sql = """
            SELECT c.commentId, c.commentText AS content, c.createdAt,
                   u.userId, u.firstName, u.lastName
            FROM comments c
            JOIN user u ON c.userId = u.userId
            WHERE c.postId = ?
            ORDER BY c.createdAt ASC
            """;

        return jdbc.query(sql, (rs, row) -> {
            User user = new User(
                String.valueOf(rs.getInt("userId")),
                rs.getString("firstName"),
                rs.getString("lastName")
            );
            return new Comment(
                String.valueOf(rs.getInt("commentId")),
                rs.getString("content"),
                rs.getTimestamp("createdAt").toLocalDateTime().format(DISPLAY_FORMAT),
                user
            );
        }, postId);
    }
        public int getCommentsCount(int postId) {
        String sql = "SELECT COUNT(*) FROM comments WHERE postId = ?";
        try {
            Integer count = jdbc.queryForObject(sql, Integer.class, postId);
            return count != null ? count : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}