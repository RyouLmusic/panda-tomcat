import java.util.Scanner;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/5/14 20:35
 */
public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        int N = scanner.nextInt();
        int L = scanner.nextInt();

        int sum = 0;

        int left = 0;
        int right = 1;
        int start = 0;
        int end = N;
        while(left < right && left <= N) {
            if(sum < N) {
                sum += right;
                right++;
            } else if(sum > N) {
                sum -= left;
                left++;
            } else {
                if(right-left >= L && right-left < end - start) {
                    start = left;
                    end = right;
                }
                sum -= left;
                left++;
            }
        }

        if(end-start >= N || end - start > 100) {System.out.print("No");}
        else {
            for(int i = start; i < end - 1; i++) {

                System.out.print(i + " ");
            }
            System.out.print(end-1);
        }
    }
}