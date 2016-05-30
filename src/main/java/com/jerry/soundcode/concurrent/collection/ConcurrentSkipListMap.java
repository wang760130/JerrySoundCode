package com.jerry.soundcode.concurrent.collection;

/**
 * ConcurrentSkipListMap提供了一种线程安全的并发访问的排序映射表。
 * 内部是SkipList（跳表）结构实现，在理论上能够在O(log(n))时间内完成查找、插入、删除操作。
 * 在非多线程的情况下，应当尽量使用TreeMap。
 * 此外对于并发性相对较低的并行程序可以使用Collections.synchronizedSortedMap将TreeMap进行包装，也可以提供较好的效率。
 * 对于高并发程序，应当使用ConcurrentSkipListMap，能够提供更高的并发度。同样，ConcurrentSkipListMap支持Map的键值进行排序
 */
public class ConcurrentSkipListMap {


}
