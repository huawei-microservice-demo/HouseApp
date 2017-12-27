FROM 100.125.0.198:20202/hwcse/dockerhub-java:8-jre-alpine
RUN mkdir -p /home/apps/server
COPY account-service-0.0.1-SNAPSHOT.jar /home/apps/server
COPY lib/ /home/apps/server/lib
ENTRYPOINT ["java", "-jar", "/home/apps/server/account-service-0.0.1-SNAPSHOT.jar"]
