FROM adoptopenjdk/openjdk11:jdk-11.0.3_7-alpine as BUILD

# Setup maven, we don't use https://hub.docker.com/_/maven/ as it declare .m2 as volume, we loose all mvn cache
# We can alternatively do as proposed by https://github.com/carlossg/docker-maven#packaging-a-local-repository-with-the-image
# this was meant to make the image smaller, but we use multi-stage build so we don't care
RUN apk add --no-cache curl tar bash unzip git

ARG MAVEN_VERSION=3.6.3
ARG USER_HOME_DIR="/root"
ARG SHA=c35a1803a6e70a126e80b2b3ae33eed961f83ed74d18fcd16909b2d44d7dada3203f1ffe726c17ef8dcca2dcaa9fca676987befeadc9b9f759967a8cb77181c0
ARG BASE_URL=https://apache.osuosl.org/maven/maven-3/${MAVEN_VERSION}/binaries

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL -o /tmp/apache-maven.tar.gz ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
  && echo "${SHA}  /tmp/apache-maven.tar.gz" | sha512sum -c - \
  && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
  && rm -f /tmp/apache-maven.tar.gz \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"

RUN git clone https://github.com/ACINQ/eclair.git
RUN cd eclair && git checkout v0.8.0 && mvn install -DskipTests=true && cd ..

COPY . .

RUN mvn install -DskipTests=true

FROM acinq/eclair:release-0.8.0
RUN mkdir /plugins

ARG PLUGIN_VERSION=0.1.0

COPY --from=BUILD /root/.m2/repository/com/getalby/eclair/eclair-keysend/${PLUGIN_VERSION}/eclair-keysend-${PLUGIN_VERSION}.jar /plugins/eclair-keysend.jar
COPY --from=BUILD /root/.m2/repository/com/getalby/eclair/eclair-rabbitmq/${PLUGIN_VERSION}/eclair-rabbitmq-${PLUGIN_VERSION}.jar /plugins/eclair-rabbitmq.jar

ENTRYPOINT JAVA_OPTS="${JAVA_OPTS}" eclair-node/bin/eclair-node.sh \
    -Declair.printToConsole \
    -Declair.datadir=${ECLAIR_DATADIR}  \
    /plugins/eclair-keysend.jar /plugins/eclair-rabbitmq.jar
