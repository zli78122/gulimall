# Gulimall: Distributed Architecture E-commerce Platform

*Video: https://youtu.be/QqitAgA-RXw*  

*Backend Source Code: https://github.com/zli78122/gulimall*  
*Frontend Source Code: https://github.com/zli78122/gulimall_renren-fast-vue*  

## Introduction
* Developed a distributed e-commerce system based on **Spring Boot** and **Spring Cloud**, used **Docker** to manage multiple middleware and **Nginx** for dynamic and static separation, reverse proxy, and load balancing.
* Introduced a complete set of **microservice** governance solution: **Nacos** as a registration and configuration center, **Gateway** as a gateway, **Feign** for remote call, **Ribbon** for load balancing, **Sentinel** for flow protection, **Sleuth** and **Zipkin** as a tracing system.
* Solved most of the problems faced by a highly concurrent distributed system: **Spring Cache** and **Redis** as distributed cache, **Elasticsearch** for faster product retrieval time, **Spring Session** for session data sharing, **thread pool** and **asynchronous task** for stability and performance.
* Implemented generating/cancelling order and locking/unlocking inventory using **RabbitMQ delayed queue** based on **BASE** theory and **Flexible Transaction** - **message reliability** and **eventual consistency** of transactions in a distributed system.
* Completed product flash sale using **Redisson distributed lock – Semaphore** and **MQ**, which can process **50,000 QPS** in one Tomcat server.

## Explanations
### 1. Relationships between SPU, SKU, Brand, Category, and Product Attribute
Understanding the following **four concepts** is crucial for you to understand the relationship between these entities.
* **Sales Attribute**: The different attribute values of the sales attributes of a product will lead to differences in the sales price of this product. e.g. iPhone 12 Pro Max 256GB and iPhone 12 Pro Max 512GB have a different sales price. So "capacity" is a sales attribute
* **Regular Attribute**: It will not affect the price of the product, like the length, weight, ppi of iPhone 12 Pro Max.
* **SPU (Standard Product Unit)**: It is a type of goods, such as a type of iPhone, like iPhone 12 Pro Max.
* **SKU (Stock Keeping Unit)**: It is a specific product unit based on the SPU, like iPhone 12 Pro Max Gold+256GB, iPhone 12 Pro Max Gold+512GB, iPhone 12 Pro Max Blue+256GB, iPhone 12 Pro Max Sliver+512GB. i.e. SKU is a combination of various Sales Attributes (color + capacity) of the SPU.
![1.png](https://zli78122-gulimall.oss-us-west-1.aliyuncs.com/chart/1.png "1.png")

### 2. Flow Chart of Generating/Canceling Order and Locking/Unlocking Inventory 
(Delayed Queue, BASE Theory, Distributed Flexible Transaction)
* **Locking Inventory**: After the order is generated and before the user pays, the product purchased by current user will be locked in the inventory, which means that during this period, the product only belongs to the current user, and any other users can not purchase this product.
* **Unlocking Inventory**: When the order is paid or cancelled, the system will unlock the inventory.
![2.png](https://zli78122-gulimall.oss-us-west-1.aliyuncs.com/chart/2.png "2.png")

### 3. Flow Chart of Product Flash Sale 
(Distributed Lock - Semaphore, MQ)
* **Flash Sale**: Online store offers substantial discounts or promotions for a very short period of time. The main goals of a flash sale strategy are to get online shoppers to impulse buy, to increase short-term sales, or to sell your surplus stock. So a flash sale means a very high amount of concurrency, which may reach millions of concurrency.
![3.png](https://zli78122-gulimall.oss-us-west-1.aliyuncs.com/chart/3.png "3.png")

## Show
![4.png](https://zli78122-gulimall.oss-us-west-1.aliyuncs.com/show/4-2.png "4.png")  
<br />
![5.png](https://zli78122-gulimall.oss-us-west-1.aliyuncs.com/show/5-2.png "5.png")  
<br />
![6.png](https://zli78122-gulimall.oss-us-west-1.aliyuncs.com/show/6.png "6.png")  
<br />
![7.png](https://zli78122-gulimall.oss-us-west-1.aliyuncs.com/show/7.png "7.png")  
<br />
![8.png](https://zli78122-gulimall.oss-us-west-1.aliyuncs.com/show/8.png "8.png")  
<br />
![9.png](https://zli78122-gulimall.oss-us-west-1.aliyuncs.com/show/9.png "9.png")  

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
