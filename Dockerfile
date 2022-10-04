FROM openjdk:17
EXPOSE 6000:6000
RUN mkdir /usr/app
COPY target/*-jar-with-dependencies.jar /usr/app/app.jar
ENTRYPOINT ["java","-jar","/usr/app/app.jar"]

