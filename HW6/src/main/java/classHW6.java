import java.util.Arrays;

public class classHW6 {
//    public Main(){};
    public static void main(String[] args) {

        Arrays.stream(method1(new int[]{0,1,2,5,2,5,5})).forEach(System.out::println);

    }

    public static int[] method1(int[] inArray){
        int[] result=new int[0];
        int ind=getlast4index(inArray);
        if(ind<0) throw new RuntimeException("В массиве нет ни одной 4ки");
        else{
            int size=inArray.length-ind-1;
            if(size==0) throw new RuntimeException("Входящий массив заканчивается 4ой, невозможно создать массив");
            else{
                result=new int[size];
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
    public boolean checkon1or4(int[] inArr){
        for (int i = 0; i < inArr.length; i++) {
            if(inArr[i]==1 || inArr[i]==4) return true;
        }
        return false;
    }
}
