FROM opensuse:latest

WORKDIR /root

ADD jdk-8u131-linux-x64.rpm jdk.rpm

RUN	rpm -i jdk.rpm && \
    rm -rf jdk.rpm /usr/java/jdk1.8.0_131/src.zip /usr/java/jdk1.8.0_131/man /tmp/*

ADD "target/highloadcup-1.0-SNAPSHOT.jar" server.jar

ENV SERVER_PORT=80

CMD ["nice", "-n19", "ionice", "-n7", "java", "-server", "-Xmx3g", "-Xms3g", "-XX:-AggressiveOpts", "-jar", "server.jar"]