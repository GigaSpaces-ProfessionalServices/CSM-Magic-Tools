FROM openjdk:8
EXPOSE 8080
ADD target/web-dihx.jar web-dihx.jar
WORKDIR /dihx
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
#RUN ./mvnw dependency:resolve
COPY src ./src
CMD ["./mvnw", "spring-boot:run"]
