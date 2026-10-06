import java.util.*;
import java.nio.charset.StandardCharsets;

class bloomFilter{

    static int filterSize = 10000000; //10 million
    static int size= (filterSize+63)>>>6;
    static long[] words= new long[size];

    public class HashGenerator {
        private static final long FNV_64_INIT = 0xcbf29ce484222325L;
        private static final long FNV_64_PRIME = 0x100000001b3L;

        public static class BaseHashes {
            public final int h1;
            public final int h2;

            public BaseHashes(int h1, int h2) {
                this.h1 = h1;
                this.h2 = h2;
            }
            public int getHash(int i) {
                return h1 + (i * h2);
            }
            public int getBoundedHash(int i, int maxBound) {
                long combined = (long) h1 + ((long) i * (long) h2);
                return (int) ((combined & 0x7fffffffffffL) % maxBound);
            }
        }

        public static BaseHashes generateBaseHashes(String input) {
            if (input == null) {
                throw new IllegalArgumentException("Input string cannot be null");
            }

            byte[] data = input.getBytes(StandardCharsets.UTF_8);
            long hash64 = FNV_64_INIT;

            for (byte b : data) {
                hash64 ^= (b & 0xff);
                hash64 *= FNV_64_PRIME;
            }

            int h1 = (int) (hash64 >>> 32); // Upper 32 bits
            int h2 = (int) hash64;          // Lower 32 bits

            return new BaseHashes(h1, h2);
        }
    }

    public static void main(String[] args) {
        for(int i=0;i<1_000_000;i++){
            String name="username" + i;
            add(name);
        }

        double ram=(words.length * 8)/(1024*1024);
        System.out.println(ram + "MB");

        int falsePositives = 0;
        int totalUninsertedQueries = 100000;

        for (int i = 0; i < totalUninsertedQueries; i++) {
            // keys that were not added 
            String unknownKey = "absent_key_" + i; 
            if (search(unknownKey))falsePositives++;
        }

        double rate = (falsePositives / (double) totalUninsertedQueries) * 100;
        System.out.println("False Positive Count: " + falsePositives);
        System.out.println("Empirical False Positive Rate: " + rate + "%");

        //System.out.println("Memory Allocated: " + String.format("%.2f", ram) + " MB");

        // Benchmark 1,000,000 inserts and queries
        //benchmarkOperations(1_000_000);
    }

    public static void setBit(int bit){
        int index=bit>>>6;
        int bitoffset=bit&63;
        words[index] |= (1L<<bitoffset);
    }

    public static boolean isSetBit(int bit){
        int index=bit>>>6;
        int bitoffset=bit&63;
        return (words[index]&(1L << bitoffset))!=0;
    }

    public static boolean add(String word){
        if(word==null) return false;
        HashGenerator.BaseHashes bases = HashGenerator.generateBaseHashes(word);
        
        //generating index and seting bit
        for (int i=0;i<7;i++) {
            int index=bases.getBoundedHash(i, filterSize);
            setBit(index);
        }

        return true;
    }

    public static boolean search(String word){
        HashGenerator.BaseHashes bases = HashGenerator.generateBaseHashes(word);

        for (int i=0;i<7;i++) {
            int index=bases.getBoundedHash(i, filterSize);
            if(!isSetBit(index)) return false;
        }

        return true;
    }

    public static void benchmarkOperations(int totalOperations) {
        System.out.println("=== Starting Performance Benchmark ===");

        // 1. JIT Warmup Phase
        for (int i = 0; i < 10_000; i++) {
            add("warmup_" + i);
            search("warmup_" + i);
        }

        // Clear bit array back to 0 after warmup
        Arrays.fill(words, 0L);

        // 2. Benchmark Insertions
        long startInsert = System.nanoTime();
        for (int i = 0; i < totalOperations; i++) {
            add("bench_user_" + i);
        }
        long endInsert = System.nanoTime();

        // 3. Benchmark Lookups
        long startLookup = System.nanoTime();
        int hits = 0;
        for (int i = 0; i < totalOperations; i++) {
            if (search("bench_user_" + i)) {
                hits++;
            }
        }
        long endLookup = System.nanoTime();

        // 4. Time & Throughput Calculations
        double insertTimeMs = (endInsert - startInsert) / 1_000_000.0;
        double lookupTimeMs = (endLookup - startLookup) / 1_000_000.0;

        double insertOpsPerSec = (totalOperations / insertTimeMs) * 1000.0;
        double lookupOpsPerSec = (totalOperations / lookupTimeMs) * 1000.0;

        // 5. Metrics Display
        System.out.printf("Operations Tested: %,d%n", totalOperations);
        System.out.printf("Total Insert Time: %.3f ms (%,.0f ops/sec)%n", insertTimeMs, insertOpsPerSec);
        System.out.printf("Total Lookup Time: %.3f ms (%,.0f ops/sec)%n", lookupTimeMs, lookupOpsPerSec);
        System.out.printf("Average Lookup Latency: %.2f nanoseconds per key%n", (endLookup - startLookup) / (double) totalOperations);
}
}