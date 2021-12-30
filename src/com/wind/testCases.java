package com.wind;

public class testCases {
    public static void main(String[] args) {
        int dim = 6;
        for (int i = 0; i < 10; i++) {
            double[][] matrix = Main.GenRandomSquareMatrix(dim, 1, 4);
            System.out.println(Main.Matix2Str(matrix));
            double[][] car_time = Main.GenRandomCarInterval(dim, 0, 10, 10, 20);
            System.out.println(Main.Matix2Str(car_time));
        }
    }
}
