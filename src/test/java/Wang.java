import java.util.*;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/5/14 21:31
 */
public class Wang {
}


class Main1 {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        int n = scanner.nextInt();
        int k = scanner.nextInt();

        int[][] nums = new int[n][k];

        for(int i = 0; i < n; i++){
            for(int j = 0; j < k; j++){
                nums[i][j] = scanner.nextInt();
            }
        }

        int count = 0;
        for(int i = 0; i < n; i++) {
            for(int m = i+1; m < n; m++) {
                int sum = nums[i][0] + nums[m][0];
                boolean is = true;
                for (int j = 1; j < k; j++) {
                    if(nums[i][j]+nums[m][j] != sum) is = false;
                }
                if (is) count++;
            }

        }

        System.out.println(count);
    }

}


class Main2 {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        int n = scanner.nextInt();
        int k = scanner.nextInt();

        int[][] nums = new int[n][k];

        for(int i = 0; i < n; i++){
            for(int j = 0; j < k; j++){
                nums[i][j] = scanner.nextInt();
            }
        }

        int count = 0;
        for(int i = 0; i < n; i++) {
            for(int j = 0; j < k; j++) {
            }

        }

        System.out.println(count);
    }

}



// 注意类名必须为 Main, 不要有任何 package xxx 信息

/**
 * 30
 * 72
 * 13
 * -1
 */
// 7464419760584745207966505775430
/**
 * 2 3 5 6
 * 2 3 4 6 8 9
 * none
 */
class Main4 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        ArrayList<String> nums = new ArrayList<>();

        // 注意 hasNext 和 hasNextLine 的区别
        /*while (!scanner.hasNext("-1")) { // 注意 while 处理多个 case
            String str = scanner.next();
            nums.add(str);
        }*/


        while (scanner.hasNext()) {


            String str = scanner.nextLine();
            if (str.equals("-1")) {
                break;
            }
            nums.add(str);
        }

        for (String num : nums) {
            StringBuilder sb = new StringBuilder();
            for(int k = 2; k <= 9; k++) {

                int n = 0;
                for(int i = 0; i < num.length(); i++) {
                    String a = num.substring(i, i+1);
                    n = (n*10+Integer.parseInt(a)) % k;
                }

                if(n == 0){
                    sb.append(k);
                    sb.append(" ");
                }

            }
           /* if (num % 9 == 0) {
                is = true;
                System.out.print(9);
            }*/
            if(sb.length() == 0) System.out.println("none");
            else System.out.println(sb.toString().trim());

        }

    }
}

class Solution {
    public int evalRPN(String[] tokens) {

        LinkedList<Integer> stack = new LinkedList<>();

        for(int i = 0; i < tokens.length; i++) {

            if(isDigit(tokens[i])) {
                stack.push(Integer.parseInt(tokens[i]));
            }
            else {
                String str = tokens[i];
                int num1 = stack.pop();
                int num2 = stack.pop();
                int s = 0;
                if(str.equals("+")) {
                    s = num1 + num2;
                    stack.push(s);
                }
                else if(str.equals("-")) {
                    s = num2 - num1;
                    stack.push(s);
                }
                else if(str.equals("*")) {
                    s = num2 * num1;
                    stack.push(s);
                }
                else {
                    s = num2 / num1;
                    stack.push(s);
                }
            }
        }
        return stack.pop();
    }

    private boolean isDigit(String str) {

        try {
            Integer.parseInt(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) {

        int a = new Solution().evalRPN(new String[]{"10","6","9","3","+","-11","*","/","*","17","+","5","+"});
        System.out.println(a);
    }
}

class Solution2 {
    /**
     *
     * @param a long长整型 木棒的长度
     * @return int整型
     */
    public int stick (long a) {
        if(a < 2) return 0;
        // write code here
        int count = 1;
        long c = 1;
        long b = 1;
        long d = 0;
        while(d <= a) {
            long temp = c;
            c = b + c;
            b = temp;
            d = d + c;
            count++;
        }
//        if(a - d > )
        return count;
    }

    public static void main(String[] args) {
        int stick = new Solution2().stick(5);

        System.out.println(stick);
    }
}