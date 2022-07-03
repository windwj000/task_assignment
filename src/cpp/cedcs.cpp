#include <iostream>
#include <vector>
#include <ctime>
#include <cstdlib>
#include <cmath>
#include <queue>
#include "string.h"

        using namespace std;

// 车辆数
        #define M 6

// 任务数
//#define K 5
        #define K 14

// 不可用 state
        #define UNAVAILABLESTATE 100


class Task{
    public:
    int index;    // 序号
    double computation;    // 计算负载
    double transmission;    // 传输负载
    //    Task next[];    // 后继任务
//    int indegree;    // 入度
    Task(int index, double computation, double transmission) : index(index), computation(computation), transmission(transmission) {}
};

class Vehicle{
    public:
    int index;    // 序号
    double capability;    // 处理能力
    double unitPrice;    // 服务成本

    Vehicle(int index, double capability, double unitPrice) : index(index), capability(capability), unitPrice(unitPrice) {}
};

// 拓扑排序得到多个 level
vector<vector<int>> topologicalSort(Task tasks[K],double dependency[K][K]){
        int indegree[K]={0};
        for(int i=0;i<K;i++){
        for(int j=0;j<K;j++){
        if(dependency[i][j]!=0){
        indegree[i]++;
        }
        }
        }

        vector<vector<int>> res;
        queue<int> q;
        for(int i=0;i<K;i++){
        if(indegree[i]==0){
        q.push(i);
        }
        }
        vector<int> level;
        while(!q.empty()){
        int v=q.front();
        q.pop();
        indegree[v]--;
        level.push_back(v);
        // 遍历邻接矩阵，找到后继任务，减入度
        for(int i=0;i<K;i++){
        if(dependency[i][v]!=0){
        indegree[i]--;
        }
        }
        if(q.empty()){
        res.push_back(level);
        level.clear();
        for(int i=0;i<K;i++){
        if(indegree[i]==0){
        q.push(i);
        }
        }
        }
        }

        return res;
        }

        double getTransmissionRateFromHop(int hop){
        double bandwidth[6]={0,10,4.8,3.2,2.4,1.92};
        return bandwidth[hop];
        }

// 获取两车间的传输速率
        double getTransmissionRate(int v1,int v2){
        int vehiclalNetworkTopology[M][M]={{0,1,2,2,3,2},
        {1,0,3,2,2,1},
        {2,3,0,2,3,2},
        {2,2,2,0,1,3},
        {3,2,3,1,0,2},
        {2,1,2,3,3,0}};
        int hop=vehiclalNetworkTopology[v1][v2];
        return getTransmissionRateFromHop(hop);
        }

// 判断车辆节点是否可用
// 1. 车辆在 VC 内
// 2. 该车不能有其他任务正在处理
        bool isVehicleAvailable(int taskIdx,int j,double minPrevFinishTime[K][M],double actualStartTimes[K][M],double finishTimes[K][M],double interval[M][2],int scheduleRes[K],vector<int> finishDistributeTasks){
        // 开始传输-处理完成 这段时间内，车辆在 VC 内
        if(minPrevFinishTime[taskIdx][j]<interval[j][0]||finishTimes[taskIdx][j]>interval[j][1]){
        return false;
        }
        for(int i=0;i<K;i++){
        // 任务处理开始和完成时间内，其他任务不能正在处理
        if(scheduleRes[i]==j&&i!=taskIdx){
        for(int k=0;k<finishDistributeTasks.size();k++){
        if(i==finishDistributeTasks[k]){
        if(!(finishTimes[i][j]<=actualStartTimes[taskIdx][j]||actualStartTimes[i][j]>=finishTimes[taskIdx][j])){
        return false;
        }
        }
        }
        }
        }

        return true;
        }

/*bool isVehicleAvailableAll(int taskIdx,int j,double minPrevFinishTime[K][M],double actualStartTimes[K][M],double finishTimes[K][M],double interval[M][2],int scheduleRes[K]){
    // 开始传输-处理完成 这段时间内，车辆在 VC 内
    if(minPrevFinishTime[taskIdx][j]<interval[j][0]||finishTimes[taskIdx][j]>interval[j][1]){
        return false;
    }
    for(int i=0;i<K;i++){
        // 任务处理开始和完成时间内，其他任务不能正在处理
        if(scheduleRes[i]==j&&i!=taskIdx){
            if(!(finishTimes[i][j]<=actualStartTimes[taskIdx][j]||actualStartTimes[i][j]>=finishTimes[taskIdx][j])){
                return false;
            }
        }
    }

    return true;
}*/

// 计算 max & min 传输速率
        vector<double> findMaxAndMinTransmissionRate(Vehicle vehicles[M],double interval[M][2]){
        vector<double> res;
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
        res.push_back(max);
        res.push_back(min);
        return res;
        }

// 划分 sub-deadline
        vector<vector<double>> divideSubDeadline(vector<vector<Task>> taskLevel,double inputTransmissionTimeRSU,double ave,double deadline){
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
    /*cout<<"transmissionTime: "<<endl;
//    double initialTransmissionTime=transmissionSum/ave;
    cout<<initialTransmissionTime<<endl;*/
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
        }

// 计算优化分配后的总 time
/*double calculateTotalTime(vector<vector<Task>> taskLevel,int scheduleRes[K],double beginAndEndTimes[K][2]){
    for(int i=0;i<taskLevel.size();i++){
        vector<Task> tasksInLevel=taskLevel[i];
        // level 内的 tasks 存在排队的情况
    }
}*/

// 计算优化分配后的总 cost
        double calculateTotalCost(int scheduleRes[K],double computationTimes[K][M],Vehicle vehicles[M]){
        double totalCost=0;
        for(int i=0;i<K;i++){
        totalCost+=computationTimes[i][scheduleRes[i]]*vehicles[scheduleRes[i]].unitPrice;
        }
        return totalCost;
        }

        int main(){
        // 第一部分：输入任务与车辆数据
        // 任务
        Task t1(1,3,3);
        Task t2(2,2,2);
        Task t3(3,3,3);
        Task t4(4,4,4);
        Task t5(5,3,3);
        Task t6(6,1,1);
        Task t7(7,4,4);
        Task t8(8,2,2);
        Task t9(9,5,5);
        Task t10(10,3,3);
        Task t11(11,3,3);
        Task t12(12,4,4);
        Task t13(13,3,3);
        Task t14(14,4,4);

        Task tasks[K]={t1,t2,t3,t4,t5,t6,t7,t8,t9,t10,t11,t12,t13,t14};

        // 车辆
        Vehicle v1(1,5,30.8);
        Vehicle v2(2,6,44);
        Vehicle v3(3,7,59.6);
        Vehicle v4(4,8,77.6);
        Vehicle v5(5,9,98);
        Vehicle v6(6,10,120.8);

        Vehicle vehicles[M]={v1,v2,v3,v4,v5,v6};

        // 计价
        // cost=capability￿￿^2*1.2+0.8

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
        double dependency[K][K]={{0,0,0,0,0,0,0,0,0,0,0,0,0,0},  //1
        {3,0,0,0,0,0,0,0,0,0,0,0,0,0},  //2
        {3,0,0,0,0,0,0,0,0,0,0,0,0,0},  //3
        {3,0,0,0,0,0,0,0,0,0,0,0,0,0},  //4
        {3,0,0,0,0,0,0,0,0,0,0,0,0,0},  //5
        {0,0,3,0,0,0,0,0,0,0,0,0,0,0},  //6
        {0,0,3,0,0,0,0,0,0,0,0,0,0,0},  //7
        {0,0,3,0,0,0,0,0,0,0,0,0,0,0},  //8
        {0,0,0,4,0,0,0,0,0,0,0,0,0,0},  //9
        {0,0,0,4,3,0,0,0,0,0,0,0,0,0},  //10
        {0,2,0,0,0,1,4,2,0,0,0,0,0,0},  //11
        {0,0,0,0,0,0,4,0,0,0,0,0,0,0},  //12
        {0,0,0,0,0,0,4,0,0,0,0,0,0,0},  //13
        {0,0,0,0,0,0,0,0,0,3,3,0,3,0}}; //14
        //1 2 3 4 5 6 7 8 9 1011121314

        // 车辆停留在 VC 的时间
        double interval[M][2]={{ 0.0,20.0},
        { 0.0,20.0},
        { 0.0,20.0},
        { 0.0,20.0},
        { 0.0,20.0},
        { 0.0,20.0}};

    /*{{ 1.0,13.0},
     { 6.0,16.0},
     { 0.0,10.0},
     {11.0,20.0}};*/

        // 截止时间
        double deadline=4.5;

        // RSU 对初始任务的输入，根据道路情况变化的传输速率
        double inputTransmissionRSU=2;
        double roadOneRate=1;
        double roadTwoRate=2;
        double roadThreeRate=3;

        double inputTransmissionTimeRSU=inputTransmissionRSU/roadThreeRate;

        // 第二部分：算法
        // 1.拓扑排序对 DAG 划分多个 level
        vector<vector<int>> res=topologicalSort(tasks,dependency);
        int levelNum=res.size();
        vector<vector<Task>> taskLevel;
        for(int i=0;i<levelNum;i++){
        vector<Task> tmp;
        for(int j=0;j<res[i].size();j++){
        tmp.push_back(tasks[res[i][j]]);
        }
        taskLevel.push_back(tmp);
        tmp.clear();
        }

        cout<<"level division: "<<endl;
        for(int i=0;i<levelNum;i++){
        cout<<"level "<<i+1<<": ";
        for(int j=0;j<taskLevel[i].size();j++){
        cout<<taskLevel[i][j].index<<" ";
        }
        cout<<endl;
        }
        cout<<endl;

        // 2.使用 max-min 算法进行初分配
        // 调度结果，每个任务开始结束时刻，任务序列
        int scheduleRes[K];
        double beginAndEndTimes[K][2];
        double transmissionTimes[K][M];
        double computationTimes[K][M];
        double minPrevFinishTimes[K][M];
        double earliestStartTimes[K][M];
        double actualStartTimes[K][M];
        double waitTimes[K][M];
        for(int i=0;i<K;i++){
        for(int j=0;j<M;j++){
        waitTimes[i][j]=0;
        }
        }
        double finishTimes[K][M];
        double costs[K][M];

        double states[K][M];
        int numbers[K];
        vector<int> finishDistributeTasks;
        double max=0;
        double min=100;
        int levelIdx=0;

        // 有了 RSU 的输入，t0 也用 max-min 算法分配
        // 通过 max-min 图计算可用时间=传输时间+执行时间，注意这两个时间次序不能变
        cout<<"max-min: "<<endl;
        while(levelIdx!=levelNum){
        cout<<"level "<<levelIdx+1<<": "<<endl;
        vector<Task> curTasks=taskLevel[levelIdx++];
        vector<Task> tmpTasks=curTasks;
        bool flag=true;
        int maxTask=-1;
        int deletedTask=-1;
        while(tmpTasks.size()!=0){
        // 计算 max-min 图
        max=0;
        // 第一次分配
        if(flag==true){
        cout<<"first distribute: "<<endl;
        flag=false;
        for(int i=0;i<tmpTasks.size();i++){
        cout<<"task "<<tmpTasks[i].index<<": "<<endl;
        min=100;
        int taskIdx=tmpTasks[i].index-1;
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
        earliestStartTime=inputTransmissionTimeRSU;
        }
        if(minPrevFinishTime==100){
        minPrevFinishTime=inputTransmissionTimeRSU;
        }

        minPrevFinishTimes[taskIdx][j]=minPrevFinishTime;
        earliestStartTimes[taskIdx][j]=earliestStartTime;

        states[taskIdx][j]=earliestStartTimes[taskIdx][j]+computationTimes[taskIdx][j];
        finishTimes[taskIdx][j]=states[taskIdx][j];
//                        costs[taskIdx][j]=computationTimes[taskIdx][j]*vehicles[j].cost;

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
        cout<<"vehicle "<<j+1<<": "<<endl;
        cout<<"state: "<<states[taskIdx][j]<<endl;
//                        cout<<"cost: "<<costs[taskIdx][j]<<endl;
        cout<<"beginTime: "<<minPrevFinishTime<<endl;
        cout<<"EST: "<<earliestStartTimes[taskIdx][j]<<endl;
        cout<<"AST: "<<actualStartTimes[taskIdx][j]<<endl;
        cout<<"endTime: "<<states[taskIdx][j]<<endl;
        cout<<"transmissionTime: "<<transmissionTimes[taskIdx][j]<<endl;
        cout<<"computationTime: "<<computationTimes[taskIdx][j]<<endl;
        cout<<endl;

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

        // max 小于初划分的 time，存在 idle time，则将 time 和 cost 进行 trade off
                    /*if(i==tmpTasks.size()-1&&max<initialDivision[levelIdx][0]){
                        // 取最接近初划分的 time 的一次分配，且 cost 要更小
                        // 这么做是因为后续的同一level内的分配，尽量使用并行处理，分配到不同节点
                        // 第一次分配的任务是负载最大的任务，如果该任务能按时完成，则其他并行处理的任务也可以
                        double closeMax=0;
                        int prevVehicle=scheduleRes[maxTask];
                        for(int j=0;j<M&&j!=prevVehicle;j++){
                            if(states[maxTask][j]>closeMax&&states[maxTask][j]<=initialDivision[levelIdx][0]&&costs[maxTask][j]<=costs[maxTask][prevVehicle]){
                                closeMax=states[maxTask][j];
                                scheduleRes[maxTask]=j;
                                beginAndEndTimes[taskIdx][0]=beginTimes[maxTask][j];
                                beginAndEndTimes[taskIdx][1]=endTimes[maxTask][j];
                            }
                        }
                        if(closeMax!=0){
                            cout<<"max is: "<<max<<endl;
                            cout<<"time initial division is: "<<initialDivision[levelIdx][0]<<endl;
                            cout<<"need trade off!"<<endl;
                            cout<<"change vehicle to "<<scheduleRes[maxTask]+1<<endl;
                            cout<<"state: "<<states[taskIdx][scheduleRes[maxTask]]<<endl;
                            cout<<endl;
                        }
                    }*/
        }
        }else{
        // 每个任务分配完后更新 max-min 图
        // 再分配给同节点就需要排队，开始时间取排队任务和前驱任务完成时间的较大者
        cout<<"other distribute: "<<endl;
        int sameVehicle=scheduleRes[maxTask];
        int prevTask=maxTask;
        max=0;
        for(int i=0;i<tmpTasks.size();i++){
        min=100;
        int taskIdx=tmpTasks[i].index-1;
        // update 过程更新的值
        actualStartTimes[taskIdx][sameVehicle]=states[prevTask][sameVehicle];
        // 取 ACT 和 EST 的最大者
        if(actualStartTimes[taskIdx][sameVehicle]<earliestStartTimes[taskIdx][sameVehicle]){
        actualStartTimes[taskIdx][sameVehicle]=earliestStartTimes[taskIdx][sameVehicle];
        }
        states[taskIdx][sameVehicle]=actualStartTimes[taskIdx][sameVehicle]+computationTimes[taskIdx][sameVehicle];

        // 当传输时间特别小，可以把任务拆成两份执行

        // update 过程更新的值
        finishTimes[taskIdx][sameVehicle]=states[taskIdx][sameVehicle];
        if(!isVehicleAvailable(taskIdx,sameVehicle,minPrevFinishTimes,actualStartTimes,finishTimes,interval,scheduleRes,finishDistributeTasks)){
        states[taskIdx][sameVehicle]=UNAVAILABLESTATE;
        }
        cout<<"update task "<<taskIdx+1<<" distribute to vehicle "<<sameVehicle+1<<": "<<endl;
        cout<<"state: "<<states[taskIdx][sameVehicle]<<endl;
        cout<<endl;

        // 原不可用的分配，可能变得可用
        for(int j=0;j<M;j++){
        // 保存不可用前的状态
        double stateBeforeUnavailable=states[taskIdx][j];
        if(!isVehicleAvailable(taskIdx,j,minPrevFinishTimes,actualStartTimes,finishTimes,interval,scheduleRes,finishDistributeTasks)){
        states[taskIdx][j]=UNAVAILABLESTATE;
        }
//                        cout<<"after update state: "<<states[taskIdx][j]<<endl;
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

        // max 小于初划分的 time，存在 idle time，则将 time 和 cost 进行 trade off
                    /*if(i==tmpTasks.size()-1&&max<initialDivision[levelIdx][0]){
                        // 取最接近初划分的 time 的一次分配，且 cost 要更小
                        // 这么做是因为后续的同一level内的分配，尽量使用并行处理，分配到不同节点
                        // 第一次分配的任务是负载最大的任务，如果该任务能按时完成，则其他并行处理的任务也可以
                        double closeMax=0;
                        int prevVehicle=scheduleRes[maxTask];
                        for(int j=0;j<M&&j!=prevVehicle;j++){
                            if(states[maxTask][j]>closeMax&&states[maxTask][j]<=initialDivision[levelIdx][0]&&costs[maxTask][j]<=costs[maxTask][prevVehicle]){
                                closeMax=states[maxTask][j];
                                scheduleRes[maxTask]=j;
                                beginAndEndTimes[taskIdx][0]=states[maxTask][j]-computationTimes[maxTask][j];
                                beginAndEndTimes[taskIdx][1]=states[maxTask][j];
                            }
                        }
                        if(closeMax!=0){
                            cout<<"max is: "<<max<<endl;
                            cout<<"time initial division is: "<<initialDivision[levelIdx][0]<<endl;
                            cout<<"need trade off!"<<endl;
                            cout<<"change vehicle to "<<scheduleRes[maxTask]+1<<endl;
                            cout<<"state: "<<states[taskIdx][scheduleRes[maxTask]]<<endl;
                            cout<<endl;
                        }
                    }*/
        }
        }
        // 完成一次分配
        cout<<"max: "<<endl;
//            cout<<"cost: "<<costs[maxTask][scheduleRes[maxTask]]<<endl;
        cout<<"task: "<<maxTask+1<<endl;
        cout<<"vehicle: "<<scheduleRes[maxTask]+1<<endl;
        cout<<"beginTime: "<<beginAndEndTimes[maxTask][0]<<endl;
        cout<<"EST: "<<earliestStartTimes[maxTask][scheduleRes[maxTask]]<<endl;
        cout<<"AST: "<<actualStartTimes[maxTask][scheduleRes[maxTask]]<<endl;
        cout<<"endTime: "<<beginAndEndTimes[maxTask][1]<<endl;
        cout<<"transmissionTime: "<<transmissionTimes[maxTask][scheduleRes[maxTask]]<<endl;
        cout<<"computationTime: "<<computationTimes[maxTask][scheduleRes[maxTask]]<<endl;
        cout<<"waitTime: "<<waitTimes[maxTask][scheduleRes[maxTask]]<<endl;

        finishDistributeTasks.push_back(maxTask);

        tmpTasks.erase(tmpTasks.begin()+deletedTask);
        cout<<endl;
        cout<<"remaining tasks: "<<endl;
        for(int i=0;i<tmpTasks.size();i++){
        cout<<tmpTasks[i].index<<endl;
        }
        cout<<endl;
        }
        }

        // max-min 算法基于 finish time 的初分配结果
        cout<<"initial schedule result: "<<endl;
    /*for(int i=0;i<K;i++){
        for(int j=0;j<M;j++){
            computationTimes[i][j]=tasks[i].computation/vehicles[j].capability;
        }
    }*/

        for(int i=0;i<K;i++){
        /*if(i==0){
            scheduleRes[i]=5;
            beginAndEndTimes[i][0]=0.666667,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=0.966667;
            earliestStartTimes[i][scheduleRes[i]]=0.666667;
            actualStartTimes[i][scheduleRes[i]]=0.666667;
            transmissionTimes[i][scheduleRes[i]]=0;
            computationTimes[i][scheduleRes[i]]=0.3;
        }
        if(i==1){
            scheduleRes[i]=5;
            beginAndEndTimes[i][0]=0.966667,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=1.86667;
            earliestStartTimes[i][scheduleRes[i]]=0.966667;
            actualStartTimes[i][scheduleRes[i]]=1.66667;
            transmissionTimes[i][scheduleRes[i]]=0;
            computationTimes[i][scheduleRes[i]]=0.2;
            *//*scheduleRes[i]=1;
            beginAndEndTimes[i][0]=0.966667,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=1.6;
            earliestStartTimes[i][scheduleRes[i]]=1.26667;
            actualStartTimes[i][scheduleRes[i]]=1.26667;
            transmissionTimes[i][scheduleRes[i]]=0.3;
            computationTimes[i][scheduleRes[i]]=0.33333;*//*
        }if(i==2){
            scheduleRes[i]=5;
            beginAndEndTimes[i][0]=0.966667,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=1.66667;
            earliestStartTimes[i][scheduleRes[i]]=0.966667;
            actualStartTimes[i][scheduleRes[i]]=1.36667;
            transmissionTimes[i][scheduleRes[i]]=0;
            computationTimes[i][scheduleRes[i]]=0.3;
            *//*scheduleRes[i]=5;
            beginAndEndTimes[i][0]=0.966667,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=1.56667;
            earliestStartTimes[i][scheduleRes[i]]=0.966667;
            actualStartTimes[i][scheduleRes[i]]=1.26667;
            transmissionTimes[i][scheduleRes[i]]=0;
            computationTimes[i][scheduleRes[i]]=0.3;*//*
        }if(i==3){
            scheduleRes[i]=5;
            beginAndEndTimes[i][0]=0.966667,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=1.36667;
            earliestStartTimes[i][scheduleRes[i]]=0.966667;
            actualStartTimes[i][scheduleRes[i]]=0.966667;
            transmissionTimes[i][scheduleRes[i]]=0;
            computationTimes[i][scheduleRes[i]]=0.4;
            *//*scheduleRes[i]=5;
            beginAndEndTimes[i][0]=0.966667,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=1.96667;
            earliestStartTimes[i][scheduleRes[i]]=0.966667;
            actualStartTimes[i][scheduleRes[i]]=1.56667;
            transmissionTimes[i][scheduleRes[i]]=0;
            computationTimes[i][scheduleRes[i]]=0.4;*//*
        }if(i==4){
            scheduleRes[i]=1;
            beginAndEndTimes[i][0]=0.966667,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=1.76667;
            earliestStartTimes[i][scheduleRes[i]]=1.26667;
            actualStartTimes[i][scheduleRes[i]]=1.26667;
            transmissionTimes[i][scheduleRes[i]]=0.3;
            computationTimes[i][scheduleRes[i]]=0.5;
            *//*scheduleRes[i]=1;
            beginAndEndTimes[i][0]=0.966667,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=2.1;
            earliestStartTimes[i][scheduleRes[i]]=1.26667;
            actualStartTimes[i][scheduleRes[i]]=1.6;
            transmissionTimes[i][scheduleRes[i]]=0.3;
            computationTimes[i][scheduleRes[i]]=0.5;*//*
 *//*scheduleRes[i]=5;
            beginAndEndTimes[i][0]=0.966667,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=1.26667;
            earliestStartTimes[i][scheduleRes[i]]=0.966667;
            actualStartTimes[i][scheduleRes[i]]=0.966667;
            transmissionTimes[i][scheduleRes[i]]=0;
            computationTimes[i][scheduleRes[i]]=0.3;*//*
        }if(i==5){
            scheduleRes[i]=4;
            beginAndEndTimes[i][0]=1.66667,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=2.71528;
            earliestStartTimes[i][scheduleRes[i]]=2.60417;
            actualStartTimes[i][scheduleRes[i]]=2.60417;
            transmissionTimes[i][scheduleRes[i]]=0.9375;
            computationTimes[i][scheduleRes[i]]=0.111111;
        }if(i==6){
            scheduleRes[i]=1;
            beginAndEndTimes[i][0]=1.66667,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=2.63333;
            earliestStartTimes[i][scheduleRes[i]]=1.96667;
            actualStartTimes[i][scheduleRes[i]]=1.96667;
            transmissionTimes[i][scheduleRes[i]]=0.3;
            computationTimes[i][scheduleRes[i]]=0.666667;
        }if(i==7){
            scheduleRes[i]=0;
            beginAndEndTimes[i][0]=1.66667,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=2.69167;
            earliestStartTimes[i][scheduleRes[i]]=2.29167;
            actualStartTimes[i][scheduleRes[i]]=2.29167;
            transmissionTimes[i][scheduleRes[i]]=0.625;
            computationTimes[i][scheduleRes[i]]=0.4;
        }if(i==8){
            scheduleRes[i]=2;
            beginAndEndTimes[i][0]=1.36667,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=2.91429;
            earliestStartTimes[i][scheduleRes[i]]=2.2;
            actualStartTimes[i][scheduleRes[i]]=2.2;
            transmissionTimes[i][scheduleRes[i]]=0.833333;
            computationTimes[i][scheduleRes[i]]=0.714286;
        }if(i==9){
            scheduleRes[i]=5;
            beginAndEndTimes[i][0]=1.36667,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=2.36667;
            earliestStartTimes[i][scheduleRes[i]]=2.06667;
            actualStartTimes[i][scheduleRes[i]]=2.06667;
            transmissionTimes[i][scheduleRes[i]]=0.3;
            computationTimes[i][scheduleRes[i]]=0.3;
            *//*scheduleRes[i]=5;
            beginAndEndTimes[i][0]=2.1,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=2.7;
            earliestStartTimes[i][scheduleRes[i]]=2.4;
            actualStartTimes[i][scheduleRes[i]]=2.4;
            transmissionTimes[i][scheduleRes[i]]=0.3;
            computationTimes[i][scheduleRes[i]]=0.3;*//*
        }if(i==10){
            *//*scheduleRes[i]=1;
            beginAndEndTimes[i][0]=1.86667,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=3.42361;
            earliestStartTimes[i][scheduleRes[i]]=2.92361;
            actualStartTimes[i][scheduleRes[i]]=2.92361;
            transmissionTimes[i][scheduleRes[i]]=0.208333;
            computationTimes[i][scheduleRes[i]]=0.5;*//*
            scheduleRes[i]=5;
            beginAndEndTimes[i][0]=1.86667,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=3.40833;
            earliestStartTimes[i][scheduleRes[i]]=3.10833;
            actualStartTimes[i][scheduleRes[i]]=3.10833;
            transmissionTimes[i][scheduleRes[i]]=0.416667;
            computationTimes[i][scheduleRes[i]]=0.3;
        }if(i==11){
            *//*scheduleRes[i]=0;
            beginAndEndTimes[i][0]=2.63333,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=3.83333;
            earliestStartTimes[i][scheduleRes[i]]=3.03333;
            actualStartTimes[i][scheduleRes[i]]=3.03333;
            transmissionTimes[i][scheduleRes[i]]=0.4;
            computationTimes[i][scheduleRes[i]]=0.8;*//*
            scheduleRes[i]=1;
            beginAndEndTimes[i][0]=2.63333,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=3.3;
            earliestStartTimes[i][scheduleRes[i]]=2.63333;
            actualStartTimes[i][scheduleRes[i]]=2.63333;
            transmissionTimes[i][scheduleRes[i]]=0;
            computationTimes[i][scheduleRes[i]]=0.666667;
        }if(i==12){
            *//*scheduleRes[i]=5;
            beginAndEndTimes[i][0]=2.63333,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=4.25595;
            earliestStartTimes[i][scheduleRes[i]]=3.95595;
            actualStartTimes[i][scheduleRes[i]]=3.95595;
            transmissionTimes[i][scheduleRes[i]]=1.04167;
            computationTimes[i][scheduleRes[i]]=0.3;*//*
            scheduleRes[i]=0;
            beginAndEndTimes[i][0]=2.63333,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=3.63333;
            earliestStartTimes[i][scheduleRes[i]]=3.03333;
            actualStartTimes[i][scheduleRes[i]]=3.03333;
            transmissionTimes[i][scheduleRes[i]]=0.4;
            computationTimes[i][scheduleRes[i]]=0.6;
        }if(i==13){
            *//*scheduleRes[i]=5;
            beginAndEndTimes[i][0]=2.36667,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=4.65595;
            earliestStartTimes[i][scheduleRes[i]]=4.25595;
            actualStartTimes[i][scheduleRes[i]]=4.25595;
            transmissionTimes[i][scheduleRes[i]]=0;
            computationTimes[i][scheduleRes[i]]=0.4;*//*
            scheduleRes[i]=1;
            beginAndEndTimes[i][0]=2.36667,finishTimes[i][scheduleRes[i]]=beginAndEndTimes[i][1]=4.6;
            earliestStartTimes[i][scheduleRes[i]]=3.93333;
            actualStartTimes[i][scheduleRes[i]]=3.93333;
            transmissionTimes[i][scheduleRes[i]]=0.3;
            computationTimes[i][scheduleRes[i]]=0.666667;
        }*/
        cout<<"task "<<i+1<<" distribute to vehicle "<<scheduleRes[i]+1<<endl;
        cout<<"from time "<<beginAndEndTimes[i][0]<<" to "<<beginAndEndTimes[i][1]<<endl;
        cout<<"EST: "<<earliestStartTimes[i][scheduleRes[i]]<<endl;
        cout<<"AST: "<<actualStartTimes[i][scheduleRes[i]]<<endl;
        cout<<"Tc: "<<transmissionTimes[i][scheduleRes[i]]<<endl;
        cout<<"Tp: "<<computationTimes[i][scheduleRes[i]]<<endl;
        cout<<endl;
        }

        double totalFinishTime=0;
        for(int k=0;k<K;k++){
        if(finishTimes[k][scheduleRes[k]]>totalFinishTime)
        totalFinishTime=finishTimes[k][scheduleRes[k]];
        }
        cout<<"total finishTime: "<<totalFinishTime<<endl;
        cout<<endl;

        vector<int> tmpSequence[levelNum][M];
        vector<int> sequence[levelNum][M];
        for(int i=0;i<levelNum;i++){
        for(int j=0;j<M;j++){
        for(int k=0;k<taskLevel[i].size();k++){
        if(scheduleRes[taskLevel[i][k].index-1]==j){
        tmpSequence[i][j].push_back(taskLevel[i][k].index-1);
//                    cout<<taskLevel[i][k].index-1<<endl;
        }
        }
        }
        }

        // 按照排队任务的顺序
        for(int i=0;i<levelNum;i++){
        for(int j=0;j<M;j++){
        while(tmpSequence[i][j].size()!=0){
        double minSequenceFinishTime=100;
        double minSequenceFinishTimeIdx=-1;
        for(int k=0;k<tmpSequence[i][j].size();k++){
        if(finishTimes[tmpSequence[i][j][k]][j]<minSequenceFinishTime){
        minSequenceFinishTime=finishTimes[tmpSequence[i][j][k]][j];
        minSequenceFinishTimeIdx=k;
        }
        }
        // sequence 中为任务 id
        if(minSequenceFinishTimeIdx!=-1){
        sequence[i][j].push_back(tmpSequence[i][j][minSequenceFinishTimeIdx]);
        tmpSequence[i][j].erase(tmpSequence[i][j].begin()+minSequenceFinishTimeIdx);
        }
        }
        }
        }

        cout<<"sequence: "<<endl;
        for(int i=0;i<levelNum;i++){
        cout<<"level "<<i+1<<": "<<endl;
        for(int j=0;j<M;j++){
        if(sequence[i][j].size()>0){
        cout<<"vehicle "<<j+1<<": "<<" ";
        for(int k=0;k<sequence[i][j].size();k++){
        cout<<sequence[i][j][k]+1<<" ";
        if(k==sequence[i][j].size()-1){
        cout<<endl;
        }
        }
        }
        }
        }

        // 3.根据 deadline、预测传输时间、负载，计算 sub-deadline
        // 计算车辆在 VC 内运动时 max min ave Vc
        vector<double> maxAndMinRes=findMaxAndMinTransmissionRate(vehicles,interval);
        double maxRate=maxAndMinRes[0];
        double minRate=maxAndMinRes[1];
        double ave=(maxRate+minRate)/2;

        cout<<endl;
        cout<<"Vc: "<<endl;
        cout<<"max: "<<maxRate<<endl;
        cout<<"min: "<<minRate<<endl;
        cout<<"ave: "<<ave<<endl;
        cout<<endl;

        ave=maxRate;

        // 划分时间，计算 sub-deadline
        vector<vector<double>> vectorInitialDivision=divideSubDeadline(taskLevel,inputTransmissionTimeRSU,ave,deadline);
        double subDeadline[levelNum][2];
        for(int i=0;i<levelNum;i++){
        for(int j=0;j<2;j++){
        subDeadline[i][j]=vectorInitialDivision[i][j];
        }
        }

        cout<<"timeDivision: "<<endl;
        for(int i=0;i<levelNum;i++){
        cout<<subDeadline[i][0]<<" "<<subDeadline[i][1]<<endl;
        }
        cout<<endl;

        int reScheduleFlag=-1;

        // 保留原分配方案
        int scheduleResBefore[K];
        double beginAndEndTimesBefore[K][2];
        double transmissionTimesBefore[K][M];
        double minPrevFinishTimesBefore[K][M];
        double earliestStartTimesBefore[K][M];
        double actualStartTimesBefore[K][M];
        double waitTimesBefore[K][M];
        double finishTimesBefore[K][M];
        vector<int> sequenceBefore[levelNum][M];

        // 保存重分配结果
        int reScheduleScheduleRes[K];
        double reScheduleBeginAndEndTimes[K][2];
        double reScheduleTransmissionTimes[K][M];
        double reScheduleMinPrevFinishTimes[K][M];
        double reScheduleEarliestStartTimes[K][M];
        double reScheduleActualStartTimes[K][M];
        double reScheduleWaitTimes[K][M];
        double reScheduleFinishTimes[K][M];
        vector<int> reScheduleSequence[levelNum][M];

        vector<int> tmpList;
        vector<int> waitList;

        // 4.初分配总完成时间超出 deadline，调整 t 变小，满足 deadline
        if(totalFinishTime>deadline){
        cout<<"need time re schedule! "<<endl;
        for(int i=1;i<levelNum;i++){
        if(reScheduleFlag==1){
        break;
        }

        cout<<"level "<<i+1<<": "<<endl;
        double tx=subDeadline[i][1];
        cout<<"tx: "<<tx<<endl;

        // 找到 ti>tx 的任务，加入未标记队列
        vector<Task> tasksInOneLevel=taskLevel[i];
        vector<Task> unmarkQueue=tasksInOneLevel;
        vector<Task> markQueue;
        vector<Task> tmpTasks;

        while(unmarkQueue.size()!=0){
        if(reScheduleFlag==1){
        break;
        }

        // 对目前未分配任务中 ti>tx 的任务进行排序，每次重分配完成一个任务后，都需要进行排序
        for(int j=0;j<unmarkQueue.size();j++){
        int taskIdx=unmarkQueue[j].index-1;
        double ti=beginAndEndTimes[taskIdx][1];
        if(ti>tx){
        tmpTasks.push_back(unmarkQueue[j]);
        }
        }
        double maxTmpTasks=0;
        int maxIdx=-1;
        for(int j=0;j<tmpTasks.size();j++){
        int taskIdx=tmpTasks[j].index-1;
        double ti=beginAndEndTimes[taskIdx][1];
        if(ti-tx>maxTmpTasks){
        maxTmpTasks=ti-tx;
        maxIdx=j;
        }
        }
        if(maxIdx==-1){
        break;
        cout<<endl;
        }

        // 取当前 ti-tx 值最大的进行重分配
        Task task=tmpTasks[maxIdx];
        int taskIdx=task.index-1;
        cout<<"re schedule task "<<taskIdx+1<<": "<<endl;
        int distributeVehicleIdx=scheduleRes[taskIdx];
        double actualStartTimeBefore=actualStartTimes[taskIdx][distributeVehicleIdx];

        // 有多个节点的重分配方案可行
        vector<int> availableNode;
                /*int tmpScheduleRes[M][K];
                double tmpBeginAndEndTimes[M][K][2];
                double tmpTransmissionTimes[M][K][M];
                double tmpMinPrevFinishTimes[M][K][M];
                double tmpEarliestStartTimes[M][K][M];
                double tmpActualStartTimes[M][K][M];
                double tmpWaitTimes[M][K][M];
                double tmpFinishTimes[M][K][M];
                vector<int> tmpSequence[M][levelNum][M];*/

        double minDiffLess=100;
        double minDiffMore=100;
        int minDiffVehicleIdxLess=-1;
        int minDiffVehicleIdxMore=-1;

//                markQueue.push_back(t2);
//                markQueue.push_back(t5);
        for(int j=0;j<M;j++){
        if(j==distributeVehicleIdx){
        continue;
        }
        cout<<"**********"<<endl;  //**********
        cout<<"re schedule to vehicle "<<j+1<<": "<<endl;


        scheduleRes[taskIdx]=j;
        int flag=1;
        int tiFlag=1;
        finishDistributeTasks.clear();

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
        estimateTransmissionTime=tasks[k].transmission/getTransmissionRate(prevVehicleIdx,j);
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
        earliestStartTime=inputTransmissionTimeRSU;
        }
        if(minPrevFinishTime==100){
        minPrevFinishTime=inputTransmissionTimeRSU;
        }

        minPrevFinishTimes[taskIdx][j]=minPrevFinishTime;
        earliestStartTimes[taskIdx][j]=earliestStartTime;
        transmissionTimes[taskIdx][j]=transmissionTime;

        beginAndEndTimes[taskIdx][0]=minPrevFinishTimes[taskIdx][j];
//                        costs[taskIdx][j]=computationTimes[taskIdx][j]*vehicles[j].cost;

        cout<<"MPFT: "<<minPrevFinishTimes[taskIdx][j]<<endl;
        cout<<"EST: "<<earliestStartTimes[taskIdx][j]<<endl;
        cout<<"Tc: "<<transmissionTimes[taskIdx][j]<<endl;

        // 重分配遇到和 level 内其他任务分配到同一节点的排队情况，根据 EST 大小顺序进行处理
        tmpList.clear();
        waitList.clear();

        tmpList.push_back(taskIdx);
        for(int k=0;k<tasksInOneLevel.size();k++){
        if(tasksInOneLevel[k].index-1!=taskIdx&&scheduleRes[tasksInOneLevel[k].index-1]==j){
        tmpList.push_back(tasksInOneLevel[k].index-1);
        }
        }
        cout<<endl;
        cout<<"wait list: "<<endl;
        while(tmpList.size()!=0){
        double minEST=100;
        int minESTidx=-1;
        for(int k=0;k<tmpList.size();k++){
        if(earliestStartTimes[tmpList[k]][j]<minEST){
        minEST=earliestStartTimes[tmpList[k]][j];
        minESTidx=k;
        }
        }
        cout<<"task "<<tmpList[minESTidx]+1<<" ";
        cout<<"EST "<<earliestStartTimes[tmpList[minESTidx]][j]<<endl;

        waitList.push_back(tmpList[minESTidx]);
        tmpList.erase(tmpList.begin()+minESTidx);
        }
        cout<<endl;

        // waitList 必有一个元素
        double prevFinishTime=earliestStartTimes[waitList[0]][j];

        // 维护 sequence
        sequence[i][j].clear();
        for(int k=0;k<waitList.size();k++){
        int waitTaskIdx=waitList[k];
        cout<<"task "<<waitTaskIdx+1<<": "<<endl;

        // 排队为串行执行
        actualStartTimes[waitTaskIdx][j]=prevFinishTime;
        finishTimes[waitTaskIdx][j]=actualStartTimes[waitTaskIdx][j]+computationTimes[waitTaskIdx][j];
        waitTimes[waitTaskIdx][j]=actualStartTimes[waitTaskIdx][j]-earliestStartTimes[waitTaskIdx][j];

        prevFinishTime=finishTimes[waitTaskIdx][j];

        beginAndEndTimes[waitTaskIdx][1]=finishTimes[waitTaskIdx][j];

        sequence[i][j].push_back(waitTaskIdx);

        cout<<"AST: "<<actualStartTimes[waitTaskIdx][j]<<endl;
        cout<<"FT: "<<finishTimes[waitTaskIdx][j]<<endl;
        cout<<"WT: "<<waitTimes[waitTaskIdx][j]<<endl;
        }

        // 更新 AST FT 后再检查可用性
        for(int k=0;k<waitList.size();k++){
        int waitTaskIdx=waitList[k];
        // 该车辆必须是可用的
        if(!isVehicleAvailable(waitTaskIdx,j,minPrevFinishTimes,actualStartTimes,finishTimes,interval,scheduleRes,finishDistributeTasks)){
        flag=-1;
        tiFlag=-1;
        }
        }

        cout<<"update sequence task "<<taskIdx+1<<" to vehicle "<<j+1<<": ";
        for(int k=0;k<sequence[i][j].size();k++){
        cout<<sequence[i][j][k]+1<<" ";
        }
        cout<<endl;

        // 更新其他任务
        // 原分配节点上排队的任务
        for(int k=0;k<tasksInOneLevel.size();k++){
        if(scheduleRes[tasksInOneLevel[k].index-1]==distributeVehicleIdx){
        cout<<endl;
        cout<<"before wait list on vehicle "<<distributeVehicleIdx+1<<": "<<endl;
        vector<int> sequenceOnBeforeVehicle=sequence[i][distributeVehicleIdx];
        // 重分配的任务后的任务向前移动一格
        for(int l=0;l<sequenceOnBeforeVehicle.size()-1;l++){
        if(sequenceOnBeforeVehicle[l]==taskIdx){
        double prevFinishTime=actualStartTimeBefore;
        for(int m=l+1;m<sequenceOnBeforeVehicle.size();m++){
        cout<<"task "<<sequenceOnBeforeVehicle[m]+1<<": "<<endl;
        actualStartTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx]=prevFinishTime;
        finishTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx]=actualStartTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx]+computationTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx];
        waitTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx]=actualStartTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx]-earliestStartTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx];

        prevFinishTime=finishTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx];

        beginAndEndTimes[sequenceOnBeforeVehicle[m]][1]=finishTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx];

        // 该车辆必须是可用的
        if(!isVehicleAvailable(sequenceOnBeforeVehicle[l],distributeVehicleIdx,minPrevFinishTimes,actualStartTimes,finishTimes,interval,scheduleRes,finishDistributeTasks)){
        flag=-1;
        tiFlag=-1;
        }

        cout<<"AST: "<<actualStartTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx]<<endl;
        cout<<"FT: "<<finishTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx]<<endl;
        cout<<"WT: "<<waitTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx]<<endl;
        }
        break;
        }
        }
        sequence[i][distributeVehicleIdx].clear();
        for(int l=0;l<sequenceOnBeforeVehicle.size()-1;l++){
        if(sequenceOnBeforeVehicle[l]!=taskIdx){
        sequence[i][distributeVehicleIdx].push_back(sequenceOnBeforeVehicle[l]);
        }
        }
        cout<<"before sequence task "<<taskIdx+1<<" to vehicle "<<distributeVehicleIdx+1<<": ";
        for(int k=0;k<sequence[i][distributeVehicleIdx].size();k++){
        cout<<sequence[i][distributeVehicleIdx][k]+1<<" ";
        }
        cout<<endl;

        break;
        }
        }

        tmpList.clear();
        waitList.clear();

        cout<<endl;
        cout<<"mark queue: "<<endl;
        for(int k=0;k<markQueue.size();k++){
        cout<<"task "<<markQueue[k].index<<" FT: "<<finishTimes[markQueue[k].index-1][scheduleRes[markQueue[k].index-1]]<<endl;
        // 已处理的任务完成时间仍小于 tx 或不能增加
        if(!(finishTimes[markQueue[k].index-1][scheduleRes[markQueue[k].index-1]]<tx||finishTimes[markQueue[k].index-1][scheduleRes[markQueue[k].index-1]]<=finishTimesBefore[markQueue[k].index-1][scheduleRes[markQueue[k].index-1]])){
        cout<<"mark queue fail!"<<endl;
        flag=-1;
        tiFlag=-1;
        }
        }

        // 一个level接一个level进行完成
        cout<<endl;
        cout<<"finish distribute tasks: ";
        for(int k=0;k<=i;k++){
        for(int l=0;l<taskLevel[k].size();l++){
        cout<<taskLevel[k][l].index<<" ";
        finishDistributeTasks.push_back(taskLevel[k][l].index-1);
        }
        }
        cout<<endl;

        // 重分配影响的是：1.同level的前后车辆 2.后续level的后继任务+同车上的可用性
        cout<<endl;
        cout<<"follow up level: "<<endl;
        // 后续 level，按车辆上的排队顺序进行更新
        for(int l=i+1;l<levelNum;l++){
        cout<<"level "<<l+1<<": "<<endl;
        for(int m=0;m<M;m++){
        if(sequence[l][m].size()==0){
        continue;
        }
        cout<<"vehicle "<<m+1<<": "<<endl;
        // 更新 EST
        for(int n=0;n<sequence[l][m].size();n++){
        int followupTaskIdx=sequence[l][m][n];
        cout<<"task "<<followupTaskIdx+1<<": "<<endl;

        double followupMinPrevFinishTime=100;
        double followupEarliestStartTime=0;
        double followupTransmissionTime=0;
        for(int k=0;k<K;k++){
        // 考虑多前驱
        if(dependency[followupTaskIdx][k]>0){
        int prevVehicleIdx=scheduleRes[k];

        // 车辆需在 minPrevEndTime 时刻在 VC 范围内
        double prevFinishTime=finishTimes[k][scheduleRes[k]];
        if(prevFinishTime<followupMinPrevFinishTime){
        followupMinPrevFinishTime=prevFinishTime;
        }

        // 预估传输时间
        double estimateTransmissionTime=0;
        if(prevVehicleIdx!=m){
        // 前驱任务的传输量
        estimateTransmissionTime=tasks[k].transmission/getTransmissionRate(prevVehicleIdx,m);
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

        cout<<"MPFT: "<<minPrevFinishTimes[followupTaskIdx][m]<<endl;
        cout<<"EST: "<<earliestStartTimes[followupTaskIdx][m]<<endl;
        cout<<"Tc: "<<transmissionTimes[followupTaskIdx][m]<<endl;
        }
        cout<<endl;

        double prevFinishTime=earliestStartTimes[sequence[l][m][0]][m];
        for(int n=0;n<sequence[l][m].size();n++){
        int followupTaskIdx=sequence[l][m][n];
        actualStartTimes[followupTaskIdx][m]=prevFinishTime;

        double maxSameVehicleFinishTime=0;
        int maxSameVehicleIdx=-1;
        // 等待前level内同车的任务完成
        for(int o=0;o<l;o++){
        for(int p=0;p<sequence[o][m].size();p++){
        int sameVehicle=sequence[o][m][p];
        if(finishTimes[sameVehicle][scheduleRes[sameVehicle]]>maxSameVehicleFinishTime){
        maxSameVehicleIdx=sameVehicle;
        maxSameVehicleFinishTime=finishTimes[sameVehicle][scheduleRes[sameVehicle]];
        }
        }
        }
        cout<<endl;
        if(maxSameVehicleFinishTime>actualStartTimes[followupTaskIdx][m]){
        cout<<"before level same vehicle task: "<<maxSameVehicleIdx+1<<endl;
        cout<<"max same vehicle finish time: "<<maxSameVehicleFinishTime<<endl;
        actualStartTimes[followupTaskIdx][m]=maxSameVehicleFinishTime;
        }

        finishTimes[followupTaskIdx][m]=actualStartTimes[followupTaskIdx][m]+computationTimes[followupTaskIdx][m];
        waitTimes[followupTaskIdx][m]=actualStartTimes[followupTaskIdx][m]-earliestStartTimes[followupTaskIdx][m];

        prevFinishTime=finishTimes[followupTaskIdx][m];

        beginAndEndTimes[followupTaskIdx][1]=finishTimes[followupTaskIdx][m];

        // 该车辆必须是可用的
        if(!isVehicleAvailable(followupTaskIdx,m,minPrevFinishTimes,actualStartTimes,finishTimes,interval,scheduleRes,finishDistributeTasks)){
        flag=-1;
        }

        cout<<"task "<<followupTaskIdx+1<<": "<<endl;
        cout<<"AST: "<<actualStartTimes[followupTaskIdx][m]<<endl;
        cout<<"FT: "<<finishTimes[followupTaskIdx][m]<<endl;
        cout<<"WT: "<<waitTimes[followupTaskIdx][m]<<endl;
        }
        cout<<endl;
        }
        }

        double expectedTotalFinishTime=0;
        for(int k=0;k<K;k++){
        if(finishTimes[k][scheduleRes[k]]>expectedTotalFinishTime){
        expectedTotalFinishTime=finishTimes[k][scheduleRes[k]];
        }
        }

        cout<<"expected total finish time: "<<expectedTotalFinishTime<<endl;
        cout<<"before flag: "<<flag<<endl;

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
        availableNode.push_back(j);
        flag=1;
        }else{
        flag=-1;
        }
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
        availableNode.push_back(j);
        flag=1;
        }
        }

        cout<<"flag: "<<flag<<endl;
        cout<<"ti flag: "<<tiFlag<<endl;
        cout<<endl;

        if(flag==1){
        // 记录分配到此车辆节点的数据
                        /*memcpy(tmpScheduleRes[j],scheduleRes,sizeof(scheduleRes));
                        memcpy(tmpBeginAndEndTimes[j],beginAndEndTimes,sizeof(beginAndEndTimes));
                        memcpy(tmpTransmissionTimes[j],transmissionTimes,sizeof(transmissionTimes));
                        memcpy(tmpMinPrevFinishTimes[j],minPrevFinishTimes,sizeof(minPrevFinishTimes));
                        memcpy(tmpEarliestStartTimes[j],earliestStartTimes,sizeof(earliestStartTimes));
                        memcpy(tmpActualStartTimes[j],actualStartTimes,sizeof(actualStartTimes));
                        memcpy(tmpWaitTimes[j],waitTimes,sizeof(waitTimes));
                        memcpy(tmpFinishTimes[j],finishTimes,sizeof(finishTimes));
                        memcpy(tmpSequence[j],sequence,sizeof(sequence));*/

        }

        // 还原
        }

        int flag=-1;
        int finalDistributeVehicleIdx;
        if(minDiffVehicleIdxLess>-1){
        double ti=finishTimes[taskIdx][minDiffVehicleIdxLess];
        finalDistributeVehicleIdx=minDiffVehicleIdxLess;
        flag=1;
        }else if(minDiffVehicleIdxMore>-1){
        finalDistributeVehicleIdx=minDiffVehicleIdxMore;
        flag=1;
        }

        cout<<"flag: "<<flag<<endl;
        if(flag==1){
        // 更新任务分配
                    /*memcpy(scheduleRes,tmpScheduleRes[finalDistributeVehicleIdx],sizeof(tmpScheduleRes[finalDistributeVehicleIdx]));
                    memcpy(beginAndEndTimes,tmpBeginAndEndTimes[finalDistributeVehicleIdx],sizeof(tmpBeginAndEndTimes[finalDistributeVehicleIdx]));
                    memcpy(transmissionTimes,tmpTransmissionTimes[finalDistributeVehicleIdx],sizeof(tmpTransmissionTimes[finalDistributeVehicleIdx]));
                    memcpy(minPrevFinishTimes,tmpMinPrevFinishTimes[finalDistributeVehicleIdx],sizeof(tmpMinPrevFinishTimes[finalDistributeVehicleIdx]));
                    memcpy(earliestStartTimes,tmpEarliestStartTimes[finalDistributeVehicleIdx],sizeof(tmpEarliestStartTimes[finalDistributeVehicleIdx]));
                    memcpy(actualStartTimes,tmpActualStartTimes[finalDistributeVehicleIdx],sizeof(tmpActualStartTimes[finalDistributeVehicleIdx]));
                    memcpy(waitTimes,tmpWaitTimes[finalDistributeVehicleIdx],sizeof(tmpWaitTimes[finalDistributeVehicleIdx]));
                    memcpy(finishTimes,tmpFinishTimes[finalDistributeVehicleIdx],sizeof(tmpFinishTimes[finalDistributeVehicleIdx]));
                    memcpy(sequence,tmpSequence[finalDistributeVehicleIdx],sizeof(tmpSequence[finalDistributeVehicleIdx]));*/


        cout<<"re schedule success! "<<endl;
        cout<<"re schedule task "<<taskIdx+1<<" to vehicle "<<finalDistributeVehicleIdx+1<<endl;
        cout<<"re schedule result: "<<endl;
        for(int k=0;k<K;k++){
        cout<<"task "<<k+1<<" distribute to vehicle "<<scheduleRes[k]+1<<endl;
        cout<<"from time "<<beginAndEndTimes[k][0]<<" to "<<beginAndEndTimes[k][1]<<endl;
        cout<<"EST: "<<earliestStartTimes[k][scheduleRes[k]]<<endl;
        cout<<"AST: "<<actualStartTimes[k][scheduleRes[k]]<<endl;
        cout<<"Tc: "<<transmissionTimes[k][scheduleRes[k]]<<endl;
        cout<<"Tp: "<<computationTimes[k][scheduleRes[k]]<<endl;
        cout<<endl;
        }

        totalFinishTime-=0.000001;
        if(totalFinishTime<=deadline){
        reScheduleFlag=1;
        }
        }

        // 标记任务为已处理
        markQueue.push_back(task);
        unmarkQueue.erase(tmpTasks.begin()+maxIdx);

        cout<<"re schedule flag: "<<reScheduleFlag<<endl;
        cout<<endl;
        }
        cout<<"re schedule flag: "<<reScheduleFlag<<endl;
        cout<<endl;
        }
        }

        double totalCost=calculateTotalCost(scheduleRes,computationTimes,vehicles);

        cout<<"time based re schedule result: "<<endl;
        for(int k=0;k<K;k++){
        cout<<"task "<<k+1<<" distribute to vehicle "<<scheduleRes[k]+1<<endl;
        cout<<"from time "<<beginAndEndTimes[k][0]<<" to "<<beginAndEndTimes[k][1]<<endl;
        cout<<"EST: "<<earliestStartTimes[k][scheduleRes[k]]<<endl;
        cout<<"AST: "<<actualStartTimes[k][scheduleRes[k]]<<endl;
        cout<<"Tc: "<<transmissionTimes[k][scheduleRes[k]]<<endl;
        cout<<"Tp: "<<computationTimes[k][scheduleRes[k]]<<endl;
        cout<<endl;
        }
        cout<<"total cost: "<<totalCost<<endl;
        cout<<endl;

        // 5.考虑 cost 优化重调度
        // level 内的顺序为负载大小
        // 一个 level 的任务都完成重调度之后，再对后续 level 的任务的完成时间进行更新。
        // update 依据是 cost 变小且重新计算 Tc 后总 time<=D
        // 从完成时间从右到左开始选择其他分配，找到合适解就 update
        // 因此每次 update，cost 都在变小，比目前最优 cost 大的方案就舍弃。
        cout<<"cost re schedule: "<<endl;
        for(int i=0;i<levelNum;i++){
        cout<<"level "<<i+1<<": "<<endl;

        vector<Task> tasksInOneLevel=taskLevel[i];
        vector<Task> unmarkQueue=tasksInOneLevel;
        vector<Task> markQueue;

        while(unmarkQueue.size()!=0){
        // 找到目前负载最大的任务
        double maxComputation=0;
        int maxIdx=-1;
        for(int j=0;j<unmarkQueue.size();j++){
        if(unmarkQueue[j].computation>maxComputation){
        maxComputation=unmarkQueue[j].computation;
        maxIdx=j;
        }
        }

        Task task=unmarkQueue[maxIdx];
        int taskIdx=task.index-1;
        int distributeVehicleIdx=scheduleRes[taskIdx];
        double actualStartTimeBefore=actualStartTimes[taskIdx][distributeVehicleIdx];
        cout<<"re schedule task "<<taskIdx+1<<": "<<endl;
        vector<int> maxFinishTimeQueue;

        double maxAvailableFinishTime=0;
        double maxAvailableFinishTimeIdx=-1;
        double maxExpectedTotalCost=0;
        for(int j=0;j<M;j++){
        if(j==distributeVehicleIdx){
        continue;
        }
        cout<<"----------"<<endl;  //----------
        cout<<"re schedule to vehicle "<<j+1<<": "<<endl;



        scheduleRes[taskIdx]=j;
        int flag=1;
        finishDistributeTasks.clear();

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
        estimateTransmissionTime=tasks[k].transmission/getTransmissionRate(prevVehicleIdx,j);
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
        earliestStartTime=inputTransmissionTimeRSU;
        }
        if(minPrevFinishTime==100){
        minPrevFinishTime=inputTransmissionTimeRSU;
        }

        minPrevFinishTimes[taskIdx][j]=minPrevFinishTime;
        earliestStartTimes[taskIdx][j]=earliestStartTime;
        transmissionTimes[taskIdx][j]=transmissionTime;

        beginAndEndTimes[taskIdx][0]=minPrevFinishTimes[taskIdx][j];
//                        costs[taskIdx][j]=computationTimes[taskIdx][j]*vehicles[j].cost;

        cout<<"MPFT: "<<minPrevFinishTimes[taskIdx][j]<<endl;
        cout<<"EST: "<<earliestStartTimes[taskIdx][j]<<endl;
        cout<<"Tc: "<<transmissionTimes[taskIdx][j]<<endl;

        // 重分配遇到和 level 内其他任务分配到同一节点的排队情况，依据先来先服务原则，根据 EST 大小顺序进行处理
        tmpList.clear();
        waitList.clear();

        tmpList.push_back(taskIdx);
        for(int k=0;k<tasksInOneLevel.size();k++){
        if(tasksInOneLevel[k].index-1!=taskIdx&&scheduleRes[tasksInOneLevel[k].index-1]==j){
        tmpList.push_back(tasksInOneLevel[k].index-1);
        }
        }
        cout<<endl;
        cout<<"wait list: "<<endl;
        while(tmpList.size()!=0){
        double minEST=100;
        int minESTidx=-1;
        for(int k=0;k<tmpList.size();k++){
        if(earliestStartTimes[tmpList[k]][j]<minEST){
        minEST=earliestStartTimes[tmpList[k]][j];
        minESTidx=k;
        }
        }
        cout<<"task "<<tmpList[minESTidx]+1<<" ";
        cout<<"EST "<<earliestStartTimes[tmpList[minESTidx]][j]<<endl;

        waitList.push_back(tmpList[minESTidx]);
        tmpList.erase(tmpList.begin()+minESTidx);
        }
        cout<<endl;

        // waitList 必有一个元素
        double prevFinishTime=earliestStartTimes[waitList[0]][j];

        // 维护 sequence
        sequence[i][j].clear();
        for(int k=0;k<waitList.size();k++){
        int waitTaskIdx=waitList[k];
        cout<<"task "<<waitTaskIdx+1<<": "<<endl;

        // 排队为串行执行
        actualStartTimes[waitTaskIdx][j]=prevFinishTime;
        finishTimes[waitTaskIdx][j]=actualStartTimes[waitTaskIdx][j]+computationTimes[waitTaskIdx][j];
        waitTimes[waitTaskIdx][j]=actualStartTimes[waitTaskIdx][j]-earliestStartTimes[waitTaskIdx][j];

        prevFinishTime=finishTimes[waitTaskIdx][j];

        beginAndEndTimes[waitTaskIdx][1]=finishTimes[waitTaskIdx][j];

        sequence[i][j].push_back(waitTaskIdx);

        cout<<"AST: "<<actualStartTimes[waitTaskIdx][j]<<endl;
        cout<<"FT: "<<finishTimes[waitTaskIdx][j]<<endl;
        cout<<"WT: "<<waitTimes[waitTaskIdx][j]<<endl;
        }

        // 更新 AST FT 后再检查可用性
        for(int k=0;k<waitList.size();k++){
        int waitTaskIdx=waitList[k];
        // 该车辆必须是可用的
        if(!isVehicleAvailable(waitTaskIdx,j,minPrevFinishTimes,actualStartTimes,finishTimes,interval,scheduleRes,finishDistributeTasks)){
        flag=-1;
        }
        }

        cout<<"update sequence task "<<taskIdx+1<<" to vehicle "<<j+1<<": ";
        for(int k=0;k<sequence[i][j].size();k++){
        cout<<sequence[i][j][k]+1<<" ";
        }
        cout<<endl;

        // 更新其他任务
        // 原分配节点上排队的任务
        for(int k=0;k<tasksInOneLevel.size();k++){
        if(scheduleRes[tasksInOneLevel[k].index-1]==distributeVehicleIdx){
        cout<<endl;
        cout<<"before wait list on vehicle "<<distributeVehicleIdx+1<<": "<<endl;
        vector<int> sequenceOnBeforeVehicle=sequence[i][distributeVehicleIdx];
        // 重分配的任务后的任务向前移动一格
        for(int l=0;l<sequenceOnBeforeVehicle.size()-1;l++){
        if(sequenceOnBeforeVehicle[l]==taskIdx){
        double prevFinishTime=actualStartTimeBefore;
        for(int m=l+1;m<sequenceOnBeforeVehicle.size();m++){
        cout<<"task "<<sequenceOnBeforeVehicle[m]+1<<": "<<endl;
        actualStartTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx]=prevFinishTime;
        finishTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx]=actualStartTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx]+computationTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx];
        waitTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx]=actualStartTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx]-earliestStartTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx];

        prevFinishTime=finishTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx];

        beginAndEndTimes[sequenceOnBeforeVehicle[m]][1]=finishTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx];

        // 该车辆必须是可用的
        if(!isVehicleAvailable(sequenceOnBeforeVehicle[l],distributeVehicleIdx,minPrevFinishTimes,actualStartTimes,finishTimes,interval,scheduleRes,finishDistributeTasks)){
        flag=-1;
        }

        cout<<"AST: "<<actualStartTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx]<<endl;
        cout<<"FT: "<<finishTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx]<<endl;
        cout<<"WT: "<<waitTimes[sequenceOnBeforeVehicle[m]][distributeVehicleIdx]<<endl;
        }
        break;
        }
        }
        sequence[i][distributeVehicleIdx].clear();
        for(int l=0;l<sequenceOnBeforeVehicle.size()-1;l++){
        if(sequenceOnBeforeVehicle[l]!=taskIdx){
        sequence[i][distributeVehicleIdx].push_back(sequenceOnBeforeVehicle[l]);
        }
        }
        cout<<"before sequence task "<<taskIdx+1<<" to vehicle "<<distributeVehicleIdx+1<<": ";
        for(int k=0;k<sequence[i][distributeVehicleIdx].size();k++){
        cout<<sequence[i][distributeVehicleIdx][k]+1<<" ";
        }
        cout<<endl;

        break;
        }
        }

        tmpList.clear();
        waitList.clear();

                /*cout<<endl;
                cout<<"mark queue: "<<endl;
                for(int k=0;k<markQueue.size();k++){
                    cout<<"task "<<markQueue[k].index<<" FT: "<<finishTimes[markQueue[k].index-1][scheduleRes[markQueue[k].index-1]]<<endl;
                    // 已处理的任务完成时间仍小于 tx 或不能增加
                    if(!(finishTimes[markQueue[k].index-1][scheduleRes[markQueue[k].index-1]]<tx||finishTimes[markQueue[k].index-1][scheduleRes[markQueue[k].index-1]]<=finishTimesBefore[markQueue[k].index-1][scheduleRes[markQueue[k].index-1]])){
                        cout<<"mark queue fail!"<<endl;
                        flag=-1;
                    }
                }
*/
        // 一个level接一个level进行完成
        cout<<endl;
        cout<<"finish distribute tasks: ";
        for(int k=0;k<=i;k++){
        for(int l=0;l<taskLevel[k].size();l++){
        cout<<taskLevel[k][l].index<<" ";
        finishDistributeTasks.push_back(taskLevel[k][l].index-1);
        }
        }
        cout<<endl;

        // 重分配影响的是：1.同level的前后车辆 2.后续level的后继任务+同车上的排队等待
        cout<<endl;
        cout<<"follow up level: "<<endl;
        // 后续 level，按车辆上的排队顺序进行更新
        for(int l=i+1;l<levelNum;l++){
        cout<<"level "<<l+1<<": "<<endl;
        for(int m=0;m<M;m++){
        if(sequence[l][m].size()==0){
        continue;
        }
        cout<<"vehicle "<<m+1<<": "<<endl;
        // 更新 EST
        for(int n=0;n<sequence[l][m].size();n++){
        int followupTaskIdx=sequence[l][m][n];
        cout<<"task "<<followupTaskIdx+1<<": "<<endl;

        double followupMinPrevFinishTime=100;
        double followupEarliestStartTime=0;
        double followupTransmissionTime=0;
        for(int k=0;k<K;k++){
        // 考虑多前驱
        if(dependency[followupTaskIdx][k]>0){
        int prevVehicleIdx=scheduleRes[k];

        // 车辆需在 minPrevEndTime 时刻在 VC 范围内
        double prevFinishTime=finishTimes[k][scheduleRes[k]];
        if(prevFinishTime<followupMinPrevFinishTime){
        followupMinPrevFinishTime=prevFinishTime;
        }

        // 预估传输时间
        double estimateTransmissionTime=0;
        if(prevVehicleIdx!=m){
        // 前驱任务的传输量
        estimateTransmissionTime=tasks[k].transmission/getTransmissionRate(prevVehicleIdx,m);
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

        cout<<"MPFT: "<<minPrevFinishTimes[followupTaskIdx][m]<<endl;
        cout<<"EST: "<<earliestStartTimes[followupTaskIdx][m]<<endl;
        cout<<"Tc: "<<transmissionTimes[followupTaskIdx][m]<<endl;
        }
        cout<<endl;

        double prevFinishTime=earliestStartTimes[sequence[l][m][0]][m];
        for(int n=0;n<sequence[l][m].size();n++){
        int followupTaskIdx=sequence[l][m][n];
        cout<<"task "<<followupTaskIdx+1<<": "<<endl;

        actualStartTimes[followupTaskIdx][m]=prevFinishTime;

        double maxSameVehicleFinishTime=0;
        // 等待前level内同车的任务完成
        for(int o=0;o<l;o++){
        for(int p=0;p<sequence[o][m].size();p++){
        int sameVehicle=sequence[o][m][p];
        if(finishTimes[sameVehicle][scheduleRes[sameVehicle]]>maxSameVehicleFinishTime){
        maxSameVehicleFinishTime=finishTimes[sameVehicle][scheduleRes[sameVehicle]];
        }
        }
        }
        cout<<"max same vehicle finish time: "<<maxSameVehicleFinishTime<<endl;
        if(maxSameVehicleFinishTime>actualStartTimes[followupTaskIdx][m]){
        actualStartTimes[followupTaskIdx][m]=maxSameVehicleFinishTime;
        }

        finishTimes[followupTaskIdx][m]=actualStartTimes[followupTaskIdx][m]+computationTimes[followupTaskIdx][m];
        waitTimes[followupTaskIdx][m]=actualStartTimes[followupTaskIdx][m]-earliestStartTimes[followupTaskIdx][m];

        prevFinishTime=finishTimes[followupTaskIdx][m];

        beginAndEndTimes[followupTaskIdx][1]=finishTimes[followupTaskIdx][m];

        // 该车辆必须是可用的
        if(!isVehicleAvailable(followupTaskIdx,m,minPrevFinishTimes,actualStartTimes,finishTimes,interval,scheduleRes,finishDistributeTasks)){
        flag=-1;
        }

        cout<<"task "<<followupTaskIdx+1<<": "<<endl;
        cout<<"AST: "<<actualStartTimes[followupTaskIdx][m]<<endl;
        cout<<"FT: "<<finishTimes[followupTaskIdx][m]<<endl;
        cout<<"WT: "<<waitTimes[followupTaskIdx][m]<<endl;
        }
        cout<<endl;
        }
        }

        double expectedTotalFinishTime=0;
        for(int k=0;k<K;k++){
        if(finishTimes[k][scheduleRes[k]]>expectedTotalFinishTime){
        expectedTotalFinishTime=finishTimes[k][scheduleRes[k]];
        }
        }

        cout<<"expected total finish time: "<<expectedTotalFinishTime<<endl;

        double expectedTotalCost=calculateTotalCost(scheduleRes,computationTimes,vehicles);
        cout<<"expected total cost: "<<expectedTotalCost<<endl;

        cout<<"flag: "<<flag<<endl;
        cout<<endl;

        // 有多个节点的重分配方案可行
        if(expectedTotalCost<totalCost&&expectedTotalFinishTime<=deadline&&flag==1){
        // 取完成时间最大的
        if(finishTimes[taskIdx][j]>maxAvailableFinishTime){
        maxExpectedTotalCost=expectedTotalCost;
        maxAvailableFinishTime=finishTimes[taskIdx][j];
        maxAvailableFinishTimeIdx=j;
        }
        }

        if(flag==1){
        // 记录分配到此车辆节点的数据
                    /*memcpy(tmpScheduleRes[j],scheduleRes,sizeof(scheduleRes));
                    memcpy(tmpBeginAndEndTimes[j],beginAndEndTimes,sizeof(beginAndEndTimes));
                    memcpy(tmpTransmissionTimes[j],transmissionTimes,sizeof(transmissionTimes));
                    memcpy(tmpMinPrevFinishTimes[j],minPrevFinishTimes,sizeof(minPrevFinishTimes));
                    memcpy(tmpEarliestStartTimes[j],earliestStartTimes,sizeof(earliestStartTimes));
                    memcpy(tmpActualStartTimes[j],actualStartTimes,sizeof(actualStartTimes));
                    memcpy(tmpWaitTimes[j],waitTimes,sizeof(waitTimes));
                    memcpy(tmpFinishTimes[j],finishTimes,sizeof(finishTimes));
                    memcpy(tmpSequence[j],sequence,sizeof(sequence));*/

        }

        // 还原

        }

        if(maxAvailableFinishTimeIdx>-1){
        // 更新任务分配
                /*memcpy(scheduleRes,tmpScheduleRes[finalDistributeVehicleIdx],sizeof(tmpScheduleRes[finalDistributeVehicleIdx]));
                memcpy(beginAndEndTimes,tmpBeginAndEndTimes[finalDistributeVehicleIdx],sizeof(tmpBeginAndEndTimes[finalDistributeVehicleIdx]));
                memcpy(transmissionTimes,tmpTransmissionTimes[finalDistributeVehicleIdx],sizeof(tmpTransmissionTimes[finalDistributeVehicleIdx]));
                memcpy(minPrevFinishTimes,tmpMinPrevFinishTimes[finalDistributeVehicleIdx],sizeof(tmpMinPrevFinishTimes[finalDistributeVehicleIdx]));
                memcpy(earliestStartTimes,tmpEarliestStartTimes[finalDistributeVehicleIdx],sizeof(tmpEarliestStartTimes[finalDistributeVehicleIdx]));
                memcpy(actualStartTimes,tmpActualStartTimes[finalDistributeVehicleIdx],sizeof(tmpActualStartTimes[finalDistributeVehicleIdx]));
                memcpy(waitTimes,tmpWaitTimes[finalDistributeVehicleIdx],sizeof(tmpWaitTimes[finalDistributeVehicleIdx]));
                memcpy(finishTimes,tmpFinishTimes[finalDistributeVehicleIdx],sizeof(tmpFinishTimes[finalDistributeVehicleIdx]));
                memcpy(sequence,tmpSequence[finalDistributeVehicleIdx],sizeof(tmpSequence[finalDistributeVehicleIdx]));*/


        totalCost=maxExpectedTotalCost;

        cout<<"re schedule success! "<<endl;
        cout<<"re schedule task "<<taskIdx+1<<" to vehicle "<<maxAvailableFinishTimeIdx+1<<endl;
        cout<<"re schedule result: "<<endl;
        for(int k=0;k<K;k++){
        cout<<"task "<<k+1<<" distribute to vehicle "<<scheduleRes[k]+1<<endl;
        cout<<"from time "<<beginAndEndTimes[k][0]<<" to "<<beginAndEndTimes[k][1]<<endl;
        cout<<"EST: "<<earliestStartTimes[k][scheduleRes[k]]<<endl;
        cout<<"AST: "<<actualStartTimes[k][scheduleRes[k]]<<endl;
        cout<<"Tc: "<<transmissionTimes[k][scheduleRes[k]]<<endl;
        cout<<"Tp: "<<computationTimes[k][scheduleRes[k]]<<endl;
        cout<<endl;
        }
        cout<<"total cost: "<<totalCost<<endl;
        }

        // 标记任务为已处理
        markQueue.push_back(task);
        unmarkQueue.erase(unmarkQueue.begin()+maxIdx);
        }
        }

        // 输出最终结果
        cout<<endl;
        cout<<"final result: "<<endl;
        for(int i=0;i<K;i++){
        cout<<"task "<<i+1<<" distribute to vehicle "<<scheduleRes[i]+1<<endl;
        cout<<"from time "<<beginAndEndTimes[i][0]<<" to "<<beginAndEndTimes[i][1]<<endl;
        cout<<"EST: "<<earliestStartTimes[i][scheduleRes[i]]<<endl;
        cout<<"AST: "<<actualStartTimes[i][scheduleRes[i]]<<endl;
        cout<<"Tc: "<<transmissionTimes[i][scheduleRes[i]]<<endl;
        cout<<"Tp: "<<computationTimes[i][scheduleRes[i]]<<endl;
        cout<<endl;
        }
        cout<<endl;
        cout<<"total cost: "<<totalCost<<endl;

        // 将任务调度结果输入仿真器中

        return 0;
        }

// 仿真器
// cost 不变，关键是不超过 deadline
// 动态传输，车辆网络拓扑不断变化，实际任务传输过程中
/*int emulator(int scheduleRes[K],double beginAndEndTimes[K][2],double earliestStartTimes[K][2],double actualStartTimes[K][2],double transmissionTimes[K][2],double computationTimes[K][2]){


    return 0;
}*/






