FROM 100.125.0.198:20202/hwcse/dockerhub-java:8-jre-alpine
RUN mkdir -p /home/apps/server
COPY user-service-0.0.1-SNAPSHOT.jar /home/apps/server
COPY lib/ /home/apps/server/lib
ENTRYPOINT ["java", "-jar", "/home/apps/server/user-service-0.0.1-SNAPSHOT.jar"]