FROM ubuntu:18.04
MAINTAINER Devavrat Kalam, dk2792@rit.edu

# install all dependencies
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y  software-properties-common && \
    apt-get update && \
    apt-get install -y openjdk-11-jdk && \
    apt-get install -y net-tools iputils-ping maven gradle nmap wget git vim build-essential && \
    apt-get clean

RUN mkdir /app
COPY LICENSE /app
COPY pubsub /app/pubsub
WORKDIR app
RUN cd pubsub && mvn clean install