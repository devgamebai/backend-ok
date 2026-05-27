/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.model;

import java.util.ArrayList;
import java.util.List;

public class LotteryResult {
    private int countNumbers;
    private String time;
    private Results results;

    public int getCountNumbers() {
        return this.countNumbers;
    }

    public void setCountNumbers(int countNumbers) {
        this.countNumbers = countNumbers;
    }

    public String getTime() {
        return this.time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Results getResults() {
        return this.results;
    }

    public void setResults(Results results) {
        this.results = results;
    }

    public static class Results {
        private List<String> \u0110B;
        private List<String> G1;
        private List<String> G2;
        private List<String> G3;
        private List<String> G4;
        private List<String> G5;
        private List<String> G6;
        private List<String> G7;

        public List<String> get\u0110B() {
            return this.\u0110B;
        }

        public void set\u0110B(List<String> \u0110B) {
            this.\u0110B = \u0110B;
        }

        public List<String> getG1() {
            return this.G1;
        }

        public void setG1(List<String> g1) {
            this.G1 = g1;
        }

        public List<String> getG2() {
            return this.G2;
        }

        public void setG2(List<String> g2) {
            this.G2 = g2;
        }

        public List<String> getG3() {
            return this.G3;
        }

        public void setG3(List<String> g3) {
            this.G3 = g3;
        }

        public List<String> getG4() {
            return this.G4;
        }

        public void setG4(List<String> g4) {
            this.G4 = g4;
        }

        public List<String> getG5() {
            return this.G5;
        }

        public void setG5(List<String> g5) {
            this.G5 = g5;
        }

        public List<String> getG6() {
            return this.G6;
        }

        public void setG6(List<String> g6) {
            this.G6 = g6;
        }

        public List<String> getG7() {
            return this.G7;
        }

        public void setG7(List<String> g7) {
            this.G7 = g7;
        }

        public List<String> get27() {
            ArrayList<String> allResults = new ArrayList<String>();
            if (this.\u0110B != null) {
                allResults.addAll(this.\u0110B);
            }
            if (this.G1 != null) {
                allResults.addAll(this.G1);
            }
            if (this.G2 != null) {
                allResults.addAll(this.G2);
            }
            if (this.G3 != null) {
                allResults.addAll(this.G3);
            }
            if (this.G4 != null) {
                allResults.addAll(this.G4);
            }
            if (this.G5 != null) {
                allResults.addAll(this.G5);
            }
            if (this.G6 != null) {
                allResults.addAll(this.G6);
            }
            if (this.G7 != null) {
                allResults.addAll(this.G7);
            }
            return allResults;
        }

        public List<String> get24() {
            ArrayList<String> allResults = new ArrayList<String>();
            if (this.\u0110B != null) {
                allResults.addAll(this.\u0110B);
            }
            if (this.G1 != null) {
                allResults.addAll(this.G1);
            }
            if (this.G2 != null) {
                allResults.addAll(this.G2);
            }
            if (this.G3 != null) {
                allResults.addAll(this.G3);
            }
            if (this.G4 != null) {
                allResults.addAll(this.G4);
            }
            if (this.G5 != null) {
                allResults.addAll(this.G5);
            }
            if (this.G6 != null) {
                allResults.addAll(this.G6);
            }
            return allResults;
        }
    }
}

