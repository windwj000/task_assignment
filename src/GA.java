import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class GA {
    static final int K=21;
    static final int M=10;
    static final double D=5;
    static final double inputRSU=(double)2/(double)3;
    static final int areaLength=1000;
    static final int areaWidth=20;
    static final double disToTopo=250;
    static final double SPEED=30;
    static final int experimentRound=1;

    // GA 相关
    static final int population=20;
    static final int roundStop=20;

    // 同一车上排队的最大任务数
    static final int LIMITTASK=3;
    // 最大发生排队的车辆数
    static final int LIMITVEHICLE=3;

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
    static ArrayList<ArrayList<Integer>> topologicalSort(Task[] tasks, double[][] dependency){
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

    static int[][] GenRandomSquareMatrix(){
        int[][] matrix = new int[population][K];
        for(int i =0;i<population;i++){
            for(int j =0;j<K;j++){
                matrix[i][j] = GenRandomInt(0,M-1);
            }
        }
        return  matrix;
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

    // 计算总 cost
    static double calculateTotalCost(int[] scheduleRes,double[][] computationTimes,Vehicle[] vehicles){
        double totalCost=0;
        for(int i=0;i<K;i++){
            totalCost+=computationTimes[i][scheduleRes[i]]*vehicles[scheduleRes[i]].unitPrice;
        }
        return totalCost;
    }

    static boolean isSameLevel(int t1,int t2,ArrayList<ArrayList<Task>> tasksInLevels){
        if(getTaskBelongLevel(t1,tasksInLevels)==getTaskBelongLevel(t2,tasksInLevels)){
            return true;
        }
        return false;
    }

    static int getTaskBelongLevel(int t,ArrayList<ArrayList<Task>> tasksInLevels){
        int levelNum = tasksInLevels.size();
        for (int i = 0; i < levelNum; i++) {
            for (int j = 0; j < tasksInLevels.get(i).size(); j++) {
                if(tasksInLevels.get(i).get(j).index-1==t){
                    return i;
                }
            }
        }
        return -1;
    }

    // 计算 fitness
    static double[] getFitness(int[] single,Task[] tasks,Vehicle[] vehicles,double[][] dependency,int[][] vehiclalNetworkTopology,double[][] interval,ArrayList<ArrayList<Task>> tasksInLevels){
        // 不可行的调度方案，返回{-1,-1}
        double[] res=new double[2];
        int levelNum = tasksInLevels.size();

        /*for (int i = 0; i < K; i++) {
            System.out.print(single[i]+" ");
        }
        System.out.println();
        System.out.println();*/

        // 同一level的同一车上产生排队
        // 采取全排列，预估此个体的最优 time&cost
        ArrayList<ArrayList<ArrayList<Integer>>> fullArray=new ArrayList<>();
        for (int i = 0; i < levelNum; i++) {
            ArrayList<ArrayList<Integer>> aa=new ArrayList<>();
            for (int j = 0; j < M; j++) {
                aa.add(new ArrayList<>());
            }
            fullArray.add(aa);
        }

        /*ArrayList<ArrayList<ArrayList<Integer>>> sameVehicle=new ArrayList<>();
        for (int i = 0; i < levelNum; i++) {
            ArrayList<ArrayList<Integer>> aa=new ArrayList<>();
            for (int j = 0; j < M; j++) {
                aa.add(new ArrayList<>());
            }
            sameVehicle.add(aa);
        }*/

        for (int i = 0; i < K; i++) {
            for (int j = 0; j < K&&j!=i; j++) {
                if(isSameLevel(i,j,tasksInLevels)&&single[i]==single[j]){
                    if(!fullArray.get(getTaskBelongLevel(i,tasksInLevels)).get(single[i]).contains(i)){
                        fullArray.get(getTaskBelongLevel(i,tasksInLevels)).get(single[i]).add(i);
                    }
                    if(!fullArray.get(getTaskBelongLevel(i,tasksInLevels)).get(single[i]).contains(j)){
                        fullArray.get(getTaskBelongLevel(i,tasksInLevels)).get(single[i]).add(j);
                    }
                }
            }
        }
        /*System.out.println("same vehicle:");
        for (int i = 0; i < levelNum; i++) {
            for (int j = 0; j < M; j++) {
                if(sameVehicle.get(i).get(j).size()==0){
                    continue;
                }
                for (int k = 0; k < sameVehicle.get(i).get(j).size(); k++) {
                    System.out.print(sameVehicle.get(i).get(j).get(k)+" ");
                }
                System.out.println();
            }
        }*/
        for (int i = 0; i < levelNum; i++) {
            for (int j = 0; j < M; j++) {
                for (int k = 0; k < K; k++) {
                    if(single[k]==j&&getTaskBelongLevel(k,tasksInLevels)==i&&!fullArray.get(i).get(j).contains(k)){
                        fullArray.get(i).get(j).add(k);
                    }
                }

            }
        }

        /*System.out.println("full array:");
        for (int i = 0; i < levelNum; i++) {
            for (int j = 0; j < M; j++) {
                if(fullArray.get(i).get(j).size()==0){
                    continue;
                }
                for (int k = 0; k < fullArray.get(i).get(j).size(); k++) {
                    System.out.print(fullArray.get(i).get(j).get(k)+" ");
                }
                System.out.println();
            }
        }*/

        // 根据FCFS计算任务执行序列
        ArrayList<ArrayList<ArrayList<Integer>>> sequence=new ArrayList<>();
        for (int i = 0; i < levelNum; i++) {
            ArrayList<ArrayList<Integer>> aa=new ArrayList<>();
            for (int j = 0; j < M; j++) {
                aa.add(new ArrayList<>());
            }
            sequence.add(aa);
        }

        int[] scheduleRes=new int[K];
        for (int i = 0; i < K; i++) {
            scheduleRes[i]=single[i];
        }
        double[][] beginAndEndTimes = new double[K][M];
        double[][] transmissionTimes = new double[K][M];
        double[][] computationTimes = new double[K][M];
        double[][] minPrevFinishTimes = new double[K][M];
        double[][] earliestStartTimes = new double[K][M];
        double[][] actualStartTimes = new double[K][M];
        double[][] waitTimes = new double[K][M];
        double[][] finishTimes = new double[K][M];
        ArrayList<Integer> finishDistributeTasks = new ArrayList<>();

        // 根据任务到车辆的映射，以及车辆上任务的执行序列，计算应用完成时间和总服务成本
        for(int i=0;i<levelNum;i++){
//            System.out.println("level "+(i+1)+":");
            for (int j = 0; j < M; j++) {
//                System.out.println("vehicle "+(j+1)+":");
                for (int l = 0; l < fullArray.get(i).get(j).size(); l++) {
                    int taskIdx=fullArray.get(i).get(j).get(l);
//                    System.out.println("task "+(taskIdx+1)+":");
                    computationTimes[taskIdx][j]=tasks[taskIdx].computation / vehicles[j].capability;
//                    System.out.println("computation time: "+computationTimes[taskIdx][j]);
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
                    /*System.out.println("minPrevFinishTimes "+minPrevFinishTimes[taskIdx][j]);
                    System.out.println("earliestStartTimes "+earliestStartTimes[taskIdx][j]);
                    System.out.println("transmissionTimes "+transmissionTimes[taskIdx][j]);
                    System.out.println("beginAndEndTimes "+beginAndEndTimes[taskIdx][0]);*/
                }
                // FCFS
                int len=fullArray.get(i).get(j).size();
                boolean[] visited=new boolean[len];
                for (int l = 0; l < len; l++){
                    double minStartTime=100;
                    int minStartTimeIdx=-1;
                    for (int k = 0; k < len; k++) {
                        if(!visited[k]){
                            if(earliestStartTimes[fullArray.get(i).get(j).get(k)][j]<minStartTime){
                                minStartTime=earliestStartTimes[fullArray.get(i).get(j).get(k)][j];
                                minStartTimeIdx=k;
                            }
                        }
                    }
                    if(minStartTimeIdx!=-1){
                        sequence.get(i).get(j).add(fullArray.get(i).get(j).get(minStartTimeIdx));
                        visited[minStartTimeIdx]=true;
                    }
                }

                for (int l = 0; l < sequence.get(i).get(j).size(); l++) {
                    int taskIdx=sequence.get(i).get(j).get(l);
                    if(l==0){
                        actualStartTimes[taskIdx][j]=earliestStartTimes[taskIdx][j];
                        finishTimes[taskIdx][j]=actualStartTimes[taskIdx][j]+computationTimes[taskIdx][j];
                        beginAndEndTimes[taskIdx][j]=finishTimes[taskIdx][j];
                    }
                    // 发生排队
                    else{
                        actualStartTimes[taskIdx][j]=finishTimes[sequence.get(i).get(j).get(l-1)][j];
                        finishTimes[taskIdx][j]=actualStartTimes[taskIdx][j]+computationTimes[taskIdx][j];
                        waitTimes[taskIdx][j]=actualStartTimes[taskIdx][j]-earliestStartTimes[taskIdx][j];
                        beginAndEndTimes[taskIdx][j]=finishTimes[taskIdx][j];
                    }
                    /*System.out.println("wait situation:");
                    System.out.println("actualStartTimes "+actualStartTimes[taskIdx][j]);
                    System.out.println("finishTimes "+finishTimes[taskIdx][j]);
                    System.out.println("waitTimes "+waitTimes[taskIdx][j]);
                    System.out.println("beginAndEndTimes "+beginAndEndTimes[taskIdx][j]);*/
                    if (!isVehicleAvailable(taskIdx, j, minPrevFinishTimes, actualStartTimes, finishTimes, interval, scheduleRes, finishDistributeTasks)) {
                        return new double[]{-1,-1};
                    }
                    finishDistributeTasks.add(taskIdx);
                }
            }
        }
        double totalFinishTime=finishTimes[K-1][scheduleRes[K-1]];
        double totalCost=calculateTotalCost(scheduleRes,computationTimes,vehicles);;
        /*System.out.println("total finish time: "+totalFinishTime);
        System.out.println("total cost: "+totalCost);*/
        res[0]=totalFinishTime;
        res[1]=totalCost;

        return res;

        /*ArrayList<ArrayList<Integer>> sequences=new ArrayList<>();
        for (int i = 0; i < levelNum; i++) {
            for (int j = 0; j < M; j++) {
                if(fullArray.get(i).get(j).size()>LIMITTASK){
                    return res;
                }
                if(fullArray.get(i).get(j).size()!=0){
                    ArrayList<Integer> tmpSequences=new ArrayList<>();
                    for (int k = 0; k < fullArray.get(i).get(j).size(); k++) {
                        tmpSequences.add(fullArray.get(i).get(j).get(k));
                    }
                    sequences.add(tmpSequences);
                }
            }
        }

        int sequencesNum=sequences.size();
        if(sequencesNum>LIMITVEHICLE){
            return res;
        }*/

        /*for (int i = 0; i < sequencesNum; i++) {
            for (int j = 0; j < sequences.get(i).size(); j++) {
                ArrayList<ArrayList<ArrayList<Integer>>> sequence=new ArrayList<>();
                for (int k = 0; k < levelNum; k++) {
                    ArrayList<ArrayList<Integer>> aa=new ArrayList<>();
                    for (int l = 0; l < M; l++) {
                        ArrayList<Integer> a=new ArrayList<>();
                        for (int m = 0; m < fullArray.get(k).get(l).size(); m++) {
                            a.add(fullArray.get(k).get(l).get(m));
                        }
                        aa.add(a);
                    }
                    sequence.add(aa);
                }
                sequence.get(i).get(j).set();
            }
        }*/
    }

    // 获取一个车辆上任务的全排列
    /*static void getFullArray(ArrayList<ArrayList<ArrayList<Integer>>> res){
        if(){
            res.add();
            return;

        }
    }*/

    // 遍历所有产生排队的车辆，得到所有可能的分配
    /*static void getAllFullArray(ArrayList<ArrayList<ArrayList<Integer>>> res){
        if(){
            res.add();
            return;

        }
    }*/

    // 计算一个排列下的 time&cost
    static double[] getTimeAndCost(int[] single,Task[] tasks,Vehicle[] vehicles,double[][] dependency,int[][] vehiclalNetworkTopology,double[][] interval,ArrayList<ArrayList<Task>> tasksInLevels,ArrayList<ArrayList<ArrayList<Integer>>> sequence){
        // 不可行的调度方案，返回{-1,-1}
        double[] res=new double[2];

        int[] scheduleRes=new int[K];
        for (int i = 0; i < K; i++) {
            scheduleRes[i]=single[i];
        }
        double[][] beginAndEndTimes = new double[K][M];
        double[][] transmissionTimes = new double[K][M];
        double[][] computationTimes = new double[K][M];
        double[][] minPrevFinishTimes = new double[K][M];
        double[][] earliestStartTimes = new double[K][M];
        double[][] actualStartTimes = new double[K][M];
        double[][] waitTimes = new double[K][M];
        double[][] finishTimes = new double[K][M];
        ArrayList<Integer> finishDistributeTasks = new ArrayList<>();

        int levelNum = tasksInLevels.size();
        // 根据任务到车辆的映射，以及车辆上任务的执行序列，计算应用完成时间和总服务成本
        for(int i=0;i<levelNum;i++){
            System.out.println("level "+(i+1)+":");
            for (int j = 0; j < M; j++) {
                System.out.println("vehicle "+(j+1)+":");
                for (int l = 0; l < sequence.get(i).get(j).size(); l++) {
                    int taskIdx=sequence.get(i).get(j).get(l);
                    System.out.println("task "+(taskIdx+1)+":");
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

                    computationTimes[taskIdx][j]=tasks[taskIdx].computation / vehicles[j].capability;
                    if(l==0){
                        actualStartTimes[taskIdx][j]=earliestStartTimes[taskIdx][j];
                        finishTimes[taskIdx][j]=actualStartTimes[taskIdx][j]+computationTimes[taskIdx][j];
                        beginAndEndTimes[taskIdx][j]=finishTimes[taskIdx][j];
                    }
                    // 发生排队
                    else{
                        actualStartTimes[taskIdx][j]=finishTimes[sequence.get(i).get(j).get(l-1)][j];
                        finishTimes[taskIdx][j]=actualStartTimes[taskIdx][j]+computationTimes[taskIdx][j];
                        waitTimes[taskIdx][j]=actualStartTimes[taskIdx][j]-earliestStartTimes[taskIdx][j];
                        beginAndEndTimes[taskIdx][j]=finishTimes[taskIdx][j];
                    }
                    if (!isVehicleAvailable(taskIdx, j, minPrevFinishTimes, actualStartTimes, finishTimes, interval, scheduleRes, finishDistributeTasks)) {
                        return new double[]{-1,-1};
                    }
                    finishDistributeTasks.add(taskIdx);
                }
            }
        }
        double totalFinishTime=finishTimes[K-1][scheduleRes[K-1]];
        double totalCost=calculateTotalCost(scheduleRes,computationTimes,vehicles);;
        System.out.println("total finish time: "+totalFinishTime);
        System.out.println("total cost: "+totalCost);
        res[0]=totalFinishTime;
        res[1]=totalCost;

        return res;
    }

    // 判断车辆节点是否可用
    // 1. 车辆在 VC 内
    // 2. 该车不能有其他任务正在处理
    // 3. 符合编码要求
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

    static void mutation(int single[]){
        int idx1=GenRandomInt(0,K-1);
        int idx2=GenRandomInt(0,M-1);
        single[idx1]=idx2;
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

//        Task[] tasks = new Task[]{t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14};
        Task[] tasks = new Task[]{t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21};

        // 实验变量-负载成倍增加
        for (int i = 0; i < K; i++) {
            double curComputation=tasks[i].getComputation();
            double curTransmission=tasks[i].getTransmission();
            tasks[i].setComputation(curComputation);
            tasks[i].setTransmission(curTransmission);
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

//        Vehicle[] vehicles = new Vehicle[]{v1, v2, v3, v4, v5, v6};
        Vehicle[] vehicles = new Vehicle[]{v1, v2, v3, v4, v5, v6, v7, v8, v9, v10};

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

        /*double[][] dependency=
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
                //1 2 3 4 5 6 7 8 9 1011121314*/

        double serviceSuccessRate=0;
        int serviceSuccessIdx=0;
        double averageServiceCost=0;
        for (int z = 0; z < experimentRound; z++) {

            // 每次实验的车辆初始位置变化导致：
            // 1.车辆间传输速率变化
            // 2.车辆停留在VC的时间有效时间变化
            double[][] carPosition=GenRandomCarPosition();
            int[][] vehiclalNetworkTopology = calculateTransmissionRate(carPosition);
            double[][] interval = calculateInterval(carPosition);

    //        int[][] vehiclalNetworkTopology=GenRandomSquareMatrix(M, 1, 4);;
            /*int[][] vehiclalNetworkTopology=
                    {{0,1,2,2,3,2},
                    {1,0,3,2,2,1},
                    {2,3,0,2,3,2},
                    {2,2,2,0,1,3},
                    {3,2,3,1,0,2},
                    {2,1,2,3,3,0}};*/

    //        double[][] interval =GenRandomCarInterval(M, 0, 1, 5, 6);
            /*double[][] interval =
                    {{0, 20},
                    {0, 20},
                    {0, 20},
                    {0, 20},
                    {0, 20},
                    {0, 20}};*/

            // 拓扑排序对 DAG 划分多个 level
            ArrayList<ArrayList<Integer>> levelRes = topologicalSort(tasks, dependency);
            ArrayList<ArrayList<Task>> tasksInLevels = new ArrayList<>();
            for (int i = 0; i < levelRes.size(); i++) {
                ArrayList<Task> tasksInOneLevel = new ArrayList<>();
                for (int j = 0; j < levelRes.get(i).size(); j++) {
                    tasksInOneLevel.add(tasks[levelRes.get(i).get(j)]);
                }
                tasksInLevels.add(tasksInOneLevel);
            }vehiclalNetworkTopology=GenRandomSquareMatrix(M, 1, 4);

            for (int i = 0; i < tasksInLevels.size(); i++) {
                for (int j = 0; j < tasksInLevels.get(i).size(); j++) {
                    System.out.print(tasksInLevels.get(i).get(j).index + " ");
                }
                System.out.println();
            }interval =GenRandomCarInterval(M, 0, 1, 99, 100);
            System.out.println();

            // 第二部分：算法
            // 编码，任务到车辆的映射
//            int[] thetaValue={1,2,3,4,5,6};

            // 随机初始种群
            int[][] initialPopulation=GenRandomSquareMatrix();
            /*for (int i = 0; i < population; i++) {
                for (int j = 0; j < K; j++) {
                    System.out.print(initialPopulation[i][j]+" ");
                }
                System.out.println();
            }
            System.out.println();*/

            // 100轮
            for (int i = 0; i < roundStop; i++) {
                System.out.println("round "+(i+1)+":");

                // 每轮新生成的个体
                int[][] newPopulation=new int[population][K];
                double[][] fitness=new double[population*2][2];
                boolean[] visited=new boolean[population];
                int idx=0;
                for (int j = 0; j < population/2; j++) {
    //                System.out.println("population "+(j+1)+":");
                    int pMale;
                    int pFemale;
                    while(true){
                        pMale=GenRandomInt(0,population-1);
                        pFemale=GenRandomInt(0,population-1);
                        if(!visited[pMale]&&!visited[pFemale]){
                            break;
                        }
                    }

                    int[] aMale=new int[K];
                    int[] aFemale=new int[K];
                    for (int k = 0; k < K; k++) {
                        aMale[k]=initialPopulation[pMale][k];
                    }
                    for (int k = 0; k < K; k++) {
                        aFemale[k]=initialPopulation[pFemale][k];
                    }

                    /*System.out.println("aMale:");
                    for (int k = 0; k < K; k++) {
                        System.out.print(aMale[k]+" ");
                    }
                    System.out.println();
                    System.out.println("aFemale:");
                    for (int k = 0; k < K; k++) {
                        System.out.print(aFemale[k]+" ");
                    }
                    System.out.println();
                    System.out.println();*/

                    // crossover
                    // 交换一个任务的映射
                    // 不可行就重新随机
                    while(true){
                        int crossoverMale=GenRandomInt(0,K-1);
                        int crossoverFemale=GenRandomInt(0,K-1);
                        int tmpCrossover=aMale[crossoverMale];
                        aMale[crossoverMale]=aFemale[crossoverFemale];
                        aFemale[crossoverFemale]=tmpCrossover;

                        /*System.out.println("after crossover:");
                        System.out.println("aMale:");
                        for (int k = 0; k < K; k++) {
                            System.out.print(aMale[k]+" ");
                        }
                        System.out.println();
                        System.out.println("aFemale:");
                        for (int k = 0; k < K; k++) {
                            System.out.print(aFemale[k]+" ");
                        }
                        System.out.println();
                        System.out.println();*/

                        // mutation
                        mutation(aMale);
                        mutation(aFemale);

                        /*System.out.println("after mutation:");
                        System.out.println("aMale:");
                        for (int k = 0; k < K; k++) {
                            System.out.print(aMale[k]+" ");
                        }
                        System.out.println();
                        System.out.println("aFemale:");
                        for (int k = 0; k < K; k++) {
                            System.out.print(aFemale[k]+" ");
                        }
                        System.out.println();
                        System.out.println();*/

                        // 遗传操作后可行性检查
                        double[] resMale=getFitness(aMale,tasks,vehicles,dependency,vehiclalNetworkTopology,interval,tasksInLevels);
                        double[] resFemale=getFitness(aFemale,tasks,vehicles,dependency,vehiclalNetworkTopology,interval,tasksInLevels);
                        /*System.out.println(resMale[0]+" "+resMale[1]);
                        System.out.println(resFemale[0]+" "+resFemale[1]);
                        System.out.println();*/
                        if(!(resMale[0]==-1||resFemale[0]==-1)){
                            break;
                        }
                    }
                    double[] resMale=getFitness(aMale,tasks,vehicles,dependency,vehiclalNetworkTopology,interval,tasksInLevels);
                    double[] resFemale=getFitness(aFemale,tasks,vehicles,dependency,vehiclalNetworkTopology,interval,tasksInLevels);
                    /*System.out.println("after crossover and mutation:");
                    System.out.println("aMale:");
                    for (int k = 0; k < K; k++) {
                        System.out.print(aMale[k]+" ");
                    }
                    System.out.println();
                    System.out.println("finish time: "+resMale[0]);
                    System.out.println("cost: "+resMale[1]);
                    System.out.println();

                    System.out.println("aFemale:");
                    for (int k = 0; k < K; k++) {
                        System.out.print(aFemale[k]+" ");
                    }
                    System.out.println();
                    System.out.println("finish time: "+resFemale[0]);
                    System.out.println("cost: "+resFemale[1]);
                    System.out.println();*/

                    newPopulation[idx++]=aMale;
                    newPopulation[idx++]=aFemale;

                    boolean flag=false;
                    for (int k = 0; k < population; k++) {
                        if(!visited[k]){
                            flag=true;
                        }
                    }
                    if(!flag){
                        break;
                    }
                }
                // 每一轮的所有个体
                int[][] totalPopulation=new int[population*2][K];
                for (int j = 0; j < population*2; j++) {
                    if(j<population){
                        totalPopulation[j]=initialPopulation[j];
                    }else{
                        totalPopulation[j]=newPopulation[j-population];
                    }
                }

                for (int k = 0; k < population*2; k++) {
                    fitness[k] = getFitness(totalPopulation[k],tasks,vehicles,dependency,vehiclalNetworkTopology,interval,tasksInLevels);
                }
                // 按适应度降序排序
                boolean[] visitedFitness = new boolean[population*2];
                int idxFitness=0;
                while(true){
                    double minTotalCost=10000;
                    int minTotalCostIdx=-1;
                    for (int k = 0; k < population*2; k++) {
                        if(!visitedFitness[k]){
                            // 首先满足deadline，再取cost最小
                            if(fitness[k][0]!=-1&&fitness[k][1]!=-1&&fitness[k][0]<=D){
                                if(fitness[k][1]<minTotalCost){
                                    minTotalCost=fitness[k][1];
                                    minTotalCostIdx=k;
                                }
                            }
                        }
                    }
                    // 按cost排序
                    if(minTotalCostIdx==-1){
                        for (int k = 0; k < population*2; k++) {
                            if(!visitedFitness[k]){
                                if(fitness[k][0]!=-1&&fitness[k][1]!=-1&&fitness[k][1]<minTotalCost){
                                    minTotalCost=fitness[k][1];
                                    minTotalCostIdx=k;
                                }
                            }
                        }
                    }
                    initialPopulation[idxFitness++]=totalPopulation[minTotalCostIdx];
                    visitedFitness[minTotalCostIdx]=true;

                    /*System.out.println("minTotalCostIdx: "+minTotalCostIdx);
                    System.out.println("finish time: "+fitness[minTotalCostIdx][0]);
                    System.out.println("cost: "+fitness[minTotalCostIdx][1]);*/

                    if(idxFitness==population){
                        break;
                    }
                }

                // 输出种群
                /*System.out.println("next generation:");
                for (int k = 0; k < population; k++) {
                    double[] res=getFitness(initialPopulation[k],tasks,vehicles,dependency,vehiclalNetworkTopology,interval,tasksInLevels);
                    System.out.println("finish time: "+res[0]);
                    System.out.println("cost: "+res[1]);
                }
                System.out.println();*/

                // 每一轮的最佳值
                double[] res=getFitness(initialPopulation[0],tasks,vehicles,dependency,vehiclalNetworkTopology,interval,tasksInLevels);
                System.out.println("elite:");
                System.out.println("finish time: "+res[0]);
                System.out.println("cost: "+res[1]);
                System.out.println();
            }

            // 输出最终结果
            System.out.println("final result:");
            for (int i = 0; i < K; i++) {
                System.out.print(initialPopulation[0][i]+" ");
            }
            System.out.println();
            System.out.println();

            double[] res=getFitness(initialPopulation[0],tasks,vehicles,dependency,vehiclalNetworkTopology,interval,tasksInLevels);
            System.out.println("total finish time: "+res[0]);
            System.out.println("total cost: "+res[1]);
            System.out.println();
            if(res[0]<=D){
                serviceSuccessIdx++;
            }
            averageServiceCost+=res[1];
        }
        serviceSuccessRate=(double)(serviceSuccessIdx/experimentRound);
        averageServiceCost=averageServiceCost/experimentRound;
        System.out.println("service success rate: "+serviceSuccessRate);
        System.out.println("average service cost: "+averageServiceCost);
    }
}
