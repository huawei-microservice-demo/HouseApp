FROM 10.162.197.95:5000/cse-jre:8u111
RUN mkdir -p /home/apps/server
COPY tomcat/ /home/apps/server/tomcat
RUN ["chmod", "+x", "/home/apps/server/tomcat/bin/catalina.sh"]
CMD ["/home/apps/server/tomcat/bin/catalina.sh","run"]