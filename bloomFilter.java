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
        for(int i=0;i<100000;i++){
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
}