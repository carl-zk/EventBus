import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Created by hero on 17-4-7.
 */
public class ConcurrentMapTest {
    Map<Integer, Integer> map = new ConcurrentHashMap<Integer, Integer>();
    CountDownLatch cdl = new CountDownLatch(1);

    @Test
    public void test() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            map.put(i, i);
        }
        Thread t1 = new Thread(new Runner1());
        Thread t2 = new Thread(new Runner1());
        t1.start();
        t2.start();
        cdl.countDown();
        t1.join();
        t2.join();
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
            it.remove(); // avoids a ConcurrentModificationException
        }
    }

    class Runner1 implements Runnable {
        @Override
        public void run() {
            try {
                cdl.await();
                for (int i = 0; i < 10; i++) {
                    int t = map.get(i);
                    map.put(i, t + 1);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
