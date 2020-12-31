# Gulimall: Distributed Architecture E-commerce Platform

## Introduction
* Developed a distributed e-commerce system based on **Spring Boot** and **Spring Cloud**, used **Docker** to manage multiple middleware and **Nginx** for dynamic and static separation, reverse proxy, and load balancing.
* Introduced a complete set of **microservice** governance solution: **Nacos** as a registration and configuration center, **Gateway** as a gateway, **Feign** for remote call, **Ribbon** for load balancing, **Sentinel** for flow protection, **Sleuth** and **Zipkin** as a tracing system.
* Solved most of the problems faced by a highly concurrent distributed system: **Spring Cache** and **Redis** as distributed cache, **Elasticsearch** for faster product retrieval time, **Spring Session** for session data sharing, **thread pool** and **asynchronous task** for stability and performance.
* Implemented generating/cancelling order and locking/unlocking stock using **RabbitMQ delayed queue** based on **BASE** theory and **Flexible Transaction** - **message reliability** and **eventual consistency** of transactions in a distributed system.
* Completed product flash sale using **Redisson distributed lock – Semaphore** and **MQ**, which can process **50,000 QPS** in one Tomcat server.

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

## Implementation & Functionalities
### 1. Vagrant
    vagrant init centos/7  
    vagrant up  
    vagrant ssh  
    exit;  
    vagrant reload  
### 2. Docker
    MySQL
    Redis
    Elasticsearch
    Kibana
    Nginx
    RabbitMQ
### 3. Spring Cloud
    Spring Cloud Alibaba - Nacos         registration center
    Spring Cloud Alibaba - Nacos         configuration center
    Spring Cloud         - Ribbon        load balancing
    Spring Cloud         - Feign         remote call
    Spring Cloud Alibaba - Sentinel      flow protection
    Spring Cloud         - Gateway       gateway
    Spring Cloud         - Sleuth        tracing system
    Spring Cloud Alibaba - Seata         distributed transaction
### 4. Cache & Distributed Lock
    Spring Cache + Redis, solve these three problems
        (1). Cache Penetration
        (2). Cache Crash
        (3). Cache Breakdown
    Local Lock: synchronized(this);
    Distributed Lock - Redisson
        (1). Reentrant Lock
        (2). ReadWrite Lock
        (3). Semaphore
        (4). CountDownLatch
    Spring Cache:
        @Cacheable   : triggers cache population
        @CacheEvict  : triggers cache eviction
        @CachePut    : updates the cache without interfering with the method execution
        @Caching     : regroups multiple cache operations to be applied on a method
        @CacheConfig : shares some common cache-related settings at class-level
### 5. Thread Pool & Asynchronous Task
    Callable + FutureTask & Thread Pool
    CompletableFuture ≈ Promise in Frontend
    Customized Thread Pool
### 6. Shopping Cart
    Data Structure: Map<String key1, Map<String key2, CartItem>>
        key1: Shopping Cart Identifier
            For a Online Shopping Cart， key1 = "gulimall:cart:[userId]"   e.g. key1 = "gulimall:cart:1"
            For a Offline Shopping Cart，key1 = "gulimall:cart:[UUID]"     e.g. key1 = "gulimall:cart:6cd5b6f7-4349-48b9-bb7d-feccadbecae1"
            Note: Online Shopping Cart means login user, Offline Shopping Cart means unidentified user
        key2 : Product Identifier
### 7. Order & Distributed Transaction
    Guarantee idempotence of interface using token.
    Lua Script
    Distributed Transaction (CAP & BASE)
        Solution 1. Spring Cloud Alibaba Seata
        Solution 2. RabbitMQ Delay Queue
    Online Payment: Alipay
### 8. Product Flash Sale
    Service Single Responsibility: Dedicated and Independent Microservices
    Random Code for Product Flash Sale - Token
    Distributed Lock - Semaphore
    RabbitMQ
