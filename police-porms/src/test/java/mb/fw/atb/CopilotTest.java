package mb.fw.atb;

import com.google.common.collect.Lists;
import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CopilotTest {

    public static void main(String[] args) {
        int a = 10;
        int b = 5;
        //더할꺼야
        System.out.println(add(a, b));
        //뺄꺼야
        System.out.println(subtract(a, b));
        //곱할꺼야
        System.out.println(multiply(a, b));
        //나눌꺼야

        List<String> fruits = Lists.newArrayList();
        fruits.add("apple");
        fruits.add("banana");
        fruits.add("cherry");
        fruits.add("date");


        //반복해서 출력해줘
        for (String fruit : fruits) {
            System.out.println(fruit);
        }

        //fruits a가 몇개 있는지 알려줘
        System.out.println(countFruits(fruits, "a"));
        

    }

    private static int countFruits(List<String> fruits, String a) {
        int count = 0;
        for (String fruit : fruits) {
            if (fruit.contains(a)) {
                count++;
            }
        }
        return count;
    }

    //덧셈 메소드
    public static int add(int a, int b) {
        return a + b;
    }

    //뺄셈 메소드
    public static int subtract(int a, int b) {
        return a - b;
    }

    //곱셈 메소드
    public static int multiply(int a, int b) {
        return a * b;
    }

    //나눗셈 메소드
    public static int divide(int a, int b) {
        return a / b;
    }
}
