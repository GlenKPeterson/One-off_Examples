// Copyright 2014 Glen Peterson.  Distributed under Apache 2.0 License.

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class ContextRel {
    interface Sugars {
        int gramsSugar();
        public static final Comparator<Sugars> COMPARATOR = (left, right) ->
                left.gramsSugar() - right.gramsSugar();
    }

    interface Heating {
        int cookingEnergy();
        public static final Comparator<Heating> COMPARATOR = (left, right) ->
                left.cookingEnergy() - right.cookingEnergy();
    }

    static class Food implements Heating, Sugars {
        private final int gramsSugar;
        private final int cookingEnergy;
        private Food(int g, int c) { gramsSugar = g; cookingEnergy = c; }
        @Override public int gramsSugar() { return gramsSugar; }
        @Override public int cookingEnergy() { return cookingEnergy; }
        @Override public String toString() {
            return "Food(" + gramsSugar + "," + cookingEnergy + ")";
        }
    }

    public static void main(String[] args) {
        List<Food> foods = Arrays.asList(new Food(5,3), new Food(4,4));
        SortedSet<Food> foodsByHeat = new TreeSet<>(Heating.COMPARATOR);
        foodsByHeat.addAll(foods);
        System.out.println("Foods by heat:");
        for (Food f : foodsByHeat) { System.out.println(f); }

        SortedSet<Food> foodsBySugar = new TreeSet<>(Sugars.COMPARATOR);
        foodsBySugar.addAll(foods);
        System.out.println("Foods by sugar:");
        for (Food f : foodsBySugar) { System.out.println(f); }
    }
}
