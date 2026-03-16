package com.totalhealthdashboard.data.remote;

import com.google.gson.annotations.SerializedName;

public class NutritionResponse {
    @SerializedName("product")
    private Product product;

    public Product getProduct() { return product; }

    public static class Product {
        @SerializedName("product_name")
        private String productName;
        @SerializedName("nutriments")
        private Nutriments nutriments;

        public String getProductName() { return productName; }
        public Nutriments getNutriments() { return nutriments; }
    }

    public static class Nutriments {
        @SerializedName("energy-kcal_100g")
        private int calories;
        @SerializedName("proteins_100g")
        private double protein;
        @SerializedName("carbohydrates_100g")
        private double carbs;
        @SerializedName("fat_100g")
        private double fat;

        public int getCalories() { return calories; }
        public double getProtein() { return protein; }
        public double getCarbs() { return carbs; }
        public double getFat() { return fat; }
    }
}