package com.atguigu.gulimall.search.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadTest {
    // 线程池对象
    public static ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        /*
        System.out.println("main - start");
        // 使用 自定义的线程池 执行 异步任务
        // 第一个参数 : 异步任务
        // 第二个参数 : 线程池对象
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
        }, executor);
        System.out.println("main - end");
        */

        /*
        System.out.println("main - start");
        // 使用 自定义的线程池 执行 有返回值的异步任务
        // 第一个参数 : 异步任务
        // 第二个参数 : 线程池对象
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            return 10 / 2;
        }, executor);
        // 接收 & 输出 异步任务的返回值
        Integer futureResult = future.get();
        System.out.println("main - end - " + futureResult);
        */

        /*
        System.out.println("main - start");
        // 使用 自定义的线程池 执行 有返回值的异步任务
        // 异步任务执行完成后，执行当前异步任务的线程将会继续执行 whenComplete() 任务
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            return 10 / 0;
        }, executor).whenComplete((result, exception) -> {
            // whenComplete() 可以得到异常信息，但它无法修改异步任务的返回值
            System.out.println("complete async task");
            System.out.println("result: " + result);
            System.out.println("exception: " + exception);
        }).exceptionally(throwable -> {
            // 修改异步任务的返回值
            return 10;
        });
        // 接收 & 输出 异步任务的返回值
        Integer futureResult = future.get();
        System.out.println("main - end - " + futureResult);
        */

        /*
        System.out.println("main - start");
        // 使用 自定义的线程池 执行 有返回值的异步任务
        // 异步任务执行完成后，将会继续执行 handle() 任务
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            return 10 / 0;
        }, executor).handle((result, exception) -> {
            // handle() 既可以得到异步任务的返回值和异常信息，也可以修改异步任务的返回值
            if (result != null) {
                return result * 2;
            }
            if (exception != null) {
                return 0;
            }
            return 0;
        });
        // 接收 & 输出 异步任务的返回值
        Integer futureResult = future.get();
        System.out.println("main - end - " + futureResult);
        */

        /*
        System.out.println("main - start");
        // 使用 自定义的线程池 执行 异步任务
        // 异步任务执行完成后，将会继续执行 thenRunAsync() 任务
        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            return 10 / 2;
        }, executor).thenRunAsync(() -> {
            // thenRun() 和 thenRunAsync() 不能获取上一个异步任务的返回值，且自身也无返回值
            System.out.println("the second task is running");
        }, executor);
        System.out.println("main - end");
        */

        /*
        System.out.println("main - start");
        // 使用 自定义的线程池 执行 异步任务
        // 异步任务执行完成后，将会继续执行 thenAcceptAsync() 任务
        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            return 10 / 2;
        }, executor).thenAcceptAsync(result -> {
            // thenAccept() 和 thenAcceptAsync() 能获取上一个异步任务的返回值，但自身无返回值
            System.out.println("the second task is running, last result = " + result);
        }, executor);
        System.out.println("main - end");
        */

        /*
        System.out.println("main - start");
        // 使用 自定义的线程池 执行 有返回值的异步任务
        // 异步任务执行完成后，将会继续执行 thenApplyAsync() 任务
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            return 10 / 2;
        }, executor).thenApplyAsync(result -> {
            // thenApply() 和 thenApplyAsync() 既能获取上一个异步任务的返回值，自身又有返回值
            System.out.println("the second task is running, last result = " + result);
            return result * 2;
        }, executor);
        // 接收 & 输出 异步任务的返回值
        // future.get() 是一个 阻塞方法
        Integer futureResult = future.get();
        System.out.println("main - end - " + futureResult);
        */

        /*
        System.out.println("main - start");
        // 任务1
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            return "result-1";
        }, executor);
        // 任务2
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            return "result-2";
        }, executor);
        // 任务1 和 任务2 都执行完之后，再执行 runAfterBothAsync()
        CompletableFuture<Void> future3 = future1.runAfterBothAsync(future2, () -> {
            // runAfterBoth() 和 runAfterBothAsync() 不能获取 任务1 和 任务2 的返回值，且自身也无返回值
            System.out.println("the third task is running");
        }, executor);
        System.out.println("main - end");
        */

        /*
        System.out.println("main - start");
        // 任务1
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            return "result-1";
        }, executor);
        // 任务2
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            return "result-2";
        }, executor);
        // 任务1 和 任务2 都执行完之后，再执行 thenAcceptBothAsync()
        CompletableFuture<Void> future3 = future1.thenAcceptBothAsync(future2, (res1, res2) -> {
            // thenAcceptBoth() 和 thenAcceptBothAsync() 能获取 任务1 和 任务2 的返回值，但自身无返回值
            System.out.println("the third task is running, the first result = " + res1 + ", the second result = " + res2);
        }, executor);
        System.out.println("main - end");
        */

        /*
        System.out.println("main - start");
        // 任务1
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            return "result-1";
        }, executor);
        // 任务2
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            return "result-2";
        }, executor);
        // 任务1 和 任务2 都执行完之后，再执行 thenCombineAsync()
        CompletableFuture<String> future3 = future1.thenCombineAsync(future2, (res1, res2) -> {
            // thenCombine() 和 thenCombineAsync() 既能获取 任务1 和 任务2 的返回值，自身又有返回值
            System.out.println("the third task is running, the first result = " + res1 + ", the second result = " + res2);
            return "result-3";
        }, executor);
        // 接收 & 输出 异步任务的返回值
        // future3.get() 是一个 阻塞方法
        String futureResult = future3.get();
        System.out.println("main - end - " + futureResult);
        */

        /*
        System.out.println("main - start");
        // 任务1
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            return "result-1";
        }, executor);
        // 任务2
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Thread: " + Thread.currentThread().getName());
            return "result-2";
        }, executor);
        // 两个任务，只要有一个完成，就会执行 runAfterEitherAsync()
        CompletableFuture<Void> future3 = future1.runAfterEitherAsync(future2, () -> {
            // runAfterEither() 和 runAfterEitherAsync() 不能获取 任务1 和 任务2 的返回值，且自身也无返回值
            System.out.println("the third task is running");
        }, executor);
        System.out.println("main - end");
        */

        /*
        System.out.println("main - start");
        // 任务1
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            return "result-1";
        }, executor);
        // 任务2
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Thread: " + Thread.currentThread().getName());
            return "result-2";
        }, executor);
        // 两个任务，只要有一个完成，就会执行 acceptEitherAsync()
        CompletableFuture<Void> future3 = future1.acceptEitherAsync(future2, result -> {
            // acceptEither() 和 acceptEitherAsync() 能获取 任务1 和 任务2 的返回值，但自身无返回值
            System.out.println("the third task is running, the first result = " + result);
        }, executor);
        System.out.println("main - end");
        */

        /*
        System.out.println("main - start");
        // 任务1
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            return "result-1";
        }, executor);
        // 任务2
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Thread: " + Thread.currentThread().getName());
            return "result-2";
        }, executor);
        // 两个任务，只要有一个完成，就会执行 applyToEitherAsync()
        CompletableFuture<String> future3 = future1.applyToEitherAsync(future2, result -> {
            // applyToEither() 和 applyToEitherAsync() 既能获取 任务1 和 任务2 的返回值，自身又有返回值
            System.out.println("the third task is running, the first result = " + result);
            return "result-3";
        }, executor);
        // 接收 & 输出 异步任务的返回值
        // future3.get() 是一个 阻塞方法
        String futureResult = future3.get();
        System.out.println("main - end - " + futureResult);
        */

        /*
        System.out.println("main - start");
        // 任务1
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的基本信息");
            return "result-1";
        }, executor);
        // 任务2
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的图片信息");
            return "result-2";
        }, executor);
        // 任务3
        CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("查询商品的属性信息");
            return "result-3";
        }, executor);
        // allOf() : 等待所有任务完成
        CompletableFuture<Void> allOf = CompletableFuture.allOf(future1, future2, future3);
        allOf.get();
        System.out.println("main - end - future1 = " + future1.get() + ", future2 = " + future2.get() + ", future3 = " + future3.get());
        */

        /*
        System.out.println("main - start");
        // 任务1
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的基本信息");
            return "result-1";
        }, executor);
        // 任务2
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的图片信息");
            return "result-2";
        }, executor);
        // 任务3
        CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("查询商品的属性信息");
            return "result-3";
        }, executor);
        // anyOf() : 只要有一个任务完成即可
        CompletableFuture<Object> anyOf = CompletableFuture.anyOf(future1, future2, future3);
        // 获取 第一个完成的任务的返回值
        String futureResult = (String) anyOf.get();
        System.out.println("main - end - futureResult = " + futureResult);
        */
    }
}
