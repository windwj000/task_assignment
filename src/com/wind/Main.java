package com.wind;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
public class Main {
    static final int K=14;
    static final int M=6;
    static final int UNAVAILABLESTATE=100;
    static final double D=4.5;
    static final double inputRSU=(double)2/(double)3;

    static class Task{
        int index;    // 序号
        double computation;    // 计算负载
        double transmission;    // 传输负载

        public Task(int index, double computation, double transmission) {
            this.index = index;
            this.computation = computation;
            this.transmission = transmission;
        }

        public int getIndex() {
            return index;
        }

        public double getComputation() {
            return computation;
        }

        public double getTransmission() {
            return transmission;
        }
    };
    static class Vehicle{
        int index;    // 序号
        double capability;    // 处理能力
        double unitPrice;    // 服务成本

        public Vehicle(int index, double capability, double unitPrice) {
            this.index = index;
            this.capability = capability;
            this.unitPrice = unitPrice;
        }

        public int getIndex() {
            return index;
        }

        public double getCapability() {
            return capability;
        }

        public double getUnitPrice() {
            return unitPrice;
        }
    };

    // 拓扑排序得到多个 level
    static ArrayList<ArrayList<Integer>> topologicalSort(Task[] tasks,double[][] dependency){
        ArrayList<ArrayList<Integer>> res=new ArrayList<>();

        int[] indegree=new int[K];
        for(int i=0;i<K;i++){
            for(int j=0;j<K;j++){
                if(dependency[i][j]!=0){
                    indegree[i]++;
                }
            }
        }

        Queue<Integer> q=new LinkedList<>();
        for(int i=0;i<K;i++){
            if(indegree[i]==0){
                q.add(i);
            }
        }

        ArrayList<Integer> level=new ArrayList<>();
        while(!q.isEmpty()){
            int v=q.remove();
            indegree[v]--;
            level.add(v);
            for(int i=0;i<K;i++){
                if(dependency[i][v]!=0){
                    indegree[i]--;
                }
            }
            if(q.isEmpty()){
                res.add(level);
                level=new ArrayList<>();
                for(int i=0;i<K;i++){
                    if(indegree[i]==0){
                        q.add(i);
                    }
                }
            }
        }

        return res;
    }

    static double getTransmissionRateFromHop(int hop){
        double[] bandwidth=new double[]{0,10,4.8,3.2,2.4,1.92};
        return bandwidth[hop];
    }

    // 获取两车间的传输速率
    static double getTransmissionRate(int v1,int v2){
        int[][] vehiclalNetworkTopology=
                {{0,1,2,2,3,2},
                {1,0,3,2,2,1},
                {2,3,0,2,3,2},
                {2,2,2,0,1,3},
                {3,2,3,1,0,2},
                {2,1,2,3,3,0}};
        int hop=vehiclalNetworkTopology[v1][v2];
        return getTransmissionRateFromHop(hop);
    }

    /**
     * 生成仅有对角线是0，其他数值在[min,max)的对角矩阵，左闭右开
     * @param dim 矩阵维度
     * @param max 最大值
     * @param min 最小值
     * @return 矩阵
     */
    static int[][] GenRandomSquareMatrix(int dim,int min,int max){

        int[][] matrix = new int[dim][dim];
        for(int i =0;i<dim;i++){
            for(int j =0;j<dim;j++){
                if(i==j){
                    matrix[i][j] = 0;
                }else if(i<j){
                    matrix[i][j] = GenRandomInt(min,max-min);
                }else{
                    //i<<j
                    matrix[i][j] = matrix[j][i] ;
                }
            }
        }
        return  matrix;
    }

    /**
     *  生成N*2的汽车时间
     * @param car_number 汽车数量
     * @param start_time_minimun 开始时间最小值
     * @param start_time_maximun 开始时间最大值
     * @param finish_time_minimun 结束时间最小值
     * @param finish_time_maximun 结束时间最大值
     * @return 矩阵
     */
    static int[][] GenRandomCarInterval(int car_number,int start_time_minimun,int start_time_maximun,int finish_time_minimun,int finish_time_maximun){
        int[][] matrix = new int[car_number][2];
        for(int i =0;i<car_number;i++){
            matrix[i][0] = GenRandomInt(start_time_minimun,start_time_maximun-start_time_minimun);
            matrix[i][1] = GenRandomInt(finish_time_minimun,finish_time_maximun-finish_time_minimun);
        }
        return  matrix;
    }


    static double jiege(){
        //huoqu jiange
//        int[M][2]
        return 0f;
    }
    static String Matix2Str(int[][] matrix){
        int x_dim = matrix.length;
        int y_dim = matrix[0].length;
        StringBuilder builder = new StringBuilder();
        for(int i =0;i<x_dim;i++){
            builder.append("|");
            for(int j =0;j<y_dim;j++){
                builder.append(matrix[i][j]);
                builder.append("|");
            }
            builder.append("\n");
        }
        return builder.toString();
    }
    static int GenRandomInt(int base,int range){
        Random rand = new Random();
        int a = rand.nextInt(range);
        return base+a;
    }

    // 判断车辆节点是否可用
    // 1. 车辆在 VC 内
    // 2. 该车不能有其他任务正在处理
    static boolean isVehicleAvailable(int taskIdx,int j,double[][] minPrevFinishTime,double[][] actualStartTimes,double[][] finishTimes,double[][] interval,int[] scheduleRes,ArrayList<Integer> finishDistributeTasks){
        // 开始传输-处理完成 这段时间内，车辆在 VC 内
        if(minPrevFinishTime[taskIdx][j]<interval[j][0]||finishTimes[taskIdx][j]>interval[j][1]){
            return false;
        }
        for(int i=0;i<K;i++){
            // 任务处理开始和完成时间内，其他任务不能正在处理
            if(scheduleRes[i]==j&&i!=taskIdx){
                for(int k=0;k<finishDistributeTasks.size();k++){
                    if(i==finishDistributeTasks.get(k)){
                        if(!(finishTimes[i][j]<=actualStartTimes[taskIdx][j]||actualStartTimes[i][j]>=finishTimes[taskIdx][j])){
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    // 计算 max & min 传输速率
    static double[] findMaxAndMinTransmissionRate(){
        double[] res=new double[2];
        double max=0;
        double min=100;
        for(int i=0;i<M;i++){
            for(int j=0;j<M&&j!=i;j++) {
                if(getTransmissionRate(i,j)>max){
                    max=getTransmissionRate(i,j);
                }
                if(getTransmissionRate(i,j)<min){
                    min=getTransmissionRate(i,j);
                }
            }
        }
        res[0]=max;
        res[1]=min;
        return res;
    }

    // 划分 sub-deadline
    /*static double[][] divideSubDeadline(vector<vector<Task>> taskLevel,double inputTransmissionTimeRSU,double ave,double deadline){
        // 根据公式 K，求每段 level 的起始时刻
        // 传输负载
        double transmissionSum=0;
        vector<double> maxTransmissionLevel;
        // 不需要计算 task1 的传输时间
        cout<<"transmission: "<<endl;
        int levelNum=taskLevel.size();
        for(int i=0;i<levelNum-1;i++){
            vector<Task> tasks=taskLevel[i];
            int maxTransmission=0;
            for(int j=0;j<tasks.size();j++){
                if(tasks[j].transmission>maxTransmission){
                    maxTransmission=tasks[j].transmission;
                }
            }
            cout<<maxTransmission<<endl;
            maxTransmissionLevel.push_back(maxTransmission);
            transmissionSum+=maxTransmission;
        }
        cout<<transmissionSum<<endl;
        cout<<endl;

        // 按不同 Vc 计算
        cout<<"transmissionTimeRSU: "<<endl;
        cout<<inputTransmissionTimeRSU<<endl;
    *//*cout<<"transmissionTime: "<<endl;
//    double initialTransmissionTime=transmissionSum/ave;
    cout<<initialTransmissionTime<<endl;*//*
        cout<<"computationTime: "<<endl;
        double initialComputationTime=2;
        double initialTransmissionTime=deadline-inputTransmissionTimeRSU-initialComputationTime;
        cout<<initialComputationTime<<endl;
        cout<<"transmissionTime: "<<endl;
        cout<<initialTransmissionTime<<endl;
        cout<<endl;

        // 计算负载
        cout<<"computation: "<<endl;
        double computationSum=0;
        vector<double> maxComputationLevel;
        for(int i=0;i<levelNum;i++){
            vector<Task> tasks=taskLevel[i];
            int maxComputation=0;
            for(int j=0;j<tasks.size();j++){
                if(tasks[j].computation>maxComputation){
                    maxComputation=tasks[j].computation;
                }
            }
            cout<<maxComputation<<endl;
            maxComputationLevel.push_back(maxComputation);
            computationSum+=maxComputation;
        }
        cout<<computationSum<<endl;
        cout<<endl;

        // 进行时间的初划分
        double initialDivision[levelNum][2];
        double tmpTime=inputTransmissionTimeRSU;
        int idx=0;
        while(idx<levelNum){
            initialDivision[idx][0]=tmpTime;
            // 执行时间
            tmpTime+=maxComputationLevel[idx]/computationSum*initialComputationTime;
            // 0.966667

            // 注意不考虑 task1 的传输时间
            if(idx!=0){
                tmpTime+=maxTransmissionLevel[idx-1]/ave;
            }
            initialDivision[idx][1]=tmpTime;
            idx++;
        }
        initialDivision[levelNum-1][1]=deadline;

        vector<vector<double>> res;
        vector<double> tmp;
        for(int i=0;i<levelNum;i++){
            for(int j=0;j<2;j++){
                tmp.push_back(initialDivision[i][j]);
            }
            res.push_back(tmp);
            tmp.clear();
        }
        return res;
    }*/

// 计算优化分配后的总 time
/*double calculateTotalTime(vector<vector<Task>> taskLevel,int scheduleRes[K],double beginAndEndTimes[K][2]){
    for(int i=0;i<taskLevel.size();i++){
        vector<Task> tasksInLevel=taskLevel[i];
        // level 内的 tasks 存在排队的情况
    }
}*/

    // 计算优化分配后的总 cost
    /*static double calculateTotalCost(int scheduleRes[K],double computationTimes[K][M],Vehicle vehicles[M]){
        double totalCost=0;
        for(int i=0;i<K;i++){
            totalCost+=computationTimes[i][scheduleRes[i]]*vehicles[scheduleRes[i]].unitPrice;
        }
        return totalCost;
    }*/

    public static void main(String[] args) {
        // 第一部分：输入任务与车辆数据
        // 任务
        Task t1 = new Task(1, 3, 3);
        Task t2 = new Task(2, 2, 2);
        Task t3 = new Task(3, 3, 3);
        Task t4 = new Task(4, 4, 4);
        Task t5 = new Task(5, 3, 3);
        Task t6 = new Task(6, 1, 1);
        Task t7 = new Task(7, 4, 4);
        Task t8 = new Task(8, 2, 2);
        Task t9 = new Task(9, 5, 5);
        Task t10 = new Task(10, 3, 3);
        Task t11 = new Task(11, 3, 3);
        Task t12 = new Task(12, 4, 4);
        Task t13 = new Task(13, 3, 3);
        Task t14 = new Task(14, 4, 4);

        Task[] tasks = new Task[]{t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14};

        // 车辆
        // 计价：cost=capability￿￿^2*1.2+0.8
        Vehicle v1 = new Vehicle(1, 5, 30.8);
        Vehicle v2 = new Vehicle(2, 6, 44);
        Vehicle v3 = new Vehicle(3, 7, 59.6);
        Vehicle v4 = new Vehicle(4, 8, 77.6);
        Vehicle v5 = new Vehicle(5, 9, 98);
        Vehicle v6 = new Vehicle(6, 10, 120.8);

        Vehicle[] vehicles = new Vehicle[]{v1, v2, v3, v4, v5, v6};

        // 循环添加随机初始数据
        /*Vehicle vehicles[M];
        for(int i=0;i<M;i++){
            double capability=(rand()%21)*0.1+1;    // 1-3
            double cost=(rand()%21)*0.1+1;    // 1-3
            double positionX=(rand()%21)*0.1+1;    // 1-3
            double positionY=(rand()%21)*0.1+1;    // 1-3
            double speedX=(rand()%21)*0.1+1;    // 1-3
            double speedY=(rand()%21)*0.1+1;    // 1-3
            Vehicle v(i+1,capability,cost,positionX,positionY,speedX,speedY,0);
            vehicles[i]=v;
        }*/

        // 邻接矩阵，保存任务依赖关系和传输数据
        double[][] dependency =
                {{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  //1
                {3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  //2
                {3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  //3
                {3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  //4
                {3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  //5
                {0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  //6
                {0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  //7
                {0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  //8
                {0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  //9
                {0, 0, 0, 4, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0},  //10
                {0, 2, 0, 0, 0, 1, 4, 2, 0, 0, 0, 0, 0, 0},  //11
                {0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0},  //12
                {0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0},  //13
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 3, 0, 3, 0}}; //14
        //1 2 3 4 5 6 7 8 9 1011121314

        // 车辆停留在 VC 的时间
        double[][] interval =
                {{0, 20},
                {0, 20},
                {0, 20},
                {0, 20},
                {0, 20},
                {0, 20}};

        /*{{ 1.0,13.0},
         { 6.0,16.0},
         { 0.0,10.0},
         {11.0,20.0}};*/


        // 第二部分：算法
        // 1.拓扑排序对 DAG 划分多个 level
        ArrayList<ArrayList<Integer>> levelRes = topologicalSort(tasks, dependency);
        ArrayList<ArrayList<Task>> tasksInLevels = new ArrayList<>();
        for (int i = 0; i < levelRes.size(); i++) {
            ArrayList<Task> tasksInOneLevel=new ArrayList<>();
            for (int j = 0; j < levelRes.get(i).size(); j++) {
                tasksInOneLevel.add(tasks[levelRes.get(i).get(j)]);
            }
            tasksInLevels.add(tasksInOneLevel);
        }

        for (int i = 0; i < tasksInLevels.size(); i++) {
            for (int j = 0; j < tasksInLevels.get(i).size(); j++) {
                System.out.print(tasksInLevels.get(i).get(j).getIndex()+" ");
            }
            System.out.println();
        }
        int levelNum=tasksInLevels.size();
        System.out.println();

        // 2.使用 max-min 算法进行初分配
        // 调度结果，每个任务开始结束时刻，任务序列
        int[] scheduleRes=new int[K];
        double[][] beginAndEndTimes=new double[K][M];
        double[][] transmissionTimes=new double[K][M];
        double[][] computationTimes=new double[K][M];
        double[][] minPrevFinishTimes=new double[K][M];
        double[][] earliestStartTimes=new double[K][M];
        double[][] actualStartTimes=new double[K][M];
        double[][] waitTimes=new double[K][M];

        double[][] finishTimes=new double[K][M];
        double[][] costs=new double[K][M];

        double[][] states=new double[K][M];
        int[] numbers=new int[K];
        ArrayList<Integer> finishDistributeTasks=new ArrayList<>();
        double max=0;
        double min=100;
        int levelIdx=0;

        // 有了 RSU 的输入，t0 也用 max-min 算法分配
        // 通过 max-min 图计算可用时间=传输时间+执行时间，注意这两个时间次序不能变
        System.out.println("max-min:");
        while(levelIdx!=levelNum){
//            System.out.println("level "+(levelIdx+1)+":");
            ArrayList<Task> curTasks=(ArrayList<Task>)tasksInLevels.get(levelIdx++).clone();
            ArrayList<Task> tmpTasks=curTasks;
            boolean flag=true;
            int maxTask=-1;
            int deletedTask=-1;
            while(tmpTasks.size()!=0){
                // 计算 max-min 图
                max=0;
                // 第一次分配
                if(flag){
//                    System.out.println("first distribute:");
                    flag=false;
                    for(int i=0;i<tmpTasks.size();i++){
                        int taskIdx=tmpTasks.get(i).index-1;
//                        System.out.println("task "+(taskIdx+1)+":");
                        min=100;
                        for(int j=0;j<M;j++){
                            transmissionTimes[taskIdx][j]=0;
                            // 先分配任务，计算执行时间
                            computationTimes[taskIdx][j]=tasks[taskIdx].computation/vehicles[j].capability;

                            double minPrevFinishTime=100;
                            double earliestStartTime=0;
                            for(int k=0;k<K;k++){
                                // 考虑多前驱
                                if(dependency[taskIdx][k]>0){
                                    int prevVehicleIdx=scheduleRes[k];

                                    // 车辆需在 minPrevEndTime 时刻在 VC 范围内
                                    double prevFinishTime=finishTimes[k][scheduleRes[k]];
                                    if(prevFinishTime<minPrevFinishTime){
                                        minPrevFinishTime=prevFinishTime;
                                    }

                                    // 预估传输时间
                                    double estimateTransmissionTime=0;
                                    if(prevVehicleIdx!=j){
                                        estimateTransmissionTime=tasks[k].transmission/getTransmissionRate(prevVehicleIdx,j);
                                    }
                                    // 需要前驱任务全完成，包含传输完成
                                    if(prevFinishTime+estimateTransmissionTime>earliestStartTime){
                                        earliestStartTime=prevFinishTime+estimateTransmissionTime;
                                        // 取（前驱任务完成+传输）值最大的传输时间
                                        transmissionTimes[taskIdx][j]=estimateTransmissionTime;
                                    }
                                }
                            }

                            // 任务首节点
                            if(earliestStartTime==0){
                                earliestStartTime=inputRSU;
                            }
                            if(minPrevFinishTime==100){
                                minPrevFinishTime=inputRSU;
                            }

                            minPrevFinishTimes[taskIdx][j]=minPrevFinishTime;
                            earliestStartTimes[taskIdx][j]=earliestStartTime;

                            states[taskIdx][j]=earliestStartTimes[taskIdx][j]+computationTimes[taskIdx][j];

                            finishTimes[taskIdx][j]=states[taskIdx][j];
                            actualStartTimes[taskIdx][j]=earliestStartTimes[taskIdx][j];

                            // 保存不可用前的状态
                            double stateBeforeUnavailable=states[taskIdx][j];

                            // 该车辆必须是可用的
                            if(!isVehicleAvailable(taskIdx,j,minPrevFinishTimes,actualStartTimes,finishTimes,interval,scheduleRes,finishDistributeTasks)){
                                states[taskIdx][j]=UNAVAILABLESTATE;
                            }
                            if(states[taskIdx][j]<min){
                                min=states[taskIdx][j];
                                numbers[taskIdx]=j;
                            }
                            /*cout<<"vehicle "<<j+1<<": "<<endl;
                            cout<<"state: "<<states[taskIdx][j]<<endl;
                            cout<<"beginTime: "<<minPrevFinishTime<<endl;
                            cout<<"EST: "<<earliestStartTimes[taskIdx][j]<<endl;
                            cout<<"AST: "<<actualStartTimes[taskIdx][j]<<endl;
                            cout<<"endTime: "<<states[taskIdx][j]<<endl;
                            cout<<"transmissionTime: "<<transmissionTimes[taskIdx][j]<<endl;
                            cout<<"computationTime: "<<computationTimes[taskIdx][j]<<endl;
                            cout<<endl;*/

                            // 复原车辆不可用状态
                            states[taskIdx][j]=stateBeforeUnavailable;
                        }
                        // 先求 min，再求 max
                        if(min!=UNAVAILABLESTATE&&min>max){
                            max=min;
                            maxTask=taskIdx;
                            deletedTask=i;
                            scheduleRes[taskIdx]=numbers[taskIdx];
                            beginAndEndTimes[taskIdx][0]=minPrevFinishTimes[taskIdx][scheduleRes[taskIdx]];
                            beginAndEndTimes[taskIdx][1]=states[taskIdx][scheduleRes[taskIdx]];
                        }
                    }
                }else{
                    // 每个任务分配完后更新 max-min 图
                    // 再分配给同节点就需要排队，开始时间取排队任务和前驱任务完成时间的较大者
//                    cout<<"other distribute: "<<endl;
                    int sameVehicle=scheduleRes[maxTask];
                    int prevTask=maxTask;
                    max=0;
                    for(int i=0;i<tmpTasks.size();i++){
                        min=100;
                        int taskIdx=tmpTasks.get(i).index-1;
                        // update 过程更新的值
                        actualStartTimes[taskIdx][sameVehicle]=states[prevTask][sameVehicle];
                        // 取 ACT 和 EST 的最大者
                        if(actualStartTimes[taskIdx][sameVehicle]<earliestStartTimes[taskIdx][sameVehicle]){
                            actualStartTimes[taskIdx][sameVehicle]=earliestStartTimes[taskIdx][sameVehicle];
                        }
                        states[taskIdx][sameVehicle]=actualStartTimes[taskIdx][sameVehicle]+computationTimes[taskIdx][sameVehicle];

                        // update 过程更新的值
                        finishTimes[taskIdx][sameVehicle]=states[taskIdx][sameVehicle];
                        if(!isVehicleAvailable(taskIdx,sameVehicle,minPrevFinishTimes,actualStartTimes,finishTimes,interval,scheduleRes,finishDistributeTasks)){
                            states[taskIdx][sameVehicle]=UNAVAILABLESTATE;
                        }
                        /*cout<<"update task "<<taskIdx+1<<" distribute to vehicle "<<sameVehicle+1<<": "<<endl;
                        cout<<"state: "<<states[taskIdx][sameVehicle]<<endl;
                        cout<<endl;*/

                        // 原不可用的分配，可能变得可用
                        for(int j=0;j<M;j++){
                            // 保存不可用前的状态
                            double stateBeforeUnavailable=states[taskIdx][j];
                            if(!isVehicleAvailable(taskIdx,j,minPrevFinishTimes,actualStartTimes,finishTimes,interval,scheduleRes,finishDistributeTasks)){
                                states[taskIdx][j]=UNAVAILABLESTATE;
                            }
//                          cout<<"after update state: "<<states[taskIdx][j]<<endl;
                            if(states[taskIdx][j]<min){
                                min=states[taskIdx][j];
                                numbers[taskIdx]=j;
                            }
                            // 复原车辆不可用状态
                            states[taskIdx][j]=stateBeforeUnavailable;
                        }

                        if(min>max){
                            max=min;
                            maxTask=taskIdx;
                            deletedTask=i;
                            scheduleRes[taskIdx]=numbers[taskIdx];
                            beginAndEndTimes[taskIdx][0]=minPrevFinishTimes[taskIdx][scheduleRes[taskIdx]];
                            beginAndEndTimes[taskIdx][1]=states[taskIdx][scheduleRes[taskIdx]];
                            if(actualStartTimes[taskIdx][scheduleRes[taskIdx]]>earliestStartTimes[taskIdx][scheduleRes[taskIdx]]){
                                // 存在排队
                                // update 过程更新的值
                                waitTimes[taskIdx][scheduleRes[taskIdx]]=actualStartTimes[taskIdx][scheduleRes[taskIdx]]-earliestStartTimes[taskIdx][scheduleRes[taskIdx]];
                            }
                        }
                    }
                }
                // 完成一次分配
                /*cout<<"max: "<<endl;
                cout<<"task: "<<maxTask+1<<endl;
                cout<<"vehicle: "<<scheduleRes[maxTask]+1<<endl;
                cout<<"beginTime: "<<beginAndEndTimes[maxTask][0]<<endl;
                cout<<"EST: "<<earliestStartTimes[maxTask][scheduleRes[maxTask]]<<endl;
                cout<<"AST: "<<actualStartTimes[maxTask][scheduleRes[maxTask]]<<endl;
                cout<<"endTime: "<<beginAndEndTimes[maxTask][1]<<endl;
                cout<<"transmissionTime: "<<transmissionTimes[maxTask][scheduleRes[maxTask]]<<endl;
                cout<<"computationTime: "<<computationTimes[maxTask][scheduleRes[maxTask]]<<endl;
                cout<<"waitTime: "<<waitTimes[maxTask][scheduleRes[maxTask]]<<endl;*/

                finishDistributeTasks.add(maxTask);

                tmpTasks.remove(deletedTask);
                /*cout<<endl;
                cout<<"remaining tasks: "<<endl;
                for(int i=0;i<tmpTasks.size();i++){
                    cout<<tmpTasks[i].index<<endl;
                }
                cout<<endl;*/
            }
        }

        // max-min 算法基于 finish time 的初分配结果
        System.out.println("initial schedule result:");
        for(int i=0;i<K;i++){
            System.out.println("task "+(i+1)+" distribute to vehicle "+(scheduleRes[i]+1));
            System.out.println("from time "+beginAndEndTimes[i][0]+" to "+beginAndEndTimes[i][1]);
            /*System.out.println("EST: "+earliestStartTimes[i][scheduleRes[i]]);
            System.out.println("AST: "+actualStartTimes[i][scheduleRes[i]]);
            System.out.println("Tc: "+transmissionTimes[i][scheduleRes[i]]);
            System.out.println("Tp: "+computationTimes[i][scheduleRes[i]]);*/
            System.out.println();
        }

        System.out.println("total finishTime: "+finishTimes[K-1][scheduleRes[K-1]]);
        System.out.println();

        int[][][] tmpSequence=new int[levelNum][M][M];
        ArrayList<ArrayList<ArrayList<Integer>>> sequence=new ArrayList<>();
        for(int i=0;i<levelNum;i++){
            for(int j=0;j<M;j++){
                int idx=0;
                for(int k=0;k<tasksInLevels.get(i).size();k++){
                    if(scheduleRes[tasksInLevels.get(i).get(k).index-1]==j){
                        tmpSequence[i][j][idx++]=tasksInLevels.get(i).get(k).index;
                    }
                }
            }
        }

        // 按照排队任务的顺序
        for(int i=0;i<levelNum;i++){
            ArrayList<ArrayList<Integer>> aa=new ArrayList<>();
            for(int j=0;j<M;j++){
                ArrayList<Integer> a=new ArrayList<>();
                for(int k=0;k<M;k++){
                    double minSequence=100;
                    int minSequenceIdx=-1;
                    for(int l=0;l<M;l++){
                        if(tmpSequence[i][j][l]!=0&&finishTimes[tmpSequence[i][j][l]][j]<minSequence){
                            minSequence=finishTimes[tmpSequence[i][j][l]][j];
                            minSequenceIdx=l;
                        }
                    }
                    if(minSequenceIdx!=-1){
                        a.add(tmpSequence[i][j][minSequenceIdx]);
                        tmpSequence[i][j][minSequenceIdx]=0;
                    }
                }
                aa.add(a);
            }
            sequence.add(aa);
        }

        System.out.println("sequence:");
        for(int i=0;i<levelNum;i++){
            System.out.println("level "+(i+1)+":");
            for(int j=0;j<M;j++){
                if(sequence.get(i).get(j).size()!=0){
                    System.out.println("vehicle "+(j+1)+":");
                    for(int k=0;k<sequence.get(i).get(j).size();k++){
                        System.out.print(sequence.get(i).get(j).get(k)+" ");
                        if(k==sequence.get(i).get(j).size()-1){
                            System.out.println();
                        }
                    }
                }
            }
        }
    }
}
