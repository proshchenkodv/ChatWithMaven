import java.util.Arrays;

public class classForTest {
//    public Main(){};
   /* public static void main(String[] args) {

        Arrays.stream(method1(arraygenerator(100))).forEach(System.out::println);

    }*/

    public static int[] method1(int[] inArray){
        int[] result=new int[inArray.length];
        int ind=getlast4index(inArray);
        if(ind<0) new RuntimeException("В массиве нет ни одной 4ки");
        else{
            int size=inArray.length-ind-1;
            if(size==0) new RuntimeException("Входящий массив заканчивается 4ой, невозможно создать массив");
            else{
                result= Arrays.copyOfRange(inArray,ind+1,inArray.length);
            }
        }
        return result;
    }
    public static int getlast4index(int[] testArray){
        int result=-1;
        for (int i = 0; i < testArray.length; i++) {
            if(testArray[i]==4) result=i;
        }
        return result;
    }
    public static int[] arraygenerator(int size){
        int[] result=new int[size];
        for (int i = 0; i < size; i++) {
            result[i]=(int)(Math.random()*10);
        }
        return result;
    }
}
