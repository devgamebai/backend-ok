/*
 * Decompiled with CFR 0.152.
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class demo {
    public static List<List<String>> generateCombinations(List<List<String>> arrays) {
        ArrayList<List<String>> combinations = new ArrayList<List<String>>();
        demo.backtrackCombinations(new ArrayList<String>(), arrays, combinations, 0);
        return combinations;
    }

    private static void backtrackCombinations(List<String> currentCombination, List<List<String>> arrays, List<List<String>> combinations, int currentIndex) {
        if (currentIndex == arrays.size()) {
            combinations.add(new ArrayList<String>(currentCombination));
            return;
        }
        List<String> currentArray = arrays.get(currentIndex);
        for (String item : currentArray) {
            currentCombination.add(item);
            demo.backtrackCombinations(currentCombination, arrays, combinations, currentIndex + 1);
            currentCombination.remove(currentCombination.size() - 1);
        }
    }

    public static List<List<String>> generatePermutations(List<List<String>> combinations) {
        ArrayList<List<String>> permutations = new ArrayList<List<String>>();
        for (List<String> combination : combinations) {
            demo.backtrackPermutations(new ArrayList<String>(), combination, new boolean[combination.size()], permutations);
        }
        return permutations;
    }

    private static void backtrackPermutations(List<String> currentPermutation, List<String> combination, boolean[] used, List<List<String>> permutations) {
        if (currentPermutation.size() == combination.size()) {
            permutations.add(new ArrayList<String>(currentPermutation));
            return;
        }
        for (int i = 0; i < combination.size(); ++i) {
            if (used[i]) continue;
            currentPermutation.add(combination.get(i));
            used[i] = true;
            demo.backtrackPermutations(currentPermutation, combination, used, permutations);
            currentPermutation.remove(currentPermutation.size() - 1);
            used[i] = false;
        }
    }

    public static void main(String[] args) {
        ArrayList<List<String>> arrays = new ArrayList<List<String>>();
        arrays.add(Arrays.asList("A", "B"));
        arrays.add(Arrays.asList("X", "Y"));
        arrays.add(Arrays.asList("1", "2"));
        List<List<String>> combinations = demo.generateCombinations(arrays);
        List<List<String>> permutations = demo.generatePermutations(combinations);
        permutations.sort((p1, p2) -> {
            for (int i = 0; i < arrays.size(); ++i) {
                int index2;
                int index1 = ((List)arrays.get(i)).indexOf(p1.get(i));
                if (index1 == (index2 = ((List)arrays.get(i)).indexOf(p2.get(i)))) continue;
                return Integer.compare(index1, index2);
            }
            return 0;
        });
        for (List<String> permutation : permutations) {
            System.out.println(permutation);
        }
    }
}

