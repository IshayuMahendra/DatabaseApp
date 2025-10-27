-- Create the database.
create database if not exists csx370_mb_platform;

-- Use the created database.
use csx370_mb_platform;

-- Create the user table.
create table if not exists user (
    userId int auto_increment,
    username varchar(255) not null,
    password varchar(255) not null,
    firstName varchar(255) not null,
    lastName varchar(255) not null,
    primary key (userId),
    unique (username),
    constraint userName_min_length check (char_length(trim(userName)) >= 2),
    constraint firstName_min_length check (char_length(trim(firstName)) >= 2),
    constraint lastName_min_length check (char_length(trim(lastName)) >= 2)

-- Create the post table.
CREATE TABLE IF NOT EXISTS post (
    postId INT AUTO_INCREMENT,
    userId INT NOT NULL,
    content TEXT NOT NULL,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (postId),
    FOREIGN KEY (userId) REFERENCES user(userId)
);

-- Likes for posts.
CREATE TABLE IF NOT EXISTS likes (
    likeId INT AUTO_INCREMENT,
    userId INT NOT NULL,
    postId INT NOT NULL,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (likeId),
    UNIQUE (userId, postId),
    FOREIGN KEY (userId) REFERENCES user(userId),
    FOREIGN KEY (postId) REFERENCES post(postId)
);
-- Bookmarks for posts.
CREATE TABLE IF NOT EXISTS bookmarks (
    bookmarkId INT AUTO_INCREMENT,
    userId INT NOT NULL,
    postId INT NOT NULL,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (bookmarkId),
    UNIQUE (userId, postId),
    FOREIGN KEY (userId) REFERENCES user(userId),
    FOREIGN KEY (postId) REFERENCES post(postId)
);

-- Comments on posts.
CREATE TABLE IF NOT EXISTS comments (
    commentId INT AUTO_INCREMENT,
    userId INT NOT NULL,
    postId INT NOT NULL,
    commentText TEXT NOT NULL,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (commentId),
    FOREIGN KEY (userId) REFERENCES user(userId),
    FOREIGN KEY (postId) REFERENCES post(postId)
);

-- Notifications for post interactions.
CREATE TABLE IF NOT EXISTS notifications (
    notificationId INT AUTO_INCREMENT,
    userId INT NOT NULL,         -- user receiving the notification
    message VARCHAR(255) NOT NULL,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    isRead BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (notificationId),
    FOREIGN KEY (userId) REFERENCES user(userId)
);

-- Non-trivial feature: Select top 5 trending hastags
CREATE TABLE hashtags (
    tag_id INT AUTO_INCREMENT PRIMARY KEY,
    tag_text VARCHAR(100) NOT NULL,
    usage_count INT DEFAULT 1
);


);

-- Create the post table.
create table if not exists post (
    postId INT AUTO_INCREMENT,
    userId INT NOT NULL,
    content TEXT NOT NULL,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (postId),
    FOREIGN KEY (userId) REFERENCES user(userId)
);

-- Likes for posts.
CREATE TABLE IF NOT EXISTS likes (
    likeId INT AUTO_INCREMENT,
    userId INT NOT NULL,
    postId INT NOT NULL,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (likeId),
    UNIQUE (userId, postId),
    FOREIGN KEY (userId) REFERENCES user(userId),
    FOREIGN KEY (postId) REFERENCES post(postId)
);
-- Bookmarks for posts.
CREATE TABLE IF NOT EXISTS bookmarks (
    bookmarkId INT AUTO_INCREMENT,
    userId INT NOT NULL,
    postId INT NOT NULL,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (bookmarkId),
    UNIQUE (userId, postId),
    FOREIGN KEY (userId) REFERENCES user(userId),
    FOREIGN KEY (postId) REFERENCES post(postId)
);

-- Comments on posts.
CREATE TABLE IF NOT EXISTS comments (
    commentId INT AUTO_INCREMENT,
    userId INT NOT NULL,
    postId INT NOT NULL,
    commentText TEXT NOT NULL,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (commentId),
    FOREIGN KEY (userId) REFERENCES user(userId),
    FOREIGN KEY (postId) REFERENCES post(postId)
);

-- Notifications for post interactions.
CREATE TABLE IF NOT EXISTS notifications (
    notificationId INT AUTO_INCREMENT,
    userId INT NOT NULL,         -- user receiving the notification
    message VARCHAR(255) NOT NULL,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    isRead BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (notificationId),
    FOREIGN KEY (userId) REFERENCES user(userId)
);

-- Non-trivial feature: Select top 5 trending hastags
CREATE TABLE hashtags (
    tag_id INT AUTO_INCREMENT PRIMARY KEY,
    tag_text VARCHAR(100) NOT NULL,
    usage_count INT DEFAULT 1
);

-- Tracks who follows who
create table if not exists follows (
    followerId int not null,
    followingId int not null,
    createdAt datetime default CURRENT_TIMESTAMP,
    primary key (followerId, followingId),
    foreign key (followerId) references user(userId),
    foreign key (followingId) references user(userId)
);
