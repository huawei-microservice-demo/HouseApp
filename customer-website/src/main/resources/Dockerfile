FROM 100.125.0.198:20202/hwcse/dockerhub-java:8-jre-alpine
RUN mkdir -p /home/apps/server
COPY ./customer-website/target/customer-website-0.0.1-SNAPSHOT.jar /home/apps/server
ENTRYPOINT ["java", "-Dcse.tcc.transaction.redis.host=${TCC_REDIS_HOST}", "-Dcse.tcc.transaction.redis.port=${TCC_REDIS_PORT}", "-Dcse.tcc.transaction.redis.password=${TCC_REDIS_PASSWD}", "-jar", "/home/apps/server/customer-website-0.0.1-SNAPSHOT.jar"]
