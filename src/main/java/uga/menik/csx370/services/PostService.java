package uga.menik.csx370.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uga.menik.csx370.models.Post;
import java.util.Collections;
import java.util.Comparator;
import uga.menik.csx370.models.User;


/*
 * Contains post related functions 
 */
@Service
public class PostService {
    // connection to database
    @Autowired
    private DataSource dataSource;

    public PostService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /*
     * Helper method to sort posts by most recent post first
     */
    private void sortPostsByDateDecending(List<Post> posts) {
        Collections.sort(posts, new Comparator<Post>() {
            @Override
            public int compare(Post a, Post b) {
                return b.getPostDate().compareTo(a.getPostDate());
            }
        });
    } // sortPostsByDateDecending

    /*
     * Helper method to get user by id
     */
    private User getUserById(int id) {
        User user = null;
        final String sql = "select * from user where userId = ?";
        try(Connection conn = dataSource.getConnection(); 
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if(rs.next()) {
                user = new User(
                    String.valueOf(rs.getInt("userId")),
                    rs.getString("firstName"),
                    rs.getString("lastName")
                );
            } // if

        } catch (SQLException e) {
            e.printStackTrace();
        } // try catch
        return user;
    } // getUserById

    /*
     * Gets all posts
     */
    public List<Post> getAllPosts() {
        List<Post> posts = new ArrayList<>();
        final String sql = "select * from post";

        try(Connection conn = dataSource.getConnection(); 
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = pstmt.executeQuery();
            // add posts from database to post list
            while(rs.next()) {
                int userId = rs.getInt("userId");
                User user = getUserById(userId); // add to UserService
                Post post = new Post(
                    String.valueOf(rs.getInt("postId")),
                    rs.getString("content"),
                    rs.getString("createdAt"),
                    user,
                    0,
                    0,
                    false,
                    false
                );
                posts.add(post);
            } // while

        } catch (SQLException e) {
            e.printStackTrace();
        } // try catch

        // sort posts
        sortPostsByDateDecending(posts);

        return posts;

    } // getAllPosts

    /*
     * Gets posts by specific user
     */
    public List<Post> getPostsByUser(String userId) {
        List<Post> posts = new ArrayList<>();
        final String sql = "select * from post where userId = ? order by createdAt desc";
        try(Connection conn = dataSource.getConnection(); 
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, Integer.parseInt(userId));
            ResultSet rs = pstmt.executeQuery();

            while(rs.next()) {
                User user = getUserById(rs.getInt("userId"));
                 Post post = new Post(
                     String.valueOf(rs.getInt("postId")),
                     rs.getString("content"),
                     rs.getString("createdAt"),
                     user,
                     0,
                     0,
                     false,
                     false
                 );
                 posts.add(post);
            } // while

        } catch (SQLException e) {
            e.printStackTrace();
        } // try catch
        return posts;
    } // getPostsByUser

    /*
     * Adding a new post
     */
    public void addPost(String userId, String content) {
        final String sql = "insert into post (userId, content, postDate, heartsCount, commentsCount, isBookmarked) values (?, ?, now(), 0, 0, false)";
        try(Connection conn = dataSource.getConnection(); 
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, Integer.parseInt(userId));
                pstmt.setString(2, content);
                pstmt.executeQuery();
                System.out.println("Post added to database by user " + userId);

        } catch (SQLException e) {
            e.printStackTrace();
        } // try catch
    } // addPost

    /*
     * Liking and unliking a post
     */
    public void toggleHeart(String postId, boolean isAdd) {
        final String sql = isAdd
            ? "update post set heartsCount = heartsCount + 1 where postId = ?"
            : "update post set heartsCount = greatest(heartsCount - 1, 0) where postId = ?";

        try(Connection conn = dataSource.getConnection(); 
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(postId));
            pstmt.executeQuery();
            System.out.println("Heart added to database on post " + postId);

        } catch (SQLException e) {
            e.printStackTrace();
        } // try catch
    } // toggleHeart

} // PostService 
