package com.wind;

public class testCases {
    public static void main(String[] args) {
        int dim=6;
        int[][] matrix = Main.GenRandomSquareMatrix(dim,1,5);
        System.out.println(Main.Matix2Str(matrix));
        int [][] car_time  = Main.GenRandomCarInterval(dim,0,10,10,20);
        System.out.println(Main.Matix2Str(car_time));
    }
}
