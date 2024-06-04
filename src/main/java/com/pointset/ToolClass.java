package main.java.com.pointset;

import gurobi.GRB;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hao
 * @description: 工具类
 * @date 2023/3/7 21:12
 */
public class ToolClass {
    /**
     * @param pointArray 任意 List集合
     * @return int[] 任意 一维数组
     * @description 将List集合转化为一维数组，并排序
     * @author hao
     * @date 2023/3/7 20:57
     */
    public static int[] listToArray(List<Integer> pointArray) {
        //拷贝数组
        int[] tempArray = listCopyToArray(pointArray);
        //数组排序
        sortedArray(tempArray);
        return tempArray;
    }

    /**
     * @description 对一维数组排序
     * @author hao
     * @date 2023/3/8 11:01
     */
    public static void sortedArray(int[] tempArray) {
        for (int i = 0; i < tempArray.length; i++) {
            int minIndex = i;
            for (int k = i; k < tempArray.length; k++) {
                if (tempArray[k] <= tempArray[minIndex]) {
                    minIndex = k;
                }
            }
            //交换i和minIndex的值
            int temp = tempArray[minIndex];
            tempArray[minIndex] = tempArray[i];
            tempArray[i] = temp;
        }
    }

    /**
     * @param arrayList
     * @return int[]
     * @description array转一维数组
     * @author hao
     * @date 2023/3/8 11:01
     */
    public static int[] listCopyToArray(List<Integer> arrayList) {
        if (arrayList == null) {
            return null;
        }
        int[] tempArray = new int[arrayList.size()];
        int j = 0;
        for (int i : arrayList) {
            tempArray[j] = i;
            j++;
        }
        return tempArray;
    }

    /**
     * @param array
     * @return List<Integer>
     * @description 一维数组转array
     * @author hao
     * @date 2023/3/8 11:32
     */
    public static List<Integer> arrayCopyToList(int[] array) {
        if (array == null) {
            return null;
        }
        List<Integer> arrayList = new ArrayList<>();
        for (int i : array) {
            arrayList.add(i);
        }
        return arrayList;
    }

    /**
     * @param array
     * @param num
     * @return boolean
     * @description 检查数组中是否包含一个数
     * @author hao
     * @date 2023/3/9 8:44
     */
    public static boolean contains(int[] array, int num) {
        for (int i : array) {
            if (i == num) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param src
     * @param dest
     * @description 复制一份Array
     * @author hao
     * @date 2023/3/27 22:39
     */
    public static void copyList(List<Integer> src, List<Integer> dest) {
        if (src == null || dest == null) {
            return;
        }
        for (int i : src) {
            dest.add(i);
        }
    }

    /**
     * @param array
     * @param minValue
     * @param maxValue
     * @return int[]
     * @description 根据最大最小值，获取符合条件的点集
     * @author hao
     * @date 2023/3/29 21:02
     */
    public static int[] getCoorPoints(int[] array, int minValue, int maxValue) {
        int i = 0;
        List<Integer> list = new ArrayList<>();
        for (; i < array.length; i++) {
            if (array[i] < minValue) {
                continue;
            }
            if (array[i] <= maxValue) {
                list.add(array[i]);
            }
        }
        return listCopyToArray(list);
    }
    /**
     * @param arr
     * @return int
     * @description 求和
     * @author hao
     * @date 2023/3/26 15:37
     */
    public static int getTotal(int[] arr) {
        int total = 0;
        for (int i : arr) {
            total += i;
        }
        return total;
    }
    // 对二维数组中的指定位置排序
    public static void sortedTwoDim(int[][] defPoints ,int index){
        for (int i = 0; i < defPoints.length; i++) {
            int minIndex = i;
            for (int j = i + 1 ; j < defPoints.length; j++) {
                if(defPoints[minIndex][index] > defPoints[j][index]){
                    minIndex = j;
                }
            }
            if(minIndex != i){
                int[] temp = defPoints[minIndex];
                defPoints[minIndex] = defPoints[i];
                defPoints[i] = temp;
            }
        }
    }

    public static int[][] copyTwoDim(int[][] array) {
        int[][] tempArray = new int[array.length][array[0].length];
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                tempArray[i][j] = array[i][j];
            }
        }
        return tempArray;
    }

    // 合并int[][] 和 list<int[]>
    public static int[][] copyTwoDim(int[][] array, List<int[]> listArray) {
        int length1 = array.length + listArray.size();
        int[][] tempArray = new int[length1][array[0].length];
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                tempArray[i][j] = array[i][j];
            }
        }
        for (int i = 0; i < listArray.size(); i++) {
            for (int j = 0; j < listArray.get(i).length; j++) {
                tempArray[i + array.length][j] = listArray.get(i)[j];
            }
        }
        return tempArray;
    }

    public static String getStatusString(int statusCode) {
        switch (statusCode) {
            case GRB.Status.LOADED:
                return "Loaded";
            case GRB.Status.OPTIMAL:
                return "Optimal";
            case GRB.Status.INFEASIBLE:
                return "Infeasible";
            case GRB.Status.INF_OR_UNBD:
                return "Infeasible or Unbounded";
            case GRB.Status.UNBOUNDED:
                return "Unbounded";
            case GRB.Status.CUTOFF:
                return "Cutoff";
            case GRB.Status.ITERATION_LIMIT:
                return "Iteration Limit";
            case GRB.Status.NODE_LIMIT:
                return "Node Limit";
            case GRB.Status.TIME_LIMIT:
                return "Time Limit";
            case GRB.Status.SOLUTION_LIMIT:
                return "Solution Limit";
            case GRB.Status.INTERRUPTED:
                return "Interrupted";
            case GRB.Status.NUMERIC:
                return "Numeric";
            case GRB.Status.SUBOPTIMAL:
                return "Suboptimal";
            case GRB.Status.INPROGRESS:
                return "In Progress";
            default:
                return "Unknown status";
        }
    }

}
