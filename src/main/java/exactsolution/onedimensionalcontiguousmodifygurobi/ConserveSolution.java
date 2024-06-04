package main.java.exactsolution.onedimensionalcontiguousmodifygurobi;

import java.io.Serializable;

/**
 * @author xzbz 保存模型的不可行解 -- 这些不可行解产生的约束将被用于 提升高度后模型的构建
 * 如果条带有旋转，则x-check需要重新校验。如果没有旋转，则不需要重新校验
 * @create 2024-06-12 13:27
 */
public class ConserveSolution  implements Serializable {
   public int[][] liftCut;
   public int[][] divideSolution;

    public ConserveSolution(int[][] divideSolution, int[][] liftCut) {
        this.liftCut = liftCut;
        this.divideSolution = divideSolution;
    }
}
