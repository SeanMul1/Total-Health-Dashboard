package com.totalhealthdashboard.data.models;

public class NutritionData {
    private String foodName;
    private int calories;
    private double protein;
    private double carbs;
    private double fat;
    private String imageUrl;

    public NutritionData(String foodName, int calories, double protein, double carbs, double fat) {
        this(foodName, calories, protein, carbs, fat, null);
    }

    public NutritionData(String foodName, int calories, double protein, double carbs, double fat, String imageUrl) {
        this.foodName = foodName;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.imageUrl = imageUrl;
    }

    public String getFoodName() { return foodName; }
    public int getCalories() { return calories; }
    public double getProtein() { return protein; }
    public double getCarbs() { return carbs; }
    public double getFat() { return fat; }
    public String getImageUrl() { return imageUrl; }

    public void setCalories(int calories) { this.calories = calories; }
    public void setProtein(double protein) { this.protein = protein; }
    public void setCarbs(double carbs) { this.carbs = carbs; }
    public void setFat(double fat) { this.fat = fat; }
}