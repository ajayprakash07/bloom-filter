import java.util.*;
import java.nio.charset.StandardCharsets;

class bloomFilter{

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
        String name ="Ajay Prakash Singh";

        HashGenerator.BaseHashes bases = HashGenerator.generateBaseHashes(name);
        System.out.println("h1: " + bases.h1);
        System.out.println("h2: " + bases.h2);

        System.out.println("\n--- 5 Generated Hashes ---");
        for (int i=0;i<7;i++){
            System.out.println("Hash " +i+ ": " +bases.getHash(i));
        }

        int filterSize = 10000000;
        System.out.println("\n--- 5 Bounded Array Indexes ---");
        for (int i = 0; i < 7; i++) {
            System.out.println("Index " + i + ": " + bases.getBoundedHash(i, filterSize));
        }
        
        }
}