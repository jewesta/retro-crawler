# sh

# Build modules relevant for retro-crawler-app in the correct order,
# skipping tests. Must be executed from parent pom, NOT retro-crawler-app pom.
# To do this we target the parent pom via -f
mvn -f ../pom.xml -pl retro-crawler-cli -am -DskipTests clean install

# Start Vaadin app via Spring Boot. Must be executed from the app pom
mvn -f ../retro-crawler-cli exec:java