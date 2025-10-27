Ishayu Mahendra - Github moderation, demo video and read me
Samhit - Making posts, hashtags, post functionality, sql queries
Khan - Unique feature, logged in user behavior, comments, data
kayla - hashtag, profile page, webpage functionality

How to run (same as project instructions taken from that): 

  Navigate to the directory with the pom.xml using the terminal in your local machine and run the following command:
  On unix like machines:
  mvn spring-boot:run -Dspring-boot.run.jvmArguments='-Dserver.port=8081'
  On windows command line:
  mvn spring-boot:run -D"spring-boot.run.arguments=--server.port=8081"
  On windows power shell:
  mvn spring-boot:run --% -Dspring-boot.run.arguments="--server.port=8081"


Non-trivial feature information:
  The non-trivial feature that Khan added is the Trending Hashtags System, which identifies and displays the top 5 most frequently used hashtags by querying the database and updating the UI dynamically.

New Feature: Trending Hashtags
  
  This feature adds a Trending Hashtags page that displays the top 5 most used hashtags across all posts. It helps users see what topics are currently popular on the platform.

Access:
  Visit http://localhost:8081/trending to view the trending list.

Implementation:

  UI: trending.mustache : shows hashtags with their usage counts.
  
  Controller: TrendingController.java : handles /trending requests and passes data to the view.
  
  Service: TrendingService.java : retrieves top hashtags from the database.
  
  SQL/Schema: Added a hashtags table
