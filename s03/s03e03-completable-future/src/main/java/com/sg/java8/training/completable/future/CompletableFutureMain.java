package com.sg.java8.training.completable.future;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A few {@link java.util.concurrent.CompletableFuture} usage samples
 */
public class CompletableFutureMain {

    private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

    private static final Executor EXECUTOR = Executors.newWorkStealingPool(AVAILABLE_PROCESSORS / 2);

    public static void main(String[] args) {
        simpleCompletableFutures();

        chainedCompletionStages();

        productsOperations();

        shutdownExecutor();
    }

    private static void simpleCompletableFutures() {
        final CompletableFuture<String> completableFuture =
                CompletableFuture.supplyAsync(() -> "a very simple text");

        final Consumer<String> stringConsumer = stringPrinter();
        completableFuture.thenAcceptAsync(stringConsumer);

        final CompletableFuture<String> anotherFuture =
                CompletableFuture.supplyAsync(() -> "another text");

        completableFuture.thenCompose(value -> anotherFuture);
        final CompletableFuture<String> completableFuture1 =
                anotherFuture.thenApplyAsync(value -> value);

        completableFuture.exceptionally(throwable -> "Thrown: " + throwable.getMessage());

        completableFuture.thenApplyAsync(String::toUpperCase, Executors.newCachedThreadPool());
        completableFuture.acceptEither(anotherFuture, stringConsumer);
    }

    private static void chainedCompletionStages() {
        CompletableFuture<String> first = CompletableFuture.supplyAsync(() -> {
            System.out.println(Thread.currentThread().getName());
            //Unchecked.consumer(it -> Thread.sleep(100));
            return "first";
        }, EXECUTOR);

        CompletableFuture<String> second = CompletableFuture.supplyAsync(() -> {
            System.out.println(Thread.currentThread().getName());
            //Unchecked.consumer(it -> Thread.sleep(150));
            return "second";
        }, EXECUTOR);

        CompletableFuture<Integer> third = CompletableFuture.supplyAsync(() -> {
            System.out.println(Thread.currentThread().getName());
            //Unchecked.consumer(it -> Thread.sleep(200));
            return 7;
        }, EXECUTOR);

        final CompletableFuture<Integer> future =
                first.thenComposeAsync(value -> second)
                     .thenComposeAsync(value -> third);

        System.out.println(future.join());

        final CompletableFuture<Void> finalFuture = CompletableFuture.allOf(first, second);
        finalFuture.thenAccept(value -> notifyFinishedTasks());
    }

    private static void productsOperations() {
        final ProductProcessor productProcessor = new ProductProcessor();

        final CompletableFuture<Long> getProductsStock = productProcessor.getProductsStock("iPad");
        final Function<Long, CompletableFuture<Double>> getProductsPrice = productProcessor.getProductsPrice();
        final Function<Double, CompletableFuture<String>> getProductsDisplayText = productProcessor.getDisplayedText();

        final String productsText = getProductsStock.thenComposeAsync(getProductsPrice, EXECUTOR)
                                                    .thenComposeAsync(getProductsDisplayText, EXECUTOR)
                                                    .join();
        System.out.println(productsText);
    }

    private static void notifyFinishedTasks() {
        System.out.println(Thread.currentThread().getName() + " - All good");
    }

    private static Consumer<String> stringPrinter() {
        return value -> System.out.println(Thread.currentThread().getName() + " - " + value);
    }

    private static void shutdownExecutor() {
        ((ExecutorService) EXECUTOR).shutdown();
        System.out.println("The executor was properly shutdown");
    }
}