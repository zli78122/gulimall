# Gulimall: Distributed Architecture E-commerce Platform

## Introduction
* Developed a full-stack e-commerce system based on Docker environment, which solved most of the problems faced by a highly concurrent distributed system, and used Nginx for dynamic and static separation, reverse proxy, and load balancing.
* Introduced a complete set of microservice governance solution: Nacos as a registration and configuration center, Gateway as a gateway, Feign for remote call, Ribbon for load balancing, Sentinel for flow protection, Sleuth and Zipkin as a tracing system, Seata for distributed transaction.
* Used Elasticsearch to retrieve product data, which can significantly reduce retrieval time compared to querying in databases.
* Created thread pool and used CompletableFuture asynchronous task to reduce system resource consumption and improve response speed.
* Implemented generate/cancel order and lock/unlock stock via RabbitMQ delay queue to achieve reliable messages and ultimate consistency of transactions in a distributed system.
* Completed product flash sale using Redisson distributed lock - Semaphore, which can process 50,000 QPS in one Tomcat server.

## Tech Stacks
IntelliJ IDEA, Visual Studio Code, Kibana

MySQL, Redis, Google Cloud Storage

Spring, Spring MVC, MyBatis/MyBatis-Plus, Spring Boot, Spring Cache, Spring Session  
Spring Cloud : Nacos, Ribbon, Feign, Gateway, Seata, Sentinel, Sleuth, Zipkin  
Thymeleaf, Elasticsearch, RabbitMQ  
Redisson, CompletableFuture, Alibaba Cloud SMS, OAuth 2.0, Alipay, Cron

HTML, CSS, JavaScript, jQuery, XML, JSON  
ES6, Axios, element-ui, Node.js, Webpack, Vue.js  

Linux, Vagrant, Docker, Nginx  

Maven, Git/GitHub, Apache JMeter  
