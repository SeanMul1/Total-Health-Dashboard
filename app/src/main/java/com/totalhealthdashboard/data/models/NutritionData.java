package com.totalhealthdashboard.data.models;

public class NutritionData {
    private String foodName;
    private int calories;
    private double protein;
    private double carbs;
    private double fat;

    public NutritionData(String foodName, int calories, double protein, double carbs, double fat) {
        this.foodName = foodName;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
    }

    public String getFoodName() { return foodName; }
    public int getCalories() { return calories; }
    public double getProtein() { return protein; }
    public double getCarbs() { return carbs; }
    public double getFat() { return fat; }
}