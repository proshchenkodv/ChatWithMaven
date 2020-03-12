//import org.junit.Before;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class classHW6Test {
    private classHW6 ourClass;
    private int[] inArr;
    private int[] outArr;
    private boolean isit;
    RuntimeException error;

    @Before
    public void setUp() throws Exception {
        ourClass= new classHW6();
    }

    public classHW6Test(int[] inArr, int[] outArr, boolean isit) {
        this.inArr = inArr;
        this.outArr = outArr;
        this.isit=isit;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {new int[]{0,1,2,5,2,5,5,4,2,5,6}, new int[]{2,5,6},true},
                {new int[]{0,2,5,2,5,5}, new int[]{0},false},
                {new int[]{0,1,2,5,2,5,5,5,2,5,4}, new int[]{0},true},
                {new int[]{0,1,4,5,2,4,5,99,2,5,6}, new int[]{5,99,2,5,6},true},
                {new int[]{4,1,2,5,2,5,5,55,2,5,6}, new int[]{1,2,5,2,5,5,55,2,5,6},true}
        });
    }


    @Test(expected = RuntimeException.class)
    public void paramTestArrayException() {
        Assert.assertArrayEquals(ourClass.method1(inArr), outArr);
    }

    @Test
    public void paramTestArray() {
        Assert.assertArrayEquals(ourClass.method1(inArr), outArr);
    }
    @Test
    public void paramTestCheckArray() {
        Assert.assertEquals(ourClass.checkon1or4(inArr), isit);
    }
}