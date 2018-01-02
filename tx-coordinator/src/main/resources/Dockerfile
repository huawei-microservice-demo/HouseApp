FROM 100.125.0.198:20202/hwcse/dockerhub-java:8-jre-alpine
RUN mkdir -p /home/apps/server
COPY ./tx-coordinator/target/tx-coordinator-0.0.1-SNAPSHOT.jar /home/apps/server
COPY ./tx-coordinator/target/lib/ /home/apps/server/lib
ENTRYPOINT ["java", "-jar", "/home/apps/server/tx-coordinator-0.0.1-SNAPSHOT.jar"]