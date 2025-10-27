/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.csx370.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uga.menik.csx370.models.FollowableUser;
import uga.menik.csx370.models.User;
import uga.menik.csx370.utility.Utility;

/**
 * This service contains people related functions.
 */
@Service
public class PeopleService {
    @Autowired
    private final DataSource dataSource;
    @Autowired
    private PostService postService;
    
    @Autowired
    public PeopleService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * This function should query and return all users that 
     * are followable. The list should not contain the user 
     * with id userIdToExclude.
     */
    public List<FollowableUser> getFollowableUsers(String userIdToExclude) {
        // Write an SQL query to find the users that are not the current user.

        // Run the query with a datasource.
        // See UserService.java to see how to inject DataSource instance and
        // use it to run a query.
    
        // Use the query result to create a list of followable users.
        // See UserService.java to see how to access rows and their attributes
        // from the query result.
        // Check the following createSampleFollowableUserList function to see 
        // how to create a list of FollowableUsers.    

        // Replace the following line and return the list you created.

        List<FollowableUser> followableUsers = new ArrayList<>();

        final String sql = "select * from user where userId != ?";

        try (Connection conn = dataSource.getConnection();
                        PreparedStatement pstmt = conn.prepareStatement(sql)) {
                            pstmt.setString(1, userIdToExclude);
            try (ResultSet rs = pstmt.executeQuery()) {
                // Traverse the result rows one at a time.
                // Note: This specific while loop will only run at most once 
                // since username is unique.
                while (rs.next()) {
                    String userId = rs.getString("userId");
                    String firstName = rs.getString("firstName");
                    String lastName = rs.getString("lastName");
                    boolean isFollowed = false;
                    String lastActiveDate = "";

                    FollowableUser user = new FollowableUser(userId, firstName, lastName, isFollowed, lastActiveDate);
                    followableUsers.add(user);
                }
            } 
        }

        catch(SQLException e) {
            e.printStackTrace();
        }
        return followableUsers;
    }

    /*
     * To follow user
     */
    public void followUser(int followerId, int followingId) {
        final String sql = "insert ignore into follows (followerId, followingId) values (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, followerId);
            pstmt.setInt(2, followingId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    } // followUser

    /*
     * To unfollow user
     */
    public void unfollowUser(int followerId, int followingId) {
        final String sql = "delete from follows where followerId = ? and followingId = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, followerId);
            pstmt.setInt(2, followingId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    } // unfollowUser

    /*
     * Get users except for loggedin user
     */
    public List<Map<String, String>> getAllUsersExcept(int loggedInUserId) {
        List<Map<String, String>> users = new ArrayList<>();

        final String sql = "SELECT userId, firstName, lastName FROM user WHERE userId != ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, loggedInUserId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int userId = rs.getInt("userId");
                String first = rs.getString("firstName");
                String last = rs.getString("lastName");

                // Get last post time from PostService
                String lastPostTime = postService.getLastPostTimeForUser(userId);

                Map<String, String> item = new HashMap<>();
                item.put("userId", String.valueOf(userId));
                item.put("name", first + " " + last);
                item.put("lastPostTime", lastPostTime);

                users.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }


}
