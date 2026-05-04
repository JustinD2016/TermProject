# Caveman Like Actors
ActorDle: The Daily Actor Guessing Game
CSX370 Term Project

Members and Contributions:
Connor Ray: Implemented the StandingsController.sql, Wrote security.txt, ddl.sql

Jacob Stewart: Created ProfleController.sql, queries.sql file, Copied database_setup.sql contents into ddl.sql for submission.

Justin Dorval: Create Gameserivce.java Actor.java, GameSession.java, Game_page.mustache, GameController.java, Data.sql, Datasource.txt, Generate_data_sql.py, Databasesetup.dql

Aaryan Gupta: Implemented all files associated with Lookup page, drew ER diagram

Keaton Brown: Created Feed.java, FeedController.java, feed_page.mustache, feed_game_card.mustache, and feed_games_container.mustache. Recorded video and did style adjustments.

Technologies Used:
- Java 21
- Spring Boot 3.3.4
- Maven
- MySQL 8.1 (via Docker)
- JDBC / HikariCP
- JMustache (templating)
- BCrypt via Spring Security Crypto (password hashing)
- Chart.js (CDN, MIT License) — pie chart on standings page
- IMDb Non-Commercial Datasets (actor/title data)

How to Run the Project:
Database Setup:
1) Start Docker and MySQL container:
   docker start mysql-server-x370
2) Copy and run the setup file:
   docker cp database_setup.sql mysql-server-x370:/database_setup.sql
   docker exec -it mysql-server-x370 bash
   mysql -u root -p
   USE term_project;
   SOURCE /database_setup.sql;

To Run:
1) Clone the repo
2) Move into the project directory (TermProject)
3) Complete database setup above
4) Run the application: mvn spring-boot:run
5) Open app in a browser at http://localhost:8080

Database Information:
Database Name: term_project
Database Username: root
Database Password: mysqlpass

Where to Locate Code:
UI:
- src/main/resources/templates/game_page.mustache
- src/main/resources/templates/standings_page.mustache
- src/main/resources/templates/people_page.mustache
- src/main/resources/templates/profile_page.mustache
- src/main/resources/templates/feed_page.mustache
- src/main/resources/templates/fragments/

Controllers:
- src/main/java/uga/menik/csx370/controllers/GameController.java
- src/main/java/uga/menik/csx370/controllers/StandingsController.java
- src/main/java/uga/menik/csx370/controllers/PeopleController.java
- src/main/java/uga/menik/csx370/controllers/ProfileController.java
- src/main/java/uga/menik/csx370/controllers/FeedController.java

Services:
- src/main/java/uga/menik/csx370/services/UserService.java
- src/main/java/uga/menik/csx370/services/GameService.java

SQL/Schema:
- database_setup.sql (DDL)
- data.sql (seed data)

Test Accounts:
Username: [TEST USER 1]  Password: [PASSWORD]
Username: [TEST USER 2]  Password: [PASSWORD]
Username: [TEST USER 3]  Password: [PASSWORD]
