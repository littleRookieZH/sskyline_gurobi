package main.java.classtest;
import gurobi.*;
/**
 * @author xzbz
 * @create 2024-05-28 21:20
 */


public class TestGurobi {
    public static void main(String[] args) throws GRBException {
        try {
            // 创建空的环境
            GRBEnv env = new GRBEnv(true);
            env.set("logFile", "mip1.log");
            env.start();

            // 创建空的模型
            GRBModel model = new GRBModel(env);

            // 创建变量
            GRBVar x = model.addVar(0.0,1.0,0.0, GRB.BINARY,"x");
            GRBVar y = model.addVar(0.0,1.0,0.0, GRB.BINARY,"y");
            GRBVar z = model.addVar(0.0,1.0,0.0, GRB.BINARY,"z");

            // 设置目标函数，maximize x + y + 2z
            GRBLinExpr expr = new GRBLinExpr();
            expr.addTerm(1.0,x);
            expr.addTerm(1.0,y);
            expr.addTerm(2.0,z);
            model.setObjective(expr,GRB.MAXIMIZE);

            // 添加约束条件一 x + 2y + 3z <= 4
            expr = new GRBLinExpr();
            expr.addTerm(1.0,x);
            expr.addTerm(2.0,y);
            expr.addTerm(3.0,z);
            model.addConstr(expr,GRB.LESS_EQUAL,4.0,"c0");

            // 添加约束条件二 x + y >= 1
            expr = new GRBLinExpr();
            expr.addTerm(1.0,x);
            expr.addTerm(1.0,y);
            model.addConstr(expr,GRB.GREATER_EQUAL,1.0,"c1");

            // 优化模型
            model.optimize();

            System.out.println(x.get(GRB.StringAttr.VarName) + " " + x.get(GRB.DoubleAttr.X));
            System.out.println(y.get(GRB.StringAttr.VarName) + " " + y.get(GRB.DoubleAttr.X));
            System.out.println(z.get(GRB.StringAttr.VarName) + " " + z.get(GRB.DoubleAttr.X));

            System.out.println("Obj:" + model.get(GRB.DoubleAttr.ObjVal));

            // 回收处理
            model.dispose();
            env.dispose();
        } catch (GRBException e){
            System.out.println("Error code:" + e.getErrorCode() + ". " + e.getMessage());
        }
    }
}
