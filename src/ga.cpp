#include <iostream>
#include <vector>
#include <ctime>
#include <cstdlib>
#include <cmath>

using namespace std;

// 车辆数
#define M 6

// 任务数
//#define K 5
#define K 14

// GA 相关
#define population 10
#define roundStop 5
#define pCrossover 0.9
#define pMutation 0.9


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

/*// 根据公式 21，用整数编码 theta 的值，可以得到某个子任务对应车辆指标 v 和时间槽指标 h
int getV(int theta){
    return theta/H+1;
}

int getH(int theta){
    return (theta%H==0)?H:theta%H;
}

// 根据公式 21 倒推 theta
int getTheta(int v, int h){
    if(h%H!=0) {
        return (v-1)*H+h;     		//*********倒推的目的是为了初始化种群？除了v=1的情况，这里计算的theta都是大于H，theta小于H的情况也要考虑
    }
    return (v-1)*H;
}

// 根据 h 得到任务完成时间，单位为 tu
double getTu(int h){                    //*********我简单分析了一下，计算方法应该是对的，但我不确定，你需要用几个例子测速一下，程序把h输出后，你可以手动计算完成时间。
    int sum=0;
    int interval=(1+gamma)*gamma/2;
    sum+=(h/gamma)*interval;
    for (int i = 0; i < h%gamma; i++) {
        sum+=(i+1);
    }
    return sum*tu;
}

// 不同跳数计算通讯时间，一跳默认为 0.5*tu，与论文中不一样，论文中没有算上通讯时间
double getC(int single[],int index){
    if(index==d-1) {
        return 0;
    }
    double c=0;
    c+=abs(getV(single[index+1])-getV(single[index]))*0.5*tu;		//*********在GA算法中，这里应该用节点间（对应不同节点，如果是同一节点就不需要传输）的初始距离查找对应带宽，然后用数据量除以带宽，获得传输时间
    return c;
}*/

// 根据公式 15 计算 fitness
// 需要大改，以 cost 为目标
double getFitness(int single[]){
    double maxFitness1=tu;
    double maxFitness2=tu;
    for (int i=0;i<dk;i++) {
        maxFitness1 = max(maxFitness1,getTu(getH(single[i])));		//*********首先要判断当前的个体是否fesiable，可行解计算fitness才有意义，因为乱序执行也能计算fitness，但没有意义。
    }
//    cout<<maxFitness1<<endl;
    for (int i=dk;i<2*dk;i++) {
        maxFitness2 = max(maxFitness2,getTu(getH(single[i])));		//*********如果当前的个体不可行，则将其fitness赋一个较大的数值，比如100000000000。
    }
//    cout<<maxFitness2<<endl;
    return (maxFitness1+maxFitness2)/2;
}

// 根据公式 16-20 这些约束条件判断后代是否可行
// 以 time 为依据
bool isFeasible(int single[],vector<int> thetaValue,Vehicle vehicles[],Mission missions[]){
    // 公式 16，theta 的取值要是可行解
    for (int i = 0; i < d; i++) {
        bool flag=false;
        for (int j = 0; j < thetaValue.size(); j++) {
            if(single[i]==thetaValue[j]) {
                flag=true;
                break;
            }
        }
        if(!flag) {
            return false;
        }
    }
    // 且 theta 都要唯一
    for (int i = 0; i < d; i++) {
        for (int j = i + 1; j < d; j++) {
            if (single[i] == single[j]) {
                return false;
            }
        }
    }
    // 前后两个任务不能在同一个时间槽内，或者后一个任务的时间槽在前一个任务之前
    for (int i = 0; i < d-1; i++) {
        for (int j = i + 1; j < d; j++) {
            if(i/dk==j/dk&&getH(single[i])>=getH(single[j])) {  	//*********“或”条件应该用 ||，如何判断乱序，现在这个方法好像不完备，你现在这个方法默认任务i＋1是任务i的后继，但不同的任务调用图（拓扑）有不同的情况，任务i＋1和任务i可能是独立的关系，可并行执行的。所以，应该根据输入的任务调用图来判断当前任务j和它的前驱任务（可能是多个任务，即它们的输出是任务j的输入）的时间关系。
                return false;
            }
        }
    }

    // 公式 17，对比通讯时间+处理时间，和时间槽大小
    for (int i = 0; i < d; i++) {
        int theta=single[i];
        int v = getV(theta);
        int h = getH(theta);
        double p=0;
        // 计算处理时间 p
        if(i==0){
            p=(double)missions[0].omega1/vehicles[v-1].fn;
        }else if(i==1){
            p=(double)missions[0].omega2/vehicles[v-1].fn;
        }else if(i==2){
            p=(double)missions[1].omega1/vehicles[v-1].fn;
        }else{
            p=(double)missions[1].omega2/vehicles[v-1].fn;
        }

        // 计算通讯时间 c
        double c=getC(single,i);

        // 计算时间间隙 s，注意单位为 tu
        double s=(h%gamma==0)?gamma:h%gamma;
        s*=tu;
//        cout<<"s: "<<s<<" c: "<<c<<" p: "<<p<<endl;
        if(c+p>s) {
            return false;
        }
    }

    return true;
}

// 在可行解中随机突变
void mutation(int single[],vector<int> thetaValue){
    // 对 theta 突变
    double p1=rand()%1000000/(double)1000000;
    // 对 task 突变
    double p2=rand()%1000000/(double)1000000;
    /*cout<<"in mutation: "<<endl;
    cout<<"p1: "<<p1<<endl;
    cout<<"p2: "<<p2<<endl;*/
    int index=0;
    double tmp=(double)1/thetaValue.size();
    while(p1>tmp) {
        tmp+=(double)1/thetaValue.size();
        index++;
    }
    int theta=thetaValue[index];
//    cout<<"theta: "<<theta<<endl;
    index=0;
    tmp=(double)1/d;
    while(p2>tmp) {
        tmp+=(double)1/d;
        index++;
    }
//    cout<<"index: "<<index<<endl;
    single[index]=theta;
    /*cout<<"res: "<<endl;
    for (int i = 0; i < d; i++) {
        cout<<single[i]<<" ";
    }
    cout<<endl;*/
}

int main() {
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

    // 编码，任务对应车辆，cost


    cout<<"thetaValue: "<<endl;
    for(int i=0;i<thetaValue.size();i++){
        cout<<thetaValue[i]<<" ";
    }
    cout<<endl;



    // 随机初始种群为 population*D
    int a[population][d] = {{ 3,12,15,24},
                            { 4,14,15,24},
                            {13,15, 4, 6},
                            {20,31,12, 6}};

    // 设置随机数种子，使每次产生的随机序列不同
    srand(time(NULL));		//*********time(NULL)可能是一个定值，之前有学生好像测试过，你可以每次都输出time(NULL)，如果其数值相同，则需要把它替换成其他变量k，变量k后边进行＋＋操作就可以了。
    for (int i = 0; i < roundStop; i++) {
        cout<<endl;
        cout<<"---round"<<i<<"---"<<endl;

        // 算法第四行
        double fitness[population];
        for (int j = 0; j < population; j++) {
            fitness[j] = getFitness(a[j]);
        }
        cout<<"fitness: "<<endl;
        for (int j = 0; j < population; j++) {
            cout<<fitness[j]<<" ";
        }
        cout<<endl;
        // 算法第五行，使用选择排序
        for (int j = 0; j < population-1; j++) {
            int max=j;
            for (int k = j + 1; k < population; k++) {
                if(fitness[k]>fitness[max]) {
                    max=k;
                }
            }
            // 交换 a 和 fitness
            for (int k = 0; k < d; k++) {
                int tmp=a[j][k];
                a[j][k]=a[max][k];
                a[max][k]=tmp;
            }
            double tmpFitness=fitness[j];
            fitness[j]=fitness[max];
            fitness[max]=tmpFitness;
        }
        // 输出种群
        cout<<"a: "<<endl;
        for (int j = 0; j < population; j++) {
            for (int k = 0; k < d; k++) {
                cout<<a[j][k]<<" ";
            }
            cout<<endl;
        }

        // 算法第六行，输出每一代的最优解
        cout<<"elite: "<<endl;
        for (int j = 0; j < d; j++) {
            cout<<a[population-1][j]<<" ";
        }
        cout<<endl;

        // 一轮中父辈+产生的后代
        int total[4*population][d];
        int count=0;

        // 根据 roulette selection，各个个体被选中的概率与其适应度大小成正比，得到每个个体对应的概率
        double probability[population];
        double sumFitness=0;
        for (int j = 0; j < population; j++) {
            sumFitness+=fitness[j];
        }
        cout<<"probability: "<<endl;
        for (int j = 0; j < population; j++) {
            probability[j]=(fitness[j]/sumFitness);
            cout<<probability[j]<<" ";
        }
        cout<<endl;

        // 算法第七行开始
        for (int j = 0; j < population; j++) {
            cout<<"---population"<<j+1<<"---"<<endl;

            // 算法第八行，roulette selection
            // 产生随机数
            double pFemale=rand()%1000000/(double)1000000;
            double pMale=rand()%1000000/(double)1000000;
            cout<<"p: "<<pFemale<<" "<<pMale<<endl;
            int indexFemale=0;
            int indexMale=0;
            double curProbability=0;
            for (int k = 0; k < population; k++) {
                curProbability+=probability[k];
                if(pFemale<curProbability) {
                    indexFemale=k;
                    break;
                }
            }
            curProbability=0;
            for (int k = 0; k < population; k++) {
                curProbability+=probability[k];
                if(pMale<curProbability) {
                    indexMale=k;
                    break;
                }
            }
            cout<<"aFemale: "<<endl;
            for (int k = 0; k < d; k++) {
                cout<<a[indexFemale][k]<<" ";
            }
            cout<<endl;
            cout<<"aMale: "<<endl;
            for (int k = 0; k < d; k++) {
                cout<<a[indexMale][k]<<" ";
            }
            cout<<endl;

            int female[d];
            for (int k = 0; k < d; k++) {
                female[k]=a[indexFemale][k];
                total[count][k]=a[indexFemale][k];
            }
            count++;
            int male[d];
            for (int k = 0; k < d; k++) {
                male[k]=a[indexMale][k];
                total[count][k]=a[indexMale][k];
            }
            count++;

            // 算法第九行开始，poscut 以后的任务，需要将父母的 theta 互换，d 为总的任务数
            vector<int> poset;
            for (int k = 0; k < d-1; k++) {
                cout<<"---poscut"<<k+1<<"---"<<endl;

                int curFemale[d];
                for (int l = 0; l < d; l++) {
                    curFemale[l]=female[l];
                }
                int curMale[d];
                for (int l = 0; l < d; l++) {
                    curMale[l]=male[l];
                }

                // crossover
                for (int l = k + 1; l < d; l++) {
                    int tmp=curFemale[l];
                    curFemale[l]=curMale[l];
                    curMale[l]=tmp;
                }

                // 产生两个后代
                cout<<"curFemale: "<<endl;
                for (int l = 0; l < d; l++) {
                    cout<<curFemale[l]<<" ";
                }
                cout<<endl;
                cout<<"curMale: "<<endl;
                for (int l = 0; l < d; l++) {
                    cout<<curMale[l]<<" ";
                }
                cout<<endl;

                // 检验后代可行性
                if(isFeasible(curFemale,thetaValue,vehicles,missions)&&
                   isFeasible(curMale,thetaValue,vehicles,missions)) {
                    poset.push_back(k);
                }
            }
            cout<<"poset: "<<endl;
            for (int k = 0; k < poset.size(); k++) {
                cout<<poset[k]<<" ";
            }
            cout<<endl;

            // 算法15行
            double p1=rand()%1000000/(double)1000000;
            cout<<"p1: "<<p1<<endl;
            if (p1 <= pCrossover && poset.size()!=0) {
                // 算法17行
                double pAny=rand()%1000000/(double)1000000;
                cout<<"pAny: "<<pAny<<endl;
                int index=0;
                double tmp=(double)1/poset.size();
                while(pAny>tmp) {
                    tmp+=(double)1/poset.size();
                    index++;
                }
                int pos=poset[index];
                cout<<"pos: "<<pos<<endl;

                // crossover
                int curFemale[d];
                for (int l = 0; l < d; l++) {
                    curFemale[l]=female[l];
                }
                int curMale[d];
                for (int l = 0; l < d; l++) {
                    curMale[l]=male[l];
                }
                for (int k = pos+1; k < d; k++) {
                    int tmp=curFemale[k];
                    curFemale[k]=curMale[k];
                    curMale[k]=tmp;
                }

                // 交叉后的结果
                cout<<"after crossover: "<<endl;
                cout<<"curFemale: "<<endl;
                for (int l = 0; l < d; l++) {
                    cout<<curFemale[l]<<" ";
                }
                cout<<endl;
                cout<<"curMale: "<<endl;
                for (int l = 0; l < d; l++) {
                    cout<<curMale[l]<<" ";
                }
                cout<<endl;

                // 算法19行
                double p2=rand()%1000000/(double)1000000;
                cout<<"p2: "<<p2<<endl;
                if (p2 <= pMutation) {
                    // 变异
                    mutation(curFemale,thetaValue);
                    mutation(curMale,thetaValue);

                    cout<<"after mutation: "<<endl;
                    cout<<"curFemale: "<<endl;
                    for (int l = 0; l < d; l++) {
                        cout<<curFemale[l]<<" ";
                    }
                    cout<<endl;
                    cout<<"curMale: "<<endl;
                    for (int l = 0; l < d; l++) {
                        cout<<curMale[l]<<" ";
                    }
                    cout<<endl;

                    // modification 改为了产生的后代和父辈共同排序，选择适应度高的个体
                    if(isFeasible(curFemale,thetaValue,vehicles,missions)) {
                        for (int l = 0; l < d; l++) {
                            total[count][l]=curFemale[l];
                        }
                        count++;
                    }
                    if(isFeasible(curMale,thetaValue,vehicles,missions)) {
                        for (int l = 0; l < d; l++) {
                            total[count][l]=curMale[l];
                        }
                        count++;
                    }
                }
            }
        }
        cout<<"total: "<<endl;
        for(int j=0;j<count;j++){
            for (int k = 0; k < d; k++) {
                cout<<total[j][k]<<" ";
            }
            cout<<endl;
        }

        // 去重
        for(int j=0;j<count;j++){
            for (int k = j+1; k < count; k++) {
                if(total[j][0]==total[k][0]&&total[j][1]==total[k][1]&&total[j][2]==total[k][2]&&total[j][3]==total[k][3]) {
                    for (int l = 0; l < d; l++) {
                        total[k][l]=24;
                    }
                }
            }
        }

        cout<<"unique total: "<<endl;
        for(int j=0;j<count;j++){
            for (int k = 0; k < d; k++) {
                cout<<total[j][k]<<" ";
            }
            cout<<endl;
        }

        // 按适应度升序排序
        double fitness2[count];
        for (int j = 0; j < count; j++) {
            fitness2[j] = getFitness(total[j]);
        }
        cout<<"fitness: "<<endl;
        for (int j = 0; j < count; j++) {
            cout<<fitness2[j]<<" ";
        }
        cout<<endl;

        for (int j = 0; j < count-1; j++) {
            int min=j;
            for (int k = j + 1; k < count; k++) {
                if(fitness2[k]<fitness2[min]) {
                    min=k;
                }
            }
            // 交换 total 和 fitness
            for (int k = 0; k < d; k++) {
                int tmp=total[j][k];
                total[j][k]=total[min][k];
                total[min][k]=tmp;
            }
            double tmpFitness=fitness2[j];
            fitness2[j]=fitness2[min];
            fitness2[min]=tmpFitness;
        }

        // 输出种群
        cout<<"next generation a: "<<endl;
        // 只有前 population 可以进入下一代
        for (int j = 0; j < population; j++) {
            for (int k = 0; k < d; k++) {
                a[j][k]=total[j][k];
                cout<<a[j][k]<<" ";
            }
            cout<<endl;
        }
//        break;
    }

    // 输出最终结果
    cout<<endl;
    cout<<"final result: "<<endl;
    cout<<"theta: "<<endl;
    for (int i = 0; i < d; i++) {
        cout<<a[0][i]<<" ";
    }
    cout<<endl;
    for (int i = 0; i < d; i++) {
        double p;
        if(i==0){
            p=(double)missions[0].omega1/vehicles[getV(a[0][i])-1].fn;
        }else if(i==1){
            p=(double)missions[0].omega2/vehicles[getV(a[0][i])-1].fn;
        }else if(i==2){
            p=(double)missions[1].omega1/vehicles[getV(a[0][i])-1].fn;
        }else{
            p=(double)missions[1].omega2/vehicles[getV(a[0][i])-1].fn;
        }
        double c=getC(a[0],i);

        cout<<endl;
        cout<<"mission"<<i/dk+1<<" "<<"task"<<i%dk+1<<":"<<endl;
        cout<<"H:"<<getH(a[0][i])<<" "<<"V:"<<getV(a[0][i])<<endl;
        cout<<"process time: "<<p<<endl;
        cout<<"communication time: "<<c<<endl;
    }
    cout<<endl;
    cout<<"fitness: "<<getFitness(a[0])<<endl;
    cout<<endl;

    return 0;
}





