FROM 10.162.197.95:5000/cse-jre:8u111
RUN mkdir -p /home/apps/server
COPY account-service-0.0.1-SNAPSHOT.jar /home/apps/server
COPY lib/ /home/apps/server/lib
ENTRYPOINT ["java", "-Ddb.password=${DB_PASSWORD}","-Ddb.url=jdbc:mysql://${DB_HOST}/poc_account_db","-Dcse.service.registry.address=https://128.8.110.251:30100","-Dcse.config.client.serverUri=https://128.8.110.251:30103","-jar",  "/home/apps/server/account-service-0.0.1-SNAPSHOT.jar"]