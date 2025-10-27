package uga.menik.csx370.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uga.menik.csx370.models.Post;
import uga.menik.csx370.models.User;


@Service
public class TrendingService {

    private final DataSource dataSource;

    @Autowired
    public TrendingService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<String> getTrendingHashtags() {
        List<String> trendingTags = new ArrayList<>();
        final String sql = "SELECT tag_text FROM hashtags ORDER BY usage_count DESC LIMIT 5";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                trendingTags.add(rs.getString("tag_text"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trendingTags;
    }


public List<Post> getTrendingPosts() {
    List<Post> trendingPosts = new ArrayList<>();

    final String trendingTagsSql = "SELECT tag_text FROM hashtags ORDER BY usage_count DESC LIMIT 5";
    final String postsSqlTemplate =
        "SELECT p.postId, p.userId, p.content, p.createdAt, u.firstName, u.lastName " +
        "FROM post p JOIN user u ON p.userId = u.userId " +
        "WHERE " +
        "({conditions}) " +
        "ORDER BY p.createdAt DESC LIMIT 20";

    try (Connection conn = dataSource.getConnection();
         PreparedStatement tagStmt = conn.prepareStatement(trendingTagsSql);
         ResultSet tagRs = tagStmt.executeQuery()) {

        // Step 1: collect top 5 hashtags
        List<String> topTags = new ArrayList<>();
        while (tagRs.next()) {
            topTags.add(tagRs.getString("tag_text"));
        }

        if (topTags.isEmpty()) return trendingPosts;

        // Step 2: dynamically build the WHERE clause
        StringBuilder conditions = new StringBuilder();
        for (int i = 0; i < topTags.size(); i++) {
            if (i > 0) conditions.append(" OR ");
            conditions.append("p.content LIKE ?");
        }

        // Step 3: prepare final SQL
        String postsSql = postsSqlTemplate.replace("{conditions}", conditions.toString());
        try (PreparedStatement postStmt = conn.prepareStatement(postsSql)) {
            for (int i = 0; i < topTags.size(); i++) {
                postStmt.setString(i + 1, "%" + topTags.get(i) + "%");
            }

            ResultSet postRs = postStmt.executeQuery();

            while (postRs.next()) {
                User user = new User(
                    String.valueOf(postRs.getInt("userId")),
                    postRs.getString("firstName"),
                    postRs.getString("lastName")
                );

                Post post = new Post(
                    String.valueOf(postRs.getInt("postId")),
                    postRs.getString("content"),
                    postRs.getString("createdAt"),
                    user,
                    0, 0, false, false
                );

                trendingPosts.add(post);
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
    }

    return trendingPosts;
}



}
