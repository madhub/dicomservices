FROM openjdk:17
COPY ./build/libs/ /tmp
WORKDIR /tmp
ENTRYPOINT ["java","-jar","cstoreservice-1.0-SNAPSHOT.jar"]