package com.wind;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class CEDCS {
    static final int K=21;
    static final int M=10;
    static final int UNAVAILABLESTATE=100;
    static final double D=5*1.85;
    static final double inputRSU=(double)2/(double)3;
    static final int areaLength=1000;
    static final int areaWidth=20;
    static final double disToTopo=250;
    static final double SPEED=30;
    static final int experimentRound=50;

    static class Task{
        int index;    // 序号
        double computation;    // 计算负载
        double transmission;    // 传输负载

        public Task(int index, double computation, double transmission) {
            this.index = index;
            this.computation = computation;
            this.transmission = transmission;
        }

        public double getComputation() {
            return computation;
        }

        public double getTransmission() {
            return transmission;
        }

        public void setComputation(double computation) {
            this.computation = computation;
        }

        public void setTransmission(double transmission) {
            this.transmission = transmission;
        }
    }

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
    }

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
        // 最大7跳
        double[] bandwidth=new double[]{0,10,4.8,3.2,2.4,1.92,1.44,1.28};
        return bandwidth[hop];
    }

    // 获取两车间的传输速率
    static double getTransmissionRate(int v1,int v2,int[][] vehiclalNetworkTopology){
        /*int[][] vehiclalNetworkTopology=
                {{0,1,2,2,3,2},
                {1,0,3,2,2,1},
                {2,3,0,2,3,2},
                {2,2,2,0,1,3},
                {3,2,3,1,0,2},
                {2,1,2,3,3,0}};*/
        int hop=vehiclalNetworkTopology[v1][v2];
        return getTransmissionRateFromHop(hop);
    }

    /**
     * 生成仅有对角线是0，其他数值在[min,max]的对角矩阵，左闭右闭
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
        return matrix;
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
    static double[][] GenRandomCarInterval(int car_number,int start_time_minimun,int start_time_maximun,int finish_time_minimun,int finish_time_maximun){
        double[][] matrix = new double[car_number][2];
        for(int i =0;i<car_number;i++){
            matrix[i][0] = GenRandomDouble(start_time_minimun,start_time_maximun-start_time_minimun);
            matrix[i][1] = GenRandomDouble(finish_time_minimun,finish_time_maximun-finish_time_minimun);
        }
        return matrix;
    }

    // 随机生成车辆的初始位置
    static double[][] GenRandomCarPosition(){
        double[][] matrix = new double[M][2];
        for(int i =0;i<M;i++){
            matrix[i][0] = GenRandomDouble(0,areaLength);
            matrix[i][1] = GenRandomDouble(0,areaWidth);
        }
        return  matrix;
    }

    // 根据车辆位置计算两车间传输速率
    static int[][] calculateTransmissionRate(double[][] carPosition){
        int[][] res=new int[M][M];
        int[][] topo=new int[M][M];
        for (int i = 0; i < M-1; i++) {
            for (int j = i+1; j < M; j++) {
                double dis=Math.sqrt(Math.pow(carPosition[i][0]-carPosition[j][0],2)+Math.pow(carPosition[i][1]-carPosition[j][1],2));
                if(dis<=disToTopo){
                    topo[i][j]=1;
                }
            }
        }
        for (int i = 0; i < M-1; i++) {
            for (int j = i+1; j < M; j++) {
                res[i][j]=getTransmissionRateByDijkstra(topo,i,j);
            }
        }

        return res;
    }

    // Dijkstra算法计算两车间跳数
    static int getTransmissionRateByDijkstra(int[][] topo,int v1,int v2){
        boolean[] visit = new boolean[M]; // 标记某节点是否被访问过
        int[] res = new int[M];     // 记录 start 点到各点的最短路径长度
        for (int i = 0; i < M; i++) {
            res[i] = topo[v1][i];
        }

        // 查找 n - 1 次（n 为节点个数），每次确定一个节点
        for(int i = 1; i < M; i++) {
            int min = Integer.MAX_VALUE;
            int p = 0;
            // 找出一个未标记的离出发点最近的节点
            for(int j = 0; j < M; j++){
                if(j != v1 && !visit[j] && res[j] < min){
                    min = res[j];
                    p = j;
                }
            }
            // 标记该节点为已经访问过
            visit[p] = true;

            for (int j = 0; j < M; j++){
                if (j == p || topo[p][j] == 0) {  // p 点不能到达 j
                    continue;
                }
                if (res[p] + topo[p][j] < res[j]){
                    res[j] = res[p] + topo[p][j];  //更新最短路径
                }
            }
        }
        return res[v2];
//        return getTransmissionRateFromHop(hop);
    }

    // 根据车辆位置计算车辆在车载云的有效时间
    static double[][] calculateInterval(double[][] carPosition){
        double[][] interval = new double[M][2];
        for(int i =0;i<M;i++){
            // 车辆向右移动，只关注横坐标
            interval[i][1]=(areaLength-carPosition[i][0])/SPEED;
        }

        return interval;
    }

    /*static String Matix2Str(double[][] matrix){
        int x_dim = matrix.length;
        int y_dim = matrix[0].length;
        StringBuilder builder = new StringBuilder();
        for(int i =0;i<x_dim;i++){
            builder.append("|");
            for(int j =0;j<y_dim;j++){
                String number_str = String.format("%1f", matrix[i][j]);
                builder.append(number_str);
                builder.append("|");
            }
            builder.append("\n");
        }
        return builder.toString();
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
    }*/

    static int GenRandomInt(int base,int range){
        Random rand = new Random();
        int a = rand.nextInt(range+1);
        return base+a;
    }
    static double GenRandomDouble(int base,int range){
        int max = base+range;
        double b = ThreadLocalRandom.current().nextDouble(base,max+0.4f);
        b = max<b? max:b;
        return b;
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

    // 计算 ave 传输速率
    static double getAve(int[][] vehiclalNetworkTopology){
        double ave;
        double max=0;
        double min=100;
        for(int i=0;i<M;i++){
            for(int j=0;j<M&&j!=i;j++) {
                if(getTransmissionRate(i,j,vehiclalNetworkTopology)>max){
                    max=getTransmissionRate(i,j,vehiclalNetworkTopology);
                }
                if(getTransmissionRate(i,j,vehiclalNetworkTopology)<min){
                    min=getTransmissionRate(i,j,vehiclalNetworkTopology);
                }
            }
        }
        ave=(max+min)/2;
        return ave;
    }

    // 划分 sub-deadline
    static ArrayList<ArrayList<Double>> divideSubDeadline(ArrayList<ArrayList<Task>> tasksInLevels,double inputRSU,double ave){
        // 求每段 level 的起始时刻
        // 传输负载
        double transmissionSum=0;
        ArrayList<Double> maxTransmissionInLevels=new ArrayList<>();
        // 不需要计算 task1 的传输时间
        for(int i=0;i<tasksInLevels.size()-1;i++){
            double maxTransmission=0;
            for(int j=0;j<tasksInLevels.get(i).size();j++){
                if(tasksInLevels.get(i).get(j).transmission>maxTransmission){
                    maxTransmission=tasksInLevels.get(i).get(j).transmission;
                }
            }
            maxTransmissionInLevels.add(maxTransmission);
            transmissionSum+=maxTransmission;
        }

        // 按不同 Vc 计算
        double initialTransmissionTime=transmissionSum/ave;
        double initialComputationTime=D-inputRSU-initialTransmissionTime;

        // 计算负载
        double computationSum=0;
        ArrayList<Double> maxComputationInLevels=new ArrayList<>();
        for(int i=0;i<tasksInLevels.size();i++){
            double maxComputation=0;
            for(int j=0;j<tasksInLevels.get(i).size();j++){
                if(tasksInLevels.get(i).get(j).computation>maxComputation){
                    maxComputation=tasksInLevels.get(i).get(j).computation;
                }
            }
            maxComputationInLevels.add(maxComputation);
            computationSum+=maxComputation;
        }

        // 进行时间的初划分
        double[][] initialDivision=new double[tasksInLevels.size()][2];
        double tmpTime=inputRSU;
        int idx=0;
        while(idx<tasksInLevels.size()){
            initialDivision[idx][0]=tmpTime;
            // 执行时间
            tmpTime+=maxComputationInLevels.get(idx)/computationSum*initialComputationTime;

            // 注意不考虑 task1 的传输时间
            if(idx!=0){
                tmpTime+=maxTransmissionInLevels.get(idx-1)/ave;
            }
            initialDivision[idx][1]=tmpTime;
            idx++;
        }
        initialDivision[tasksInLevels.size()-1][1]=D;

        ArrayList<ArrayList<Double>> res=new ArrayList<>();
        for(int i=0;i<tasksInLevels.size();i++){
            ArrayList<Double> tmp=new ArrayList<>();
            for(int j=0;j<2;j++){
                tmp.add(initialDivision[i][j]);
            }
            res.add(tmp);
        }
        return res;
    }

// 计算优化分配后的总 time
/*double calculateTotalTime(vector<vector<Task>> taskLevel,int scheduleRes[K],double beginAndEndTimes[K][2]){
    for(int i=0;i<taskLevel.size();i++){
        vector<Task> tasksInLevel=taskLevel[i];
        // level 内的 tasks 存在排队的情况
    }
}*/

    // 计算优化分配后的总 cost
    static double calculateTotalCost(int[] scheduleRes,double[][] computationTimes,Vehicle[] vehicles){
        double totalCost=0;
        for(int i=0;i<K;i++){
            totalCost+=computationTimes[i][scheduleRes[i]]*vehicles[scheduleRes[i]].unitPrice;
        }
        return totalCost;
    }

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
        Task t15 = new Task(15, 2, 2);
        Task t16 = new Task(16, 1, 1);
        Task t17 = new Task(17, 5, 5);
        Task t18 = new Task(18, 2, 2);
        Task t19 = new Task(19, 3, 3);
        Task t20 = new Task(20, 3, 3);
        Task t21 = new Task(21, 4, 4);

        Task[] tasks = new Task[]{t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21};

        // 实验变量-负载成倍增加
        for (int i = 0; i < K; i++) {
            double curComputation=tasks[i].getComputation();
            double curTransmission=tasks[i].getTransmission();
            tasks[i].setComputation(curComputation*2);
            tasks[i].setTransmission(curTransmission*2);
        }

        // 车辆
        // 计价：cost=capability￿￿^2*1.2+0.8
        Vehicle v1 = new Vehicle(1, 5, 30.8);
        Vehicle v2 = new Vehicle(2, 6, 44);
        Vehicle v3 = new Vehicle(3, 7, 59.6);
        Vehicle v4 = new Vehicle(4, 8, 77.6);
        Vehicle v5 = new Vehicle(5, 9, 98);
        Vehicle v6 = new Vehicle(6, 10, 120.8);
        Vehicle v7 = new Vehicle(7, 5.5, 37.1);
        Vehicle v8 = new Vehicle(8, 6.5, 51.5);
        Vehicle v9 = new Vehicle(9, 7.5, 68.3);
        Vehicle v10 = new Vehicle(10, 8.5, 87.5);

        Vehicle[] vehicles = new Vehicle[]{v1, v2, v3, v4, v5, v6, v7, v8, v9, v10};

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
        double[][] dependency=new double[K][K];
        // DAG①
        /*dependency[1][0]=3;
        dependency[2][0]=3;
        dependency[3][0]=3;
        dependency[4][0]=3;
        dependency[5][0]=3;
        dependency[6][1]=2;
        dependency[7][1]=2;
        dependency[7][2]=3;
        dependency[9][2]=3;
        dependency[8][3]=4;
        dependency[10][3]=4;
        dependency[10][4]=3;
        dependency[10][5]=5;
        dependency[11][5]=5;
        dependency[12][6]=1;
        dependency[12][7]=4;
        dependency[13][7]=4;
        dependency[13][8]=2;
        dependency[14][8]=2;
        dependency[14][9]=5;
        dependency[15][9]=5;
        dependency[15][10]=3;
        dependency[17][10]=3;
        dependency[17][11]=3;
        dependency[16][12]=4;
        dependency[16][13]=3;
        dependency[18][13]=3;
        dependency[18][14]=2;
        dependency[19][14]=2;
        dependency[19][15]=1;
        dependency[19][17]=2;
        dependency[20][16]=5;
        dependency[20][18]=3;
        dependency[20][19]=3;*/

        // DAG②
        /*dependency[1][0]=3;
        dependency[2][0]=3;
        dependency[3][0]=3;
        dependency[4][0]=3;
        dependency[5][1]=2;
        dependency[6][1]=2;
        dependency[12][1]=2;
        dependency[6][2]=3;
        dependency[7][2]=3;
        dependency[9][2]=3;
        dependency[13][2]=3;
        dependency[7][3]=4;
        dependency[8][3]=4;
        dependency[14][3]=4;
        dependency[8][4]=3;
        dependency[9][4]=3;
        dependency[10][4]=3;
        dependency[15][4]=3;
        dependency[11][5]=5;
        dependency[11][6]=1;
        dependency[11][7]=4;
        dependency[11][8]=2;
        dependency[11][9]=5;
        dependency[11][10]=3;
        dependency[12][11]=3;
        dependency[13][11]=3;
        dependency[14][11]=3;
        dependency[15][11]=3;
        dependency[16][12]=4;
        dependency[16][13]=3;
        dependency[17][14]=2;
        dependency[17][15]=1;
        dependency[18][16]=5;
        dependency[19][17]=2;
        dependency[20][18]=3;
        dependency[20][19]=3;*/

        // DAG③
        dependency[1][0]=3;
        dependency[2][0]=3;
        dependency[3][1]=2;
        dependency[4][1]=2;
        dependency[5][1]=2;
        dependency[6][1]=2;
        dependency[7][2]=3;
        dependency[8][2]=3;
        dependency[9][2]=3;
        dependency[10][2]=3;
        dependency[12][3]=4;
        dependency[11][3]=4;
        dependency[13][4]=3;
        dependency[11][4]=3;
        dependency[14][5]=5;
        dependency[11][5]=5;
        dependency[15][6]=1;
        dependency[11][6]=1;
        dependency[11][7]=4;
        dependency[16][7]=4;
        dependency[11][8]=2;
        dependency[17][8]=2;
        dependency[18][9]=5;
        dependency[11][9]=5;
        dependency[11][10]=3;
        dependency[19][10]=3;
        dependency[20][12]=4;
        dependency[20][13]=3;
        dependency[20][14]=2;
        dependency[20][15]=1;
        dependency[20][16]=5;
        dependency[20][17]=2;
        dependency[20][18]=3;
        dependency[20][19]=3;

                /*{{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  //1
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
        //1 2 3 4 5 6 7 8 9 1011121314*/

        double serviceSuccessRate=0;
        int serviceSuccessIdx=0;
        double averageServiceCost=0;
        for (int z = 0; z < experimentRound; z++) {
            // 每次实验的车辆初始位置变化导致：
            // 1.车辆间传输速率变化
//            System.out.println("car position:");
            double[][] carPosition=GenRandomCarPosition();
//            for (int i = 0; i < M; i++) {
//                for (int j = 0; j < 2; j++) {
//                    System.out.print(carPosition[i][j]+" ");
//                }
//                System.out.println();
//            }
//            System.out.println();
//            System.out.println("vehiclal network topology:");
            int[][] vehiclalNetworkTopology = calculateTransmissionRate(carPosition);
            /*for (int i = 0; i < M; i++) {
                for (int j = 0; j < M; j++) {
                    System.out.print(vehiclalNetworkTopology[i][j]+" ");
                }
                System.out.println();
            }
            System.out.println();
            System.out.println("interval:");*/
            double[][] interval = calculateInterval(carPosition);
            /*for (int i = 0; i < M; i++) {
                for (int j = 0; j < 2; j++) {
                    System.out.print(interval[i][j]+" ");
                }
                System.out.println();
            }
            System.out.println();*/

//            int[][] vehiclalNetworkTopology=GenRandomSquareMatrix(M, 1, 4);
        /*for (int i = 0; i < M; i++) {
            for (int j = 0; j < M; j++) {
                System.out.print(vehiclalNetworkTopology[i][j]+" ");
            }
            System.out.println();
        }
        System.out.println();*/

        // 2.车辆停留在VC的时间有效时间变化
//        double[][] interval =GenRandomCarInterval(M, 0, 1, 99, 100);
        /*{{0, 20},
                {0, 20},
                {0, 20},
                {0, 20},
                {0, 20},
                {0, 20}};*/
        /*for (int i = 0; i < M; i++) {
            for (int j = 0; j < 2; j++) {
                System.out.print(interval[i][j]+" ");
            }
            System.out.println();
        }
        System.out.println();*/

            // 第二部分：算法
            // 1.拓扑排序对 DAG 划分多个 level
            ArrayList<ArrayList<Integer>> levelRes = topologicalSort(tasks, dependency);
            ArrayList<ArrayList<Task>> tasksInLevels = new ArrayList<>();
            for (int i = 0; i < levelRes.size(); i++) {
                ArrayList<Task> tasksInOneLevel = new ArrayList<>();
                for (int j = 0; j < levelRes.get(i).size(); j++) {
                    tasksInOneLevel.add(tasks[levelRes.get(i).get(j)]);
                }
                tasksInLevels.add(tasksInOneLevel);
            }
            vehiclalNetworkTopology=GenRandomSquareMatrix(M, 1, 4);

            for (int i = 0; i < tasksInLevels.size(); i++) {
                for (int j = 0; j < tasksInLevels.get(i).size(); j++) {
//                    System.out.print(tasksInLevels.get(i).get(j).getIndex() + " ");
                }
//                System.out.println();
            }
            int levelNum = tasksInLevels.size();
            interval =GenRandomCarInterval(M, 0, 1, 99, 100);
//            System.out.println();

            // 2.使用 max-min 算法进行初分配
            // 调度结果，每个任务开始结束时刻，任务序列
            int[] scheduleRes = new int[K];
            double[][] beginAndEndTimes = new double[K][M];
            double[][] transmissionTimes = new double[K][M];
            double[][] computationTimes = new double[K][M];
            double[][] minPrevFinishTimes = new double[K][M];
            double[][] earliestStartTimes = new double[K][M];
            double[][] actualStartTimes = new double[K][M];
            double[][] waitTimes = new double[K][M];
            double[][] finishTimes = new double[K][M];
            ArrayList<Integer> finishDistributeTasks = new ArrayList<>();

            double[][] states = new double[K][M];
            int[] numbers = new int[K];
            double max = 0;
            double min = 100;
            int levelIdx = 0;

            // 有了 RSU 的输入，t0 也用 max-min 算法分配
            // 通过 max-min 图计算可用时间=传输时间+执行时间，注意这两个时间次序不能变
//        System.out.println("max-min:");
            while (levelIdx != levelNum) {
//            System.out.println("level "+(levelIdx+1)+":");
                ArrayList<Task> curTasks = (ArrayList<Task>) tasksInLevels.get(levelIdx++).clone();
                ArrayList<Task> tmpTasks = curTasks;
                boolean flag = true;
                int maxTask = -1;
                int deletedTask = -1;
                while (tmpTasks.size() != 0) {
                    // 计算 max-min 图
                    max = 0;
                    // 第一次分配
                    if (flag) {
//                    System.out.println("first distribute:");
                        flag = false;
                        for (int i = 0; i < tmpTasks.size(); i++) {
                            int taskIdx = tmpTasks.get(i).index - 1;
//                        System.out.println("task "+(taskIdx+1)+":");
                            min = 100;
                            for (int j = 0; j < M; j++) {
                                transmissionTimes[taskIdx][j] = 0;
                                // 先分配任务，计算执行时间
                                computationTimes[taskIdx][j] = tasks[taskIdx].computation / vehicles[j].capability;

                                double minPrevFinishTime = 100;
                                double earliestStartTime = 0;
                                for (int k = 0; k < K; k++) {
                                    // 考虑多前驱
                                    if (dependency[taskIdx][k] > 0) {
                                        int prevVehicleIdx = scheduleRes[k];

                                        // 车辆需在 minPrevEndTime 时刻在 VC 范围内
                                        double prevFinishTime = finishTimes[k][scheduleRes[k]];
                                        if (prevFinishTime < minPrevFinishTime) {
                                            minPrevFinishTime = prevFinishTime;
                                        }

                                        // 预估传输时间
                                        double estimateTransmissionTime = 0;
                                        if (prevVehicleIdx != j) {
                                            estimateTransmissionTime = tasks[k].transmission / getTransmissionRate(prevVehicleIdx, j,vehiclalNetworkTopology);
                                        }
                                        // 需要前驱任务全完成，包含传输完成
                                        if (prevFinishTime + estimateTransmissionTime > earliestStartTime) {
                                            earliestStartTime = prevFinishTime + estimateTransmissionTime;
                                            // 取（前驱任务完成+传输）值最大的传输时间
                                            transmissionTimes[taskIdx][j] = estimateTransmissionTime;
                                        }
                                    }
                                }

                                // 任务首节点
                                if (earliestStartTime == 0) {
                                    earliestStartTime = inputRSU;
                                }
                                if (minPrevFinishTime == 100) {
                                    minPrevFinishTime = inputRSU;
                                }

                                minPrevFinishTimes[taskIdx][j] = minPrevFinishTime;
                                earliestStartTimes[taskIdx][j] = earliestStartTime;

                                states[taskIdx][j] = earliestStartTimes[taskIdx][j] + computationTimes[taskIdx][j];

                                finishTimes[taskIdx][j] = states[taskIdx][j];
                                actualStartTimes[taskIdx][j] = earliestStartTimes[taskIdx][j];

                                // 保存不可用前的状态
                                double stateBeforeUnavailable = states[taskIdx][j];

                                // 该车辆必须是可用的
                                if (!isVehicleAvailable(taskIdx, j, minPrevFinishTimes, actualStartTimes, finishTimes, interval, scheduleRes, finishDistributeTasks)) {
                                    states[taskIdx][j] = UNAVAILABLESTATE;
                                }
                                if (states[taskIdx][j] < min) {
                                    min = states[taskIdx][j];
                                    numbers[taskIdx] = j;
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
                                states[taskIdx][j] = stateBeforeUnavailable;
                            }
                            // 先求 min，再求 max
                            if (min != UNAVAILABLESTATE && min > max) {
                                max = min;
                                maxTask = taskIdx;
                                deletedTask = i;
                                scheduleRes[taskIdx] = numbers[taskIdx];
                                beginAndEndTimes[taskIdx][0] = minPrevFinishTimes[taskIdx][scheduleRes[taskIdx]];
                                beginAndEndTimes[taskIdx][1] = states[taskIdx][scheduleRes[taskIdx]];
                            }
                        }
                    } else {
                        // 每个任务分配完后更新 max-min 图
                        // 再分配给同节点就需要排队，开始时间取排队任务和前驱任务完成时间的较大者
//                    cout<<"other distribute: "<<endl;
                        int sameVehicle = scheduleRes[maxTask];
                        int prevTask = maxTask;
                        max = 0;
                        for (int i = 0; i < tmpTasks.size(); i++) {
                            min = 100;
                            int taskIdx = tmpTasks.get(i).index - 1;
                            // update 过程更新的值
                            actualStartTimes[taskIdx][sameVehicle] = states[prevTask][sameVehicle];
                            // 取 ACT 和 EST 的最大者
                            if (actualStartTimes[taskIdx][sameVehicle] < earliestStartTimes[taskIdx][sameVehicle]) {
                                actualStartTimes[taskIdx][sameVehicle] = earliestStartTimes[taskIdx][sameVehicle];
                            }
                            states[taskIdx][sameVehicle] = actualStartTimes[taskIdx][sameVehicle] + computationTimes[taskIdx][sameVehicle];

                            // update 过程更新的值
                            finishTimes[taskIdx][sameVehicle] = states[taskIdx][sameVehicle];
                            if (!isVehicleAvailable(taskIdx, sameVehicle, minPrevFinishTimes, actualStartTimes, finishTimes, interval, scheduleRes, finishDistributeTasks)) {
                                states[taskIdx][sameVehicle] = UNAVAILABLESTATE;
                            }
                        /*cout<<"update task "<<taskIdx+1<<" distribute to vehicle "<<sameVehicle+1<<": "<<endl;
                        cout<<"state: "<<states[taskIdx][sameVehicle]<<endl;
                        cout<<endl;*/

                            // 原不可用的分配，可能变得可用
                            for (int j = 0; j < M; j++) {
                                // 保存不可用前的状态
                                double stateBeforeUnavailable = states[taskIdx][j];
                                if (!isVehicleAvailable(taskIdx, j, minPrevFinishTimes, actualStartTimes, finishTimes, interval, scheduleRes, finishDistributeTasks)) {
                                    states[taskIdx][j] = UNAVAILABLESTATE;
                                }
//                          cout<<"after update state: "<<states[taskIdx][j]<<endl;
                                if (states[taskIdx][j] < min) {
                                    min = states[taskIdx][j];
                                    numbers[taskIdx] = j;
                                }
                                // 复原车辆不可用状态
                                states[taskIdx][j] = stateBeforeUnavailable;
                            }

                            if (min > max) {
                                max = min;
                                maxTask = taskIdx;
                                deletedTask = i;
                                scheduleRes[taskIdx] = numbers[taskIdx];
                                beginAndEndTimes[taskIdx][0] = minPrevFinishTimes[taskIdx][scheduleRes[taskIdx]];
                                beginAndEndTimes[taskIdx][1] = states[taskIdx][scheduleRes[taskIdx]];
                                if (actualStartTimes[taskIdx][scheduleRes[taskIdx]] > earliestStartTimes[taskIdx][scheduleRes[taskIdx]]) {
                                    // 存在排队
                                    // update 过程更新的值
                                    waitTimes[taskIdx][scheduleRes[taskIdx]] = actualStartTimes[taskIdx][scheduleRes[taskIdx]] - earliestStartTimes[taskIdx][scheduleRes[taskIdx]];
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
//            System.out.println("max-min schedule result:");
            for (int i = 0; i < K; i++) {
            /*System.out.println("task " + (i + 1) + " distribute to vehicle " + (scheduleRes[i] + 1));
            System.out.println("from time " + beginAndEndTimes[i][0] + " to " + beginAndEndTimes[i][1]);*/
            /*System.out.println("EST: "+earliestStartTimes[i][scheduleRes[i]]);
            System.out.println("AST: "+actualStartTimes[i][scheduleRes[i]]);
            System.out.println("Tc: "+transmissionTimes[i][scheduleRes[i]]);
            System.out.println("Tp: "+computationTimes[i][scheduleRes[i]]);*/
//            System.out.println();
            }

            double totalFinishTime=finishTimes[K-1][scheduleRes[K-1]];

            /*System.out.println("total finishTime: "+totalFinishTime);
            System.out.println();*/

            int[][][] ts = new int[levelNum][M][M];
            ArrayList<ArrayList<ArrayList<Integer>>> sequence = new ArrayList<>();
            for (int i = 0; i < levelNum; i++) {
                for (int j = 0; j < M; j++) {
                    int idx = 0;
                    for (int k = 0; k < tasksInLevels.get(i).size(); k++) {
                        if (scheduleRes[tasksInLevels.get(i).get(k).index - 1] == j) {
                            ts[i][j][idx++] = tasksInLevels.get(i).get(k).index;
                        }
                    }
                }
            }

            // 按照排队任务的顺序
            for (int i = 0; i < levelNum; i++) {
                ArrayList<ArrayList<Integer>> aa = new ArrayList<>();
                for (int j = 0; j < M; j++) {
                    ArrayList<Integer> a = new ArrayList<>();
                    for (int k = 0; k < M; k++) {
                        double minSequence = 100;
                        int minSequenceIdx = -1;
                        for (int l = 0; l < M; l++) {
                            if (ts[i][j][l] != 0 && finishTimes[ts[i][j][l]-1][j] < minSequence) {
                                minSequence = finishTimes[ts[i][j][l]-1][j];
                                minSequenceIdx = l;
                            }
                        }
                        if (minSequenceIdx != -1) {
                            a.add(ts[i][j][minSequenceIdx]-1);
                            ts[i][j][minSequenceIdx] = 0;
                        }
                    }
                    aa.add(a);
                }
                sequence.add(aa);
            }

//        System.out.println("sequence:");
            for (int i = 0; i < levelNum; i++) {
//            System.out.println("level " + (i + 1) + ":");
                for (int j = 0; j < M; j++) {
                    if (sequence.get(i).get(j).size() != 0) {
//                    System.out.print("vehicle " + (j + 1) + ": ");
                        for (int k = 0; k < sequence.get(i).get(j).size(); k++) {
//                        System.out.print((sequence.get(i).get(j).get(k)+1) + " ");
                            if (k == sequence.get(i).get(j).size() - 1) {
//                            System.out.println();
                            }
                        }
                    }
                }
            }
//        System.out.println();

            // 3.根据 deadline、预测传输时间、负载，计算 sub-deadline
            // 计算车辆在 VC 内运动时 ave Vc
            double ave=getAve(vehiclalNetworkTopology);

        /*System.out.println("ave: "+ave);
        System.out.println();*/

            // 划分时间，计算 sub-deadline
            ArrayList<ArrayList<Double>> subDeadlines=divideSubDeadline(tasksInLevels,inputRSU,ave);

//            System.out.println("timeDivision:");
            for(int i=0;i<levelNum;i++){
//                System.out.println(subDeadlines.get(i).get(0)+" "+subDeadlines.get(i).get(1));
            }
//            System.out.println();

            int reScheduleFlag=-1;

            // 4.初分配总完成时间超出 deadline，调整 t 变小，满足 deadline
            if(totalFinishTime>D){
//                System.out.println("need time re schedule!");
                for(int i=1;i<levelNum;i++){
                    if(reScheduleFlag==1){
                        break;
                    }

//                System.out.println("level "+(i+1)+":");
                    double tx=subDeadlines.get(i).get(1);
//                System.out.println("tx: "+tx);

                    // 找到 ti>tx 的任务，加入未标记队列
                    ArrayList<Task> tasksInOneLevel=(ArrayList<Task>)tasksInLevels.get(i).clone();
                    ArrayList<Task> unmarkQueue=(ArrayList<Task>)tasksInLevels.get(i).clone();
                    ArrayList<Task> markQueue=new ArrayList<>();

                    while(unmarkQueue.size()!=0){
                        if(reScheduleFlag==1){
                            break;
                        }
                        ArrayList<Task> tmpTasks=new ArrayList<>();

                        // 对目前未分配任务中 ti>tx 的任务进行排序，每次重分配完成一个任务后，都需要进行排序
                        for(int j=0;j<unmarkQueue.size();j++){
                            int taskIdx=unmarkQueue.get(j).index-1;
                            double ti=beginAndEndTimes[taskIdx][1];
                            if(ti>tx){
                                tmpTasks.add(unmarkQueue.get(j));
                            }
                        }
                        double maxTmpTasks=0;
                        int maxIdx=-1;
                        for(int j=0;j<tmpTasks.size();j++){
                            int taskIdx=tmpTasks.get(j).index-1;
                            double ti=beginAndEndTimes[taskIdx][1];
                            if(ti-tx>maxTmpTasks){
                                maxTmpTasks=ti-tx;
                                maxIdx=j;
                            }
                        }
                        if(maxIdx==-1){
                            break;
                        }

                        // 取当前 ti-tx 值最大的进行重分配
                        Task task=tmpTasks.get(maxIdx);
                        int taskIdx=task.index-1;
//                    System.out.println("re schedule task "+(taskIdx+1)+":");
                        int distributeVehicleIdx=scheduleRes[taskIdx];
                        double actualStartTimeBefore=actualStartTimes[taskIdx][distributeVehicleIdx];

                        // 有多个节点的重分配方案可行
                        ArrayList<Integer> availableNode=new ArrayList<>();
                        int[] scheduleResBefore=scheduleRes.clone();
                        double[][] beginAndEndTimesBefore=new double[K][M];
                        double[][] transmissionTimesBefore=new double[K][M];
                        double[][] minPrevFinishTimesBefore=new double[K][M];
                        double[][] earliestStartTimesBefore=new double[K][M];
                        double[][] actualStartTimesBefore=new double[K][M];
                        double[][] waitTimesBefore=new double[K][M];
                        double[][] finishTimesBefore=new double[K][M];
                        for (int j = 0; j < K; j++) {
                            beginAndEndTimesBefore[j]=beginAndEndTimes[j].clone();
                        }
                        for (int j = 0; j < K; j++) {
                            transmissionTimesBefore[j]=transmissionTimes[j].clone();
                        }
                        for (int j = 0; j < K; j++) {
                            minPrevFinishTimesBefore[j]=minPrevFinishTimes[j].clone();
                        }
                        for (int j = 0; j < K; j++) {
                            earliestStartTimesBefore[j]=earliestStartTimes[j].clone();
                        }
                        for (int j = 0; j < K; j++) {
                            actualStartTimesBefore[j]=actualStartTimes[j].clone();
                        }
                        for (int j = 0; j < K; j++) {
                            waitTimesBefore[j]=waitTimes[j].clone();
                        }
                        for (int j = 0; j < K; j++) {
                            finishTimesBefore[j]=finishTimes[j].clone();
                        }
                        ArrayList<ArrayList<ArrayList<Integer>>> sequenceBefore=new ArrayList<>();
                        for (int j = 0; j < levelNum; j++) {
                            ArrayList<ArrayList<Integer>> tmpJ=new ArrayList<>();
                            for (int k = 0; k < M; k++) {
                                ArrayList<Integer> tmpK=new ArrayList<>();
                                for (int l = 0; l < sequence.get(j).get(k).size(); l++) {
                                    tmpK.add(sequence.get(j).get(k).get(l));
                                }
                                tmpJ.add(tmpK);
                            }
                            sequenceBefore.add(tmpJ);
                        }
//                    ArrayList<ArrayList<ArrayList<Integer>>> sequenceBefore=(ArrayList<ArrayList<ArrayList<Integer>>>)sequence.clone();

                        int[][] tmpScheduleRes=new int[M][K];
                        double[][][] tmpBeginAndEndTimes=new double[M][K][2];
                        double[][][] tmpTransmissionTimes=new double[M][K][M];
                        double[][][] tmpMinPrevFinishTimes=new double[M][K][M];
                        double[][][] tmpEarliestStartTimes=new double[M][K][M];
                        double[][][] tmpActualStartTimes=new double[M][K][M];
                        double[][][] tmpWaitTimes=new double[M][K][M];
                        double[][][] tmpFinishTimes=new double[M][K][M];
                        ArrayList<ArrayList<ArrayList<ArrayList<Integer>>>> tmpSequence=new ArrayList<>();
                        for (int j = 0; j < M; j++) {
                            tmpSequence.add(new ArrayList<>());
                        }

                        double minDiffLess=100;
                        double minDiffMore=100;
                        int minDiffVehicleIdxLess=-1;
                        int minDiffVehicleIdxMore=-1;

                        for(int j=0;j<M;j++){
                            if(j==distributeVehicleIdx){
                                continue;
                            }
//                        System.out.println("re schedule to vehicle "+(j+1)+":");

                            scheduleRes[taskIdx]=j;
                            int flag=1;
                            int tiFlag=1;
                            finishDistributeTasks.clear();
                            for (int k = 0; k < i; k++) {
                                for (int l = 0; l < tasksInLevels.get(k).size(); l++) {
//                                System.out.print(tasksInLevels.get(k).get(l).index+" ");
                                    finishDistributeTasks.add(tasksInLevels.get(k).get(l).index - 1);
                                }
                            }

                            ArrayList<Integer> tmpList=new ArrayList<>();
                            ArrayList<Integer> waitList=new ArrayList<>();

                            // 前驱任务分配方案变化，需要重新计算 Tc 和 EST
                            double minPrevFinishTime=100;
                            double earliestStartTime=0;
                            double transmissionTime=0;
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
                                        estimateTransmissionTime=tasks[k].transmission/getTransmissionRate(prevVehicleIdx,j,vehiclalNetworkTopology);
                                    }
                                    // 需要前驱任务全完成，包含传输完成
                                    if(prevFinishTime+estimateTransmissionTime>earliestStartTime){
                                        earliestStartTime=prevFinishTime+estimateTransmissionTime;
                                        // 取（前驱任务完成+传输）值最大的传输时间
                                        transmissionTime=estimateTransmissionTime;
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
                            transmissionTimes[taskIdx][j]=transmissionTime;

                            beginAndEndTimes[taskIdx][0]=minPrevFinishTimes[taskIdx][j];

//                        System.out.println("MPFT: "+minPrevFinishTimes[taskIdx][j]);
//                        System.out.println("EST: "+earliestStartTimes[taskIdx][j]);
//                        System.out.println("Tc: "+transmissionTimes[taskIdx][j]);

                            // 重分配遇到和 level 内其他任务分配到同一节点的排队情况，根据 EST 大小顺序进行处理
                            tmpList.add(taskIdx);
                            for(int k=0;k<tasksInOneLevel.size();k++){
                                if(tasksInOneLevel.get(k).index-1!=taskIdx&&scheduleRes[tasksInOneLevel.get(k).index-1]==j){
                                    tmpList.add(tasksInOneLevel.get(k).index-1);
                                }
                            }

//                        System.out.println();
//                        System.out.println("wait list:");
                            while(tmpList.size()!=0){
                                double minEST=100;
                                int minESTidx=-1;
                                for(int k=0;k<tmpList.size();k++){
                                    if(earliestStartTimes[tmpList.get(k)][j]<minEST){
                                        minEST=earliestStartTimes[tmpList.get(k)][j];
                                        minESTidx=k;
                                    }
                                }
//                            System.out.println("task "+(tmpList.get(minESTidx)+1)+" ");
//                            System.out.println("EST "+earliestStartTimes[tmpList.get(minESTidx)][j]);

                                waitList.add(tmpList.get(minESTidx));
                                tmpList.remove(minESTidx);
                            }
//                        System.out.println();

                            // waitList 必有一个元素
                            double prevFinishTime=earliestStartTimes[waitList.get(0)][j];

                            // 维护 sequence
                            sequence.get(i).get(j).clear();
                            for(int k=0;k<waitList.size();k++){
                                int waitTaskIdx=waitList.get(k);
//                            System.out.println("task "+(waitTaskIdx+1)+":");

                                // 排队为串行执行
                                actualStartTimes[waitTaskIdx][j]=prevFinishTime;
                                finishTimes[waitTaskIdx][j]=actualStartTimes[waitTaskIdx][j]+computationTimes[waitTaskIdx][j];
                                waitTimes[waitTaskIdx][j]=actualStartTimes[waitTaskIdx][j]-earliestStartTimes[waitTaskIdx][j];

                                prevFinishTime=finishTimes[waitTaskIdx][j];

                                beginAndEndTimes[waitTaskIdx][1]=finishTimes[waitTaskIdx][j];

                                sequence.get(i).get(j).add(waitTaskIdx);

//                            System.out.println("AST "+actualStartTimes[waitTaskIdx][j]);
//                            System.out.println("FT "+finishTimes[waitTaskIdx][j]);
//                            System.out.println("WT "+waitTimes[waitTaskIdx][j]);
//                            System.out.println();
                            }

                            // 更新 AST FT 后再检查可用性
                            for(int k=0;k<waitList.size();k++){
                                int waitTaskIdx=waitList.get(k);
                                // 该车辆必须是可用的
                                if(!isVehicleAvailable(waitTaskIdx,j,minPrevFinishTimes,actualStartTimes,finishTimes,interval,scheduleRes,finishDistributeTasks)){
                                    flag=-1;
                                    tiFlag=-1;
                                }
                            }

//                        System.out.println("update sequence task "+(taskIdx+1)+" to vehicle "+(j+1)+":");
                            for(int k=0;k<sequence.get(i).get(j).size();k++){
//                            System.out.print((sequence.get(i).get(j).get(k)+1)+" ");
                            }
//                        System.out.println();
//                        System.out.println();

                            // 更新其他任务
                            // 原分配节点上排队的任务
                            for(int k=0;k<tasksInOneLevel.size();k++){
                                if(scheduleRes[tasksInOneLevel.get(k).index-1]==distributeVehicleIdx){
//                                System.out.println("before wait list on vehicle "+(distributeVehicleIdx+1)+":");
                                    ArrayList<Integer> sequenceOnBeforeVehicle=(ArrayList<Integer>)sequence.get(i).get(distributeVehicleIdx).clone();
                                    // 重分配的任务后的任务向前移动一格
                                    for(int l=0;l<sequenceOnBeforeVehicle.size()-1;l++){
                                        if(sequenceOnBeforeVehicle.get(l)==taskIdx){
                                            prevFinishTime=actualStartTimeBefore;
                                            for(int m=l+1;m<sequenceOnBeforeVehicle.size();m++){
//                                            System.out.println("task "+(sequenceOnBeforeVehicle.get(m)+1)+":");
                                                actualStartTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx]=prevFinishTime;
                                                finishTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx]=actualStartTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx]+computationTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx];
                                                waitTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx]=actualStartTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx]-earliestStartTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx];

                                                prevFinishTime=finishTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx];

                                                beginAndEndTimes[sequenceOnBeforeVehicle.get(m)][1]=finishTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx];

                                                // 该车辆必须是可用的
                                                if(!isVehicleAvailable(sequenceOnBeforeVehicle.get(l),distributeVehicleIdx,minPrevFinishTimes,actualStartTimes,finishTimes,interval,scheduleRes,finishDistributeTasks)){
                                                    flag=-1;
                                                    tiFlag=-1;
                                                }

//                                            System.out.println("AST: "+actualStartTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx]);
//                                            System.out.println("FT: "+finishTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx]);
//                                            System.out.println("WT: "+waitTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx]);
                                            }
                                            break;
                                        }
                                    }
//                                System.out.println();

                                    sequence.get(i).get(distributeVehicleIdx).clear();
                                    for(int l=0;l<sequenceOnBeforeVehicle.size()-1;l++){
                                        if(sequenceOnBeforeVehicle.get(l)!=taskIdx){
                                            sequence.get(i).get(distributeVehicleIdx).add(sequenceOnBeforeVehicle.get(l));
                                        }
                                    }
//                                System.out.println("before sequence task "+(taskIdx+1)+" to vehicle "+(distributeVehicleIdx+1)+":");
                                    for(int l=0;l<sequence.get(i).get(distributeVehicleIdx).size();l++){
//                                    System.out.print((sequence.get(i).get(distributeVehicleIdx).get(l)+1)+" ");
                                    }
//                                System.out.println();
                                    break;
                                }
                            }

                            tmpList.clear();
                            waitList.clear();

//                        System.out.println();
//                        System.out.println("mark queue:");
                            for(int k=0;k<markQueue.size();k++){
//                            System.out.println("task "+markQueue.get(k).index+" FT: "+finishTimes[markQueue.get(k).index-1][scheduleRes[markQueue.get(k).index-1]]);
                                // 已处理的任务完成时间仍小于 tx 或不能增加
                                if(!(finishTimes[markQueue.get(k).index-1][scheduleRes[markQueue.get(k).index-1]]<tx||finishTimes[markQueue.get(k).index-1][scheduleRes[markQueue.get(k).index-1]]<=finishTimesBefore[markQueue.get(k).index-1][scheduleRes[markQueue.get(k).index-1]])){
//                                System.out.println("mark queue fail!");
                                    flag=-1;
                                    tiFlag=-1;
                                }
                            }

                            // 一个level接一个level进行完成
//                        System.out.println();
//                        System.out.print("finish distribute tasks: ");
                            for(int k=0;k<=i;k++){
                                for(int l=0;l<tasksInLevels.get(k).size();l++){
//                                System.out.print(tasksInLevels.get(k).get(l).index+" ");
                                    finishDistributeTasks.add(tasksInLevels.get(k).get(l).index-1);
                                }
                            }
//                        System.out.println();

                            // 重分配影响的是：1.同level的前后车辆 2.后续level的后继任务+同车上的可用性
//                        System.out.println();
//                        System.out.println("follow up level:");
                            // 后续 level，按车辆上的排队顺序进行更新
                            for(int l=i+1;l<levelNum;l++){
//                            System.out.println("level "+(l+1)+":");
                                for (int m = 0; m < tasksInLevels.get(l-1).size(); m++) {
//                                System.out.print(tasksInLevels.get(k).get(l).index+" ");
                                    finishDistributeTasks.add(tasksInLevels.get(l-1).get(m).index - 1);
                                }
                                for(int m=0;m<M;m++){
                                    if(sequence.get(l).get(m).size()==0){
                                        continue;
                                    }
//                                System.out.println("vehicle "+(m+1)+":");
                                    // 更新 EST
                                    for(int n=0;n<sequence.get(l).get(m).size();n++){
                                        int followupTaskIdx=sequence.get(l).get(m).get(n);
//                                    System.out.println("task "+(followupTaskIdx+1)+":");

                                        double followupMinPrevFinishTime=100;
                                        double followupEarliestStartTime=0;
                                        double followupTransmissionTime=0;
                                        for(int k=0;k<K;k++){
                                            // 考虑多前驱
                                            if(dependency[followupTaskIdx][k]>0){
                                                int prevVehicleIdx=scheduleRes[k];

                                                // 车辆需在 minPrevEndTime 时刻在 VC 范围内
                                                prevFinishTime=finishTimes[k][scheduleRes[k]];
                                                if(prevFinishTime<followupMinPrevFinishTime){
                                                    followupMinPrevFinishTime=prevFinishTime;
                                                }

                                                // 预估传输时间
                                                double estimateTransmissionTime=0;
                                                if(prevVehicleIdx!=m){
                                                    // 前驱任务的传输量
                                                    estimateTransmissionTime=tasks[k].transmission/getTransmissionRate(prevVehicleIdx,m,vehiclalNetworkTopology);
                                                }
                                                // 需要前驱任务全完成，包含传输完成
                                                if(prevFinishTime+estimateTransmissionTime>followupEarliestStartTime){
                                                    followupEarliestStartTime=prevFinishTime+estimateTransmissionTime;
                                                    // 取（前驱任务完成+传输）值最大的传输时间
                                                    followupTransmissionTime=estimateTransmissionTime;
                                                }
                                            }
                                        }
                                        minPrevFinishTimes[followupTaskIdx][m]=followupMinPrevFinishTime;
                                        earliestStartTimes[followupTaskIdx][m]=followupEarliestStartTime;
                                        transmissionTimes[followupTaskIdx][m]=followupTransmissionTime;

                                        beginAndEndTimes[followupTaskIdx][0]=followupMinPrevFinishTime;
                                    }

                                    prevFinishTime=earliestStartTimes[sequence.get(l).get(m).get(0)][m];
                                    for(int n=0;n<sequence.get(l).get(m).size();n++){
                                        int followupTaskIdx=sequence.get(l).get(m).get(n);
                                        actualStartTimes[followupTaskIdx][m]=prevFinishTime;

                                        double maxSameVehicleFinishTime=0;
                                        // 等待前level内同车的任务完成
                                        for(int o=0;o<l;o++){
                                            for(int p=0;p<sequence.get(o).get(m).size();p++){
                                                int sameVehicle=sequence.get(o).get(m).get(p);
                                                if(finishTimes[sameVehicle][scheduleRes[sameVehicle]]>maxSameVehicleFinishTime){
                                                    maxSameVehicleFinishTime=finishTimes[sameVehicle][scheduleRes[sameVehicle]];
                                                }
                                            }
                                        }
                                        if(maxSameVehicleFinishTime>actualStartTimes[followupTaskIdx][m]){
                                            actualStartTimes[followupTaskIdx][m]=maxSameVehicleFinishTime;
//                                        System.out.println("followupTaskIdx"+(followupTaskIdx+1)+"actualStartTimes"+maxSameVehicleFinishTime);
                                        }

                                        finishTimes[followupTaskIdx][m]=actualStartTimes[followupTaskIdx][m]+computationTimes[followupTaskIdx][m];
                                        waitTimes[followupTaskIdx][m]=actualStartTimes[followupTaskIdx][m]-earliestStartTimes[followupTaskIdx][m];

                                        prevFinishTime=finishTimes[followupTaskIdx][m];

                                        beginAndEndTimes[followupTaskIdx][1]=finishTimes[followupTaskIdx][m];

                                        // 该车辆必须是可用的
                                        if(!isVehicleAvailable(followupTaskIdx,m,minPrevFinishTimes,actualStartTimes,finishTimes,interval,scheduleRes,finishDistributeTasks)){
                                            flag=-1;
                                        }
                                    }
                                }
                            }

                            double expectedTotalFinishTime=0;
                            for(int k=0;k<K;k++){
                                if(finishTimes[k][scheduleRes[k]]>expectedTotalFinishTime){
                                    expectedTotalFinishTime=finishTimes[k][scheduleRes[k]];
                                }
                            }

//                        System.out.println("expected total finish time: "+expectedTotalFinishTime);

                            // 有多个节点的重分配方案可行
                            double ti=finishTimes[taskIdx][j];
                            if(ti>tx){
                                // 总任务完成时间能够减小，则重分配方案可行
                                if(expectedTotalFinishTime<totalFinishTime){
                                    totalFinishTime=expectedTotalFinishTime;

                                    // 取 min(ti-tx)
                                    if(ti-tx<minDiffMore){
                                        minDiffMore=ti-tx;
                                        minDiffVehicleIdxMore=j;
                                    }
                                    availableNode.add(j);
                                    flag=1;
                                }else{
                                    flag=-1;
                                }
//                            System.out.println("minDiffVehicleIdxMore: "+minDiffVehicleIdxMore);
                            }else{
                                if(tiFlag==1){
                                    if(expectedTotalFinishTime<totalFinishTime){
                                        totalFinishTime=expectedTotalFinishTime;
                                    }
                                    // 取 min(tx-ti)
                                    if(tx-ti<minDiffLess){
                                        minDiffLess=tx-ti;
                                        minDiffVehicleIdxLess=j;
                                    }
                                    availableNode.add(j);
                                    flag=1;
//                                System.out.println("minDiffVehicleIdxLess: "+minDiffVehicleIdxLess);
                                }
                            }
//                        System.out.println();

                            if(flag==1){
                                // 记录分配到此车辆节点的数据
                                tmpScheduleRes[j]=scheduleRes.clone();
                                for (int k = 0; k < K; k++) {
                                    tmpBeginAndEndTimes[j][k]=beginAndEndTimes[k].clone();
                                }
                                for (int k = 0; k < K; k++) {
                                    tmpTransmissionTimes[j][k]=transmissionTimes[k].clone();
                                }
                                for (int k = 0; k < K; k++) {
                                    tmpMinPrevFinishTimes[j][k]=minPrevFinishTimes[k].clone();
                                }
                                for (int k = 0; k < K; k++) {
                                    tmpEarliestStartTimes[j][k]=earliestStartTimes[k].clone();
                                }
                                for (int k = 0; k < K; k++) {
                                    tmpActualStartTimes[j][k]=actualStartTimes[k].clone();
                                }
                                for (int k = 0; k < K; k++) {
                                    tmpWaitTimes[j][k]=waitTimes[k].clone();
                                }
                                for (int k = 0; k < K; k++) {
                                    tmpFinishTimes[j][k]=finishTimes[k].clone();
                                }
                                ArrayList<ArrayList<ArrayList<Integer>>> tmpL=new ArrayList<>();
                                for (int k = 0; k < levelNum; k++) {
                                    ArrayList<ArrayList<Integer>> tmpJ=new ArrayList<>();
                                    for (int l = 0; l < M; l++) {
                                        ArrayList<Integer> tmpK=new ArrayList<>();
                                        for (int m = 0; m < sequence.get(k).get(l).size(); m++) {
                                            tmpK.add(sequence.get(k).get(l).get(m));
                                        }
                                        tmpJ.add(tmpK);
                                    }
                                    tmpL.add(tmpJ);
                                }
                                tmpSequence.set(j,tmpL);
                            }
                            // 还原
                            scheduleRes=scheduleResBefore.clone();
                            for (int k = 0; k < K; k++) {
                                beginAndEndTimes[k]=beginAndEndTimesBefore[k].clone();
                            }
                            for (int k = 0; k < K; k++) {
                                transmissionTimes[k]=transmissionTimesBefore[k].clone();
                            }
                            for (int k = 0; k < K; k++) {
                                minPrevFinishTimes[k]=minPrevFinishTimesBefore[k].clone();
                            }
                            for (int k = 0; k < K; k++) {
                                earliestStartTimes[k]=earliestStartTimesBefore[k].clone();
                            }
                            for (int k = 0; k < K; k++) {
                                actualStartTimes[k]=actualStartTimesBefore[k].clone();
                            }
                            for (int k = 0; k < K; k++) {
                                waitTimes[k]=waitTimesBefore[k].clone();
                            }
                            for (int k = 0; k < K; k++) {
                                finishTimes[k]=finishTimesBefore[k].clone();
                            }
                            sequence=new ArrayList<>();
                            for (int k = 0; k < levelNum; k++) {
                                ArrayList<ArrayList<Integer>> tmpJ=new ArrayList<>();
                                for (int l = 0; l < M; l++) {
                                    ArrayList<Integer> tmpK=new ArrayList<>();
                                    for (int m = 0; m < sequenceBefore.get(k).get(l).size(); m++) {
                                        tmpK.add(sequenceBefore.get(k).get(l).get(m));
                                    }
                                    tmpJ.add(tmpK);
                                }
                                sequence.add(tmpJ);
                            }
                        }

                        int flag=-1;
                        int finalDistributeVehicleIdx=-1;
                        if(minDiffVehicleIdxLess>-1){
                            finalDistributeVehicleIdx=minDiffVehicleIdxLess;
                            flag=1;
                        }else if(minDiffVehicleIdxMore>-1){
                            finalDistributeVehicleIdx=minDiffVehicleIdxMore;
                            flag=1;
                        }

                        if(flag==1){
                            // 更新任务分配
                            scheduleRes=tmpScheduleRes[finalDistributeVehicleIdx].clone();
                            for (int k = 0; k < K; k++) {
                                beginAndEndTimes[k]=tmpBeginAndEndTimes[finalDistributeVehicleIdx][k].clone();
                            }
                            for (int k = 0; k < K; k++) {
                                transmissionTimes[k]=tmpTransmissionTimes[finalDistributeVehicleIdx][k].clone();
                            }
                            for (int k = 0; k < K; k++) {
                                minPrevFinishTimes[k]=tmpMinPrevFinishTimes[finalDistributeVehicleIdx][k].clone();
                            }
                            for (int k = 0; k < K; k++) {
                                earliestStartTimes[k]=tmpEarliestStartTimes[finalDistributeVehicleIdx][k].clone();
                            }
                            for (int k = 0; k < K; k++) {
                                actualStartTimes[k]=tmpActualStartTimes[finalDistributeVehicleIdx][k].clone();
                            }
                            for (int k = 0; k < K; k++) {
                                waitTimes[k]=tmpWaitTimes[finalDistributeVehicleIdx][k].clone();
                            }
                            for (int k = 0; k < K; k++) {
                                finishTimes[k]=tmpFinishTimes[finalDistributeVehicleIdx][k].clone();
                            }
                            sequence=new ArrayList<>();
                            for (int k = 0; k < levelNum; k++) {
                                ArrayList<ArrayList<Integer>> tmpJ=new ArrayList<>();
                                for (int l = 0; l < M; l++) {
                                    ArrayList<Integer> tmpK=new ArrayList<>();
                                    for (int m = 0; m < tmpSequence.get(finalDistributeVehicleIdx).get(k).get(l).size(); m++) {
                                        tmpK.add(tmpSequence.get(finalDistributeVehicleIdx).get(k).get(l).get(m));
                                    }
                                    tmpJ.add(tmpK);
                                }
                                sequence.add(tmpJ);
                            }

                        /*System.out.println("re schedule success!");
                        System.out.println("re schedule task "+(taskIdx+1)+" to vehicle "+(finalDistributeVehicleIdx+1));
                        System.out.println("re schedule result:");*/
                            for(int k=0;k<K;k++){
                            /*System.out.println("task "+(k+1)+" distribute to vehicle "+(scheduleRes[k]+1));
                            System.out.println("from time "+beginAndEndTimes[k][0]+" to "+beginAndEndTimes[k][1]);*/
                            /*cout<<"EST: "<<earliestStartTimes[k][scheduleRes[k]]<<endl;
                            cout<<"Tc: "<<transmissionTimes[k][scheduleRes[k]]<<endl;
                            cout<<"Tp: "<<computationTimes[k][scheduleRes[k]]<<endl;*/
                            /*System.out.println("AST: "+actualStartTimes[k][scheduleRes[k]]);
                            System.out.println();*/
                            }

                            if(totalFinishTime<=D){
                                reScheduleFlag=1;
                            }
                        }

                        // 标记任务为已处理
                        markQueue.add(task);
                        unmarkQueue.remove(maxIdx);

//                    System.out.println("re schedule flag: "+reScheduleFlag);
//                    System.out.println();
                    }
//                System.out.println("re schedule flag: "+reScheduleFlag);
//                System.out.println();
                }
            }

            double totalCost=calculateTotalCost(scheduleRes,computationTimes,vehicles);

//            System.out.println("time based re schedule result:");
            for(int k=0;k<K;k++){
            /*System.out.println("task "+(k+1)+" distribute to vehicle "+(scheduleRes[k]+1));
            System.out.println("from time "+beginAndEndTimes[k][0]+" to "+beginAndEndTimes[k][1]);*/
            /*cout<<"EST: "<<earliestStartTimes[k][scheduleRes[k]]<<endl;
            cout<<"Tc: "<<transmissionTimes[k][scheduleRes[k]]<<endl;
            cout<<"Tp: "<<computationTimes[k][scheduleRes[k]]<<endl;*/
//            System.out.println("AST: "+actualStartTimes[k][scheduleRes[k]]);
//            System.out.println();
            }
            totalFinishTime=finishTimes[K-1][scheduleRes[K-1]];
            /*System.out.println("total finish time: "+totalFinishTime);
            System.out.println("total cost: "+totalCost);
            System.out.println();*/

            // 5.考虑 cost 优化重调度
            // level 内的顺序为负载大小
            // 一个 level 的任务都完成重调度之后，再对后续 level 的任务的完成时间进行更新。
            // update 依据是 cost 变小且重新计算 Tc 后总 time<=D
            // 从完成时间从右到左开始选择其他分配，找到合适解就 update
            // 因此每次 update，cost 都在变小，比目前最优 cost 大的方案就舍弃。
//            System.out.println("cost re schedule:");
            for(int i=0;i<levelNum;i++){
//            System.out.println("level "+(i+1)+":");

                // 找到 ti>tx 的任务，加入未标记队列
                ArrayList<Task> tasksInOneLevel=(ArrayList<Task>)tasksInLevels.get(i).clone();
                ArrayList<Task> unmarkQueue=(ArrayList<Task>)tasksInLevels.get(i).clone();
                ArrayList<Task> markQueue=new ArrayList<>();

                while(unmarkQueue.size()!=0) {
                    // 找到目前负载最大的任务
                    double maxComputation=0;
                    int maxIdx=-1;
                    for(int j=0;j<unmarkQueue.size();j++){
                        if(unmarkQueue.get(j).computation>maxComputation){
                            maxComputation=unmarkQueue.get(j).computation;
                            maxIdx=j;
                        }
                    }

                    Task task=unmarkQueue.get(maxIdx);
                    int taskIdx=task.index-1;
                    int distributeVehicleIdx=scheduleRes[taskIdx];
                    double actualStartTimeBefore=actualStartTimes[taskIdx][distributeVehicleIdx];
//                System.out.println("re schedule task "+(taskIdx+1)+":");

                    int[] scheduleResBefore = scheduleRes.clone();
                    double[][] beginAndEndTimesBefore = new double[K][M];
                    double[][] transmissionTimesBefore = new double[K][M];
                    double[][] minPrevFinishTimesBefore = new double[K][M];
                    double[][] earliestStartTimesBefore = new double[K][M];
                    double[][] actualStartTimesBefore = new double[K][M];
                    double[][] waitTimesBefore = new double[K][M];
                    double[][] finishTimesBefore = new double[K][M];
                    for (int j = 0; j < K; j++) {
                        beginAndEndTimesBefore[j] = beginAndEndTimes[j].clone();
                    }
                    for (int j = 0; j < K; j++) {
                        transmissionTimesBefore[j] = transmissionTimes[j].clone();
                    }
                    for (int j = 0; j < K; j++) {
                        minPrevFinishTimesBefore[j] = minPrevFinishTimes[j].clone();
                    }
                    for (int j = 0; j < K; j++) {
                        earliestStartTimesBefore[j] = earliestStartTimes[j].clone();
                    }
                    for (int j = 0; j < K; j++) {
                        actualStartTimesBefore[j] = actualStartTimes[j].clone();
                    }
                    for (int j = 0; j < K; j++) {
                        waitTimesBefore[j] = waitTimes[j].clone();
                    }
                    for (int j = 0; j < K; j++) {
                        finishTimesBefore[j] = finishTimes[j].clone();
                    }
                    ArrayList<ArrayList<ArrayList<Integer>>> sequenceBefore = new ArrayList<>();
                    for (int j = 0; j < levelNum; j++) {
                        ArrayList<ArrayList<Integer>> tmpJ = new ArrayList<>();
                        for (int k = 0; k < M; k++) {
                            ArrayList<Integer> tmpK = new ArrayList<>();
                            for (int l = 0; l < sequence.get(j).get(k).size(); l++) {
                                tmpK.add(sequence.get(j).get(k).get(l));
                            }
                            tmpJ.add(tmpK);
                        }
                        sequenceBefore.add(tmpJ);
                    }

                    for (int j = 0; j < M; j++) {
                        if (j == distributeVehicleIdx) {
                            continue;
                        }

                        double expectedTotalCost=totalCost-computationTimes[taskIdx][distributeVehicleIdx]*vehicles[distributeVehicleIdx].unitPrice+computationTimes[taskIdx][j]*vehicles[j].unitPrice;
                        if(expectedTotalCost>totalCost){
                            continue;
                        }

                        scheduleRes[taskIdx] = j;
                        int flag = 1;
                        finishDistributeTasks.clear();
                        for (int k = 0; k < i; k++) {
                            for (int l = 0; l < tasksInLevels.get(k).size(); l++) {
//                                System.out.print(tasksInLevels.get(k).get(l).index+" ");
                                finishDistributeTasks.add(tasksInLevels.get(k).get(l).index - 1);
                            }
                        }

                        ArrayList<Integer> tmpList = new ArrayList<>();
                        ArrayList<Integer> waitList = new ArrayList<>();

                        // 前驱任务分配方案变化，需要重新计算 Tc 和 EST
                        double minPrevFinishTime = 100;
                        double earliestStartTime = 0;
                        double transmissionTime = 0;
                        for (int k = 0; k < K; k++) {
                            // 考虑多前驱
                            if (dependency[taskIdx][k] > 0) {
                                int prevVehicleIdx = scheduleRes[k];

                                // 车辆需在 minPrevEndTime 时刻在 VC 范围内
                                double prevFinishTime = finishTimes[k][scheduleRes[k]];
                                if (prevFinishTime < minPrevFinishTime) {
                                    minPrevFinishTime = prevFinishTime;
                                }

                                // 预估传输时间
                                double estimateTransmissionTime = 0;
                                if (prevVehicleIdx != j) {
                                    estimateTransmissionTime = tasks[k].transmission / getTransmissionRate(prevVehicleIdx, j,vehiclalNetworkTopology);
                                }
                                // 需要前驱任务全完成，包含传输完成
                                if (prevFinishTime + estimateTransmissionTime > earliestStartTime) {
                                    earliestStartTime = prevFinishTime + estimateTransmissionTime;
                                    // 取（前驱任务完成+传输）值最大的传输时间
                                    transmissionTime = estimateTransmissionTime;
                                }
                            }
                        }

                        // 任务首节点
                        if (earliestStartTime == 0) {
                            earliestStartTime = inputRSU;
                        }
                        if (minPrevFinishTime == 100) {
                            minPrevFinishTime = inputRSU;
                        }

                        minPrevFinishTimes[taskIdx][j] = minPrevFinishTime;
                        earliestStartTimes[taskIdx][j] = earliestStartTime;
                        transmissionTimes[taskIdx][j] = transmissionTime;

                        beginAndEndTimes[taskIdx][0] = minPrevFinishTimes[taskIdx][j];

//                        System.out.println("MPFT: "+minPrevFinishTimes[taskIdx][j]);
//                        System.out.println("EST: "+earliestStartTimes[taskIdx][j]);
//                        System.out.println("Tc: "+transmissionTimes[taskIdx][j]);

                        // 重分配遇到和 level 内其他任务分配到同一节点的排队情况，根据 EST 大小顺序进行处理
                        tmpList.add(taskIdx);
                        for (int k = 0; k < tasksInOneLevel.size(); k++) {
                            if (tasksInOneLevel.get(k).index - 1 != taskIdx && scheduleRes[tasksInOneLevel.get(k).index - 1] == j) {
                                tmpList.add(tasksInOneLevel.get(k).index - 1);
                            }
                        }

//                        System.out.println();
//                        System.out.println("wait list:");
                        while (tmpList.size() != 0) {
                            double minEST = 100;
                            int minESTidx = -1;
                            for (int k = 0; k < tmpList.size(); k++) {
                                if (earliestStartTimes[tmpList.get(k)][j] < minEST) {
                                    minEST = earliestStartTimes[tmpList.get(k)][j];
                                    minESTidx = k;
                                }
                            }
//                            System.out.println("task "+(tmpList.get(minESTidx)+1)+" ");
//                            System.out.println("EST "+earliestStartTimes[tmpList.get(minESTidx)][j]);

                            waitList.add(tmpList.get(minESTidx));
                            tmpList.remove(minESTidx);
                        }
//                        System.out.println();

                        // waitList 必有一个元素
                        double prevFinishTime = earliestStartTimes[waitList.get(0)][j];

                        // 维护 sequence
                        sequence.get(i).get(j).clear();
                        for (int k = 0; k < waitList.size(); k++) {
                            int waitTaskIdx = waitList.get(k);
//                            System.out.println("task "+(waitTaskIdx+1)+":");

                            // 排队为串行执行
                            actualStartTimes[waitTaskIdx][j] = prevFinishTime;
                            finishTimes[waitTaskIdx][j] = actualStartTimes[waitTaskIdx][j] + computationTimes[waitTaskIdx][j];
                            waitTimes[waitTaskIdx][j] = actualStartTimes[waitTaskIdx][j] - earliestStartTimes[waitTaskIdx][j];

                            prevFinishTime = finishTimes[waitTaskIdx][j];

                            beginAndEndTimes[waitTaskIdx][1] = finishTimes[waitTaskIdx][j];

                            sequence.get(i).get(j).add(waitTaskIdx);

//                            System.out.println("AST "+actualStartTimes[waitTaskIdx][j]);
//                            System.out.println("FT "+finishTimes[waitTaskIdx][j]);
//                            System.out.println("WT "+waitTimes[waitTaskIdx][j]);
//                            System.out.println();
                        }

                        // 更新 AST FT 后再检查可用性
                        for (int k = 0; k < waitList.size(); k++) {
                            int waitTaskIdx = waitList.get(k);
                            // 该车辆必须是可用的
                            if (!isVehicleAvailable(waitTaskIdx, j, minPrevFinishTimes, actualStartTimes, finishTimes, interval, scheduleRes, finishDistributeTasks)) {
                                flag = -1;
                            }
                        }

//                        System.out.println("update sequence task "+(taskIdx+1)+" to vehicle "+(j+1)+":");
                        for (int k = 0; k < sequence.get(i).get(j).size(); k++) {
//                            System.out.print((sequence.get(i).get(j).get(k)+1)+" ");
                        }
//                        System.out.println();
//                        System.out.println();

                        // 更新其他任务
                        // 原分配节点上排队的任务
                        for (int k = 0; k < tasksInOneLevel.size(); k++) {
                            if (scheduleRes[tasksInOneLevel.get(k).index - 1] == distributeVehicleIdx) {
//                                System.out.println("before wait list on vehicle "+(distributeVehicleIdx+1)+":");
                                ArrayList<Integer> sequenceOnBeforeVehicle = (ArrayList<Integer>) sequence.get(i).get(distributeVehicleIdx).clone();
                                // 重分配的任务后的任务向前移动一格
                                for (int l = 0; l < sequenceOnBeforeVehicle.size() - 1; l++) {
                                    if (sequenceOnBeforeVehicle.get(l) == taskIdx) {
                                        prevFinishTime = actualStartTimeBefore;
                                        for (int m = l + 1; m < sequenceOnBeforeVehicle.size(); m++) {
//                                            System.out.println("task "+(sequenceOnBeforeVehicle.get(m)+1)+":");
                                            actualStartTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx] = prevFinishTime;
                                            finishTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx] = actualStartTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx] + computationTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx];
                                            waitTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx] = actualStartTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx] - earliestStartTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx];

                                            prevFinishTime = finishTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx];

                                            beginAndEndTimes[sequenceOnBeforeVehicle.get(m)][1] = finishTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx];

                                            // 该车辆必须是可用的
                                            if (!isVehicleAvailable(sequenceOnBeforeVehicle.get(l), distributeVehicleIdx, minPrevFinishTimes, actualStartTimes, finishTimes, interval, scheduleRes, finishDistributeTasks)) {
                                                flag = -1;
                                            }

//                                            System.out.println("AST: "+actualStartTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx]);
//                                            System.out.println("FT: "+finishTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx]);
//                                            System.out.println("WT: "+waitTimes[sequenceOnBeforeVehicle.get(m)][distributeVehicleIdx]);
                                        }
                                        break;
                                    }
                                }
//                                System.out.println();

                                sequence.get(i).get(distributeVehicleIdx).clear();
                                for (int l = 0; l < sequenceOnBeforeVehicle.size() - 1; l++) {
                                    if (sequenceOnBeforeVehicle.get(l) != taskIdx) {
                                        sequence.get(i).get(distributeVehicleIdx).add(sequenceOnBeforeVehicle.get(l));
                                    }
                                }
//                                System.out.println("before sequence task "+(taskIdx+1)+" to vehicle "+(distributeVehicleIdx+1)+":");
                                for (int l = 0; l < sequence.get(i).get(distributeVehicleIdx).size(); l++) {
//                                    System.out.print((sequence.get(i).get(distributeVehicleIdx).get(l)+1)+" ");
                                }
//                                System.out.println();
                                break;
                            }
                        }

                        tmpList.clear();
                        waitList.clear();

                        // 一个level接一个level进行完成
//                        System.out.println();
//                        System.out.print("finish distribute tasks: ");

//                        System.out.println();

                        // 重分配影响的是：1.同level的前后车辆 2.后续level的后继任务+同车上的可用性
//                        System.out.println();
//                        System.out.println("follow up level:");
                        // 后续 level，按车辆上的排队顺序进行更新
                        for (int l = i + 1; l < levelNum; l++) {
//                            System.out.println("level "+(l+1)+":");
                            for (int m = 0; m < tasksInLevels.get(l-1).size(); m++) {
//                                System.out.print(tasksInLevels.get(k).get(l).index+" ");
                                finishDistributeTasks.add(tasksInLevels.get(l-1).get(m).index - 1);
                            }
                            for (int m = 0; m < M; m++) {
                                if (sequence.get(l).get(m).size() == 0) {
                                    continue;
                                }
//                                System.out.println("vehicle "+(m+1)+":");
                                // 更新 EST
                                for (int n = 0; n < sequence.get(l).get(m).size(); n++) {
                                    int followupTaskIdx = sequence.get(l).get(m).get(n);
//                                    System.out.println("task "+(followupTaskIdx+1)+":");

                                    double followupMinPrevFinishTime = 100;
                                    double followupEarliestStartTime = 0;
                                    double followupTransmissionTime = 0;
                                    for (int k = 0; k < K; k++) {
                                        // 考虑多前驱
                                        if (dependency[followupTaskIdx][k] > 0) {
                                            int prevVehicleIdx = scheduleRes[k];

                                            // 车辆需在 minPrevEndTime 时刻在 VC 范围内
                                            prevFinishTime = finishTimes[k][scheduleRes[k]];
                                            if (prevFinishTime < followupMinPrevFinishTime) {
                                                followupMinPrevFinishTime = prevFinishTime;
                                            }

                                            // 预估传输时间
                                            double estimateTransmissionTime = 0;
                                            if (prevVehicleIdx != m) {
                                                // 前驱任务的传输量
                                                estimateTransmissionTime = tasks[k].transmission / getTransmissionRate(prevVehicleIdx, m,vehiclalNetworkTopology);
                                            }
                                            // 需要前驱任务全完成，包含传输完成
                                            if (prevFinishTime + estimateTransmissionTime > followupEarliestStartTime) {
                                                followupEarliestStartTime = prevFinishTime + estimateTransmissionTime;
                                                // 取（前驱任务完成+传输）值最大的传输时间
                                                followupTransmissionTime = estimateTransmissionTime;
                                            }
                                        }
                                    }
                                    minPrevFinishTimes[followupTaskIdx][m] = followupMinPrevFinishTime;
                                    earliestStartTimes[followupTaskIdx][m] = followupEarliestStartTime;
                                    transmissionTimes[followupTaskIdx][m] = followupTransmissionTime;

                                    beginAndEndTimes[followupTaskIdx][0] = followupMinPrevFinishTime;
                                }

                                prevFinishTime = earliestStartTimes[sequence.get(l).get(m).get(0)][m];
                                for (int n = 0; n < sequence.get(l).get(m).size(); n++) {
                                    int followupTaskIdx = sequence.get(l).get(m).get(n);
                                    actualStartTimes[followupTaskIdx][m] = prevFinishTime;

                                    double maxSameVehicleFinishTime = 0;
                                    // 等待前level内同车的任务完成
                                    for (int o = 0; o < l; o++) {
                                        for (int p = 0; p < sequence.get(o).get(m).size(); p++) {
                                            int sameVehicle = sequence.get(o).get(m).get(p);
                                            if (finishTimes[sameVehicle][scheduleRes[sameVehicle]] > maxSameVehicleFinishTime) {
                                                maxSameVehicleFinishTime = finishTimes[sameVehicle][scheduleRes[sameVehicle]];
                                            }
                                        }
                                    }
                                    if (maxSameVehicleFinishTime > actualStartTimes[followupTaskIdx][m]) {
                                        actualStartTimes[followupTaskIdx][m] = maxSameVehicleFinishTime;
//                                        System.out.println("followupTaskIdx"+(followupTaskIdx+1)+"actualStartTimes"+maxSameVehicleFinishTime);
                                    }

                                    finishTimes[followupTaskIdx][m] = actualStartTimes[followupTaskIdx][m] + computationTimes[followupTaskIdx][m];
                                    waitTimes[followupTaskIdx][m] = actualStartTimes[followupTaskIdx][m] - earliestStartTimes[followupTaskIdx][m];

                                    prevFinishTime = finishTimes[followupTaskIdx][m];

                                    beginAndEndTimes[followupTaskIdx][1] = finishTimes[followupTaskIdx][m];

                                    // 该车辆必须是可用的
                                    if (!isVehicleAvailable(followupTaskIdx, m, minPrevFinishTimes, actualStartTimes, finishTimes, interval, scheduleRes, finishDistributeTasks)) {
                                        flag = -1;
                                    }
                                }
                            }
                        }

                        double expectedTotalFinishTime = 0;
                        for (int k = 0; k < K; k++) {
                            if (finishTimes[k][scheduleRes[k]] > expectedTotalFinishTime) {
                                expectedTotalFinishTime = finishTimes[k][scheduleRes[k]];
                            }
                        }

                        if(expectedTotalFinishTime<=D&&flag==1){
                            totalCost=expectedTotalCost;
                        /*System.out.println("re schedule success!");
                        System.out.println("re schedule "+(taskIdx+1)+" to vehicle "+(j+1)+":");
                        System.out.println("expected total cost: "+expectedTotalCost);
                        System.out.println("expected total finish time: "+expectedTotalFinishTime);*/

                            for (int k = 0; k < K; k++) {
                            /*System.out.println("task " + (k + 1) + " distribute to vehicle " + (scheduleRes[k] + 1));
                            System.out.println("from time " + beginAndEndTimes[k][0] + " to " + beginAndEndTimes[k][1]);*/
                            /*cout<<"EST: "<<earliestStartTimes[k][scheduleRes[k]]<<endl;
                            cout<<"Tc: "<<transmissionTimes[k][scheduleRes[k]]<<endl;
                            cout<<"Tp: "<<computationTimes[k][scheduleRes[k]]<<endl;*/
//                            System.out.println("AST: " + actualStartTimes[k][scheduleRes[k]]);
//                            System.out.println();
                            }
                        }else{
                            // 还原
                            scheduleRes=scheduleResBefore.clone();
                            for (int k = 0; k < K; k++) {
                                beginAndEndTimes[k]=beginAndEndTimesBefore[k].clone();
                            }
                            for (int k = 0; k < K; k++) {
                                transmissionTimes[k]=transmissionTimesBefore[k].clone();
                            }
                            for (int k = 0; k < K; k++) {
                                minPrevFinishTimes[k]=minPrevFinishTimesBefore[k].clone();
                            }
                            for (int k = 0; k < K; k++) {
                                earliestStartTimes[k]=earliestStartTimesBefore[k].clone();
                            }
                            for (int k = 0; k < K; k++) {
                                actualStartTimes[k]=actualStartTimesBefore[k].clone();
                            }
                            for (int k = 0; k < K; k++) {
                                waitTimes[k]=waitTimesBefore[k].clone();
                            }
                            for (int k = 0; k < K; k++) {
                                finishTimes[k]=finishTimesBefore[k].clone();
                            }
                            sequence=new ArrayList<>();
                            for (int k = 0; k < levelNum; k++) {
                                ArrayList<ArrayList<Integer>> tmpJ=new ArrayList<>();
                                for (int l = 0; l < M; l++) {
                                    ArrayList<Integer> tmpK=new ArrayList<>();
                                    for (int m = 0; m < sequenceBefore.get(k).get(l).size(); m++) {
                                        tmpK.add(sequenceBefore.get(k).get(l).get(m));
                                    }
                                    tmpJ.add(tmpK);
                                }
                                sequence.add(tmpJ);
                            }
                        }

                    }


                    // 标记任务为已处理
                    markQueue.add(task);
                    unmarkQueue.remove(maxIdx);
                }
            }
//            System.out.println("cost based re schedule result:");
            /*for(int k=0;k<K;k++){
            *//*System.out.println("task "+(k+1)+" distribute to vehicle "+(scheduleRes[k]+1));
            System.out.println("from time "+beginAndEndTimes[k][0]+" to "+beginAndEndTimes[k][1]);*//*
            *//*cout<<"EST: "<<earliestStartTimes[k][scheduleRes[k]]<<endl;
            cout<<"Tc: "<<transmissionTimes[k][scheduleRes[k]]<<endl;
            cout<<"Tp: "<<computationTimes[k][scheduleRes[k]]<<endl;*//*
//            System.out.println("AST: "+actualStartTimes[k][scheduleRes[k]]);
//            System.out.println();
            }*/
//            System.out.println();

            totalFinishTime=finishTimes[K-1][scheduleRes[K-1]];
            /*System.out.println("total finish time: "+totalFinishTime);
            System.out.println("total cost: "+totalCost);
            System.out.println();*/

            if(totalFinishTime<=D){
                serviceSuccessIdx++;
            }
            averageServiceCost+=totalCost;
        }
        serviceSuccessRate=(double)(serviceSuccessIdx)/experimentRound;
        averageServiceCost=averageServiceCost/experimentRound;
        System.out.println("service success rate: "+serviceSuccessRate);
        System.out.println("average service cost: "+averageServiceCost);
    }
}
