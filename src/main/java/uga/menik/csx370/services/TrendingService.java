package uga.menik.csx370.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
