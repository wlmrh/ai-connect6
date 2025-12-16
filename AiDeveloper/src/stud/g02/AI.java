package stud.g02;

import core.board.Board;
import core.board.PieceColor;
import core.game.Game;
import core.game.Move;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AI extends core.player.AI {
    private int steps = 0;
    private int[] nextStep = new int[]{0, 0};
    private int method = 1; // 1 : alpha-beta, 2 : TBS, 3 : Monte Carlo

    // todo: wlmrh
    @Override
    public Move findNextMove(Move opponentMove) {
        this.board.makeMove(opponentMove);
        Move nextMove;
        if (canWin()){
            nextMove = createMoveFromIndices(nextStep[0], nextStep[1]);
            this.board.makeMove(nextMove);
            return nextMove;
        } else if (needDefence()) {
            nextMove = createMoveFromIndices(nextStep[0], nextStep[1]);
            this.board.makeMove(nextMove);
            return nextMove;
        }

        if (Search(2) != 2){
            System.out.println("Find next move failed.\n");
            return createMoveFromIndices(0, 0);
        }

        nextMove = createMoveFromIndices(nextStep[0], nextStep[1]);
        this.board.makeMove(nextMove);
        return nextMove;
    }

    // 返回当前棋局下是否能直接获胜
    // 如果可以，将选择的两个位置索引写到 nextMove 中
    private boolean canWin() {
        PieceColor myColor = this.getBoard().whoseMove(); // 当前棋手
        PieceColor[] cells = board.get_board(); // 当前棋盘
        int[][] directions = Board.FORWARD; // 所有可能的六连方向

        for (int i = 0; i < 361; i++) {
            int col = i % 19;
            int row = i / 19;

            for (int[] dir : directions) {
                List<Integer> emptyIndices = new ArrayList<>();
                int sameColor = 0;
                boolean valid = true;

                for (int k = 0; k < 6; k++) {
                    int c = col + dir[0] * k;
                    int r = row + dir[1] * k;
                    if (c < 0 || c >= 19 || r < 0 || r >= 19) {
                        valid = false;
                        break;
                    }
                    int idx = r * 19 + c;
                    PieceColor p = cells[idx];

                    if (p == myColor) {
                        sameColor++;
                    } else if (p == PieceColor.EMPTY) {
                        emptyIndices.add(idx);
                    } else {
                        valid = false;
                        break;
                    }
                }

                if (valid) {
                    if (sameColor >= 4) {
                        nextStep[0] = emptyIndices.get(0);
                        // 如果形成一个四连，则填这两个关键点；如果形成一个五连，填当前关键点和任意一个其他点
                        nextStep[1] = (emptyIndices.size() > 1) ? emptyIndices.get(1) : getAnyEmpty(nextStep[0]);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 判断是否需要防守。
     * 策略：
     * 1. 扫描全盘威胁。
     * 2. 如果无威胁 -> return false。
     * 3. 如果单子能全防住 -> nextMove[0]=防守点, nextMove[1]=Search(1) -> return true。
     * 4. 如果单子防不住 -> 暴力搜索最佳两子防御组合 -> nextMove[0/1]=最佳点对 -> return true。
     */
    private boolean needDefence() {
        List<List<Integer>> allThreats = getAllThreatLines();

        if (allThreats.isEmpty()) {
            return false;
        }

        // 寻找能覆盖最多威胁的单点
        int bestSinglePoint = findBestSinglePoint(allThreats);

        // 检查这个点是否覆盖了所有威胁
        if (coversAllThreats(bestSinglePoint, allThreats)) {
            // 一手棋就能防守住
            nextStep[0] = bestSinglePoint;

            if (Search(1) == 0) {
                nextStep[1] = getAnyEmpty(nextStep[0]);
            }

        } else {
            // 必须两步棋全部用于防守
            // 启用暴力枚举，寻找覆盖率最高的防御点对
            int[] pair = getBestPairBruteForce(allThreats);
            nextStep[0] = pair[0];
            nextStep[1] = pair[1];
        }
        return true;
    }

    // 辅助函数
    private int[] getBestPairBruteForce(List<List<Integer>> threats) {
        // 将所有威胁点提取到 candidates
        Set<Integer> candidates = new HashSet<>();
        for (List<Integer> line : threats) {
            candidates.addAll(line);
        }
        List<Integer> candidateList = new ArrayList<>(candidates);

        if (candidateList.size() < 2) {
            System.out.println("Can't defend with one step, but only have one candidate.\n");
            return new int[]{candidateList.get(0), getAnyEmpty(candidateList.get(0))};
        }

        int[] bestPair = new int[2];
        int maxCovered = -1;

        for (int i = 0; i < candidateList.size(); i++) {
            for (int j = i + 1; j < candidateList.size(); j++) {
                int p1 = candidateList.get(i);
                int p2 = candidateList.get(j);

                int covered = countCovered(threats, p1, p2);

                if (covered > maxCovered) {
                    maxCovered = covered;
                    bestPair[0] = p1;
                    bestPair[1] = p2;

                    if (maxCovered == threats.size()) {
                        return bestPair;
                    }
                }
            }
        }
        return bestPair;
    }

    private List<List<Integer>> getAllThreatLines() {
        List<List<Integer>> lines = new ArrayList<>();
        PieceColor oppColor = this.getBoard().whoseMove().opposite();
        PieceColor[] cells = board.get_board();
        int[][] directions = Board.FORWARD;

        for (int i = 0; i < 361; i++) {
            int col = i % 19;
            int row = i / 19;
            for (int[] dir : directions) {
                List<Integer> emptyIndices = new ArrayList<>();
                int oppCount = 0;
                boolean valid = true;

                for (int k = 0; k < 6; k++) {
                    int c = col + dir[0] * k;
                    int r = row + dir[1] * k;
                    if (c < 0 || c >= 19 || r < 0 || r >= 19) {
                        valid = false; break;
                    }
                    int idx = r * 19 + c;
                    if (cells[idx] == oppColor) oppCount++;
                    else if (cells[idx] == PieceColor.EMPTY) emptyIndices.add(idx);
                    else { valid = false; break; }
                }

                if (valid && oppCount >= 4) {
                    lines.add(emptyIndices);
                }
            }
        }
        return lines;
    }

    private int findBestSinglePoint(List<List<Integer>> threats) {
        int[] scores = new int[361];
        int bestIdx = -1;
        int maxScore = -1;

        for (List<Integer> line : threats) {
            // 冲五时，将权重调整为正无穷，优先处理
            int weight = (line.size() == 1) ? 100 : 1;
            for (int idx : line) {
                scores[idx] += weight;
                if (scores[idx] > maxScore) {
                    maxScore = scores[idx];
                    bestIdx = idx;
                }
            }
        }
        if (bestIdx == -1 && !threats.isEmpty()) return threats.get(0).get(0);
        return bestIdx;
    }

    private boolean coversAllThreats(int p1, List<List<Integer>> threats) {
        for (List<Integer> line : threats) {
            if (!line.contains(p1)) return false;
        }
        return true;
    }

    private int countCovered(List<List<Integer>> threats, int p1, int p2) {
        int count = 0;
        for (List<Integer> line : threats) {
            if (line.contains(p1) || line.contains(p2)) {
                count++;
            }
        }
        return count;
    }

    // 返回当前棋盘上除了 avoidIdx 索引外的任意一个 EMTPY 位置索引
    // 如果没有找到，返回 -1
    private int getAnyEmpty(int avoidIdx) {
        PieceColor[] cells = board.get_board();
        // 优先找 avoidIdx 周围的空位
        int[] nearby = {-1, 1, -19, 19, -20, 20, -18, 18};
        for (int offset : nearby) {
            int target = avoidIdx + offset;
            if (target >= 0 && target < 361 && cells[target] == PieceColor.EMPTY) {
                return target;
            }
        }

        for (int i = 0; i < 361; i++) {
            if (i != avoidIdx && cells[i] == PieceColor.EMPTY) return i;
        }
        return -1;
    }

    // 给定当前两步棋选择的位置索引，返回对应的 Move 对象
    private Move createMoveFromIndices(int idx1, int idx2) {
        char c1 = indexToCol(idx1);
        char r1 = indexToRow(idx1);
        char c2 = indexToCol(idx2);
        char r2 = indexToRow(idx2);
        return new Move(c1, r1, c2, r2);
    }

    private char indexToCol(int index) {
        int val = index % 19;
        return (char) ('A' + val + (val >= 8 ? 1 : 0));
    }

    private char indexToRow(int index) {
        int val = index / 19;
        return (char) ('A' + val + (val >= 8 ? 1 : 0));
    }

    // todo: hethtina
    // type 表示 Search 需要生成的步数(1 / 2)
    // 将选择位置的索引写到 nextMove 数组中
    // type = 1时，将答案写到 nextMove[1] 中
    // 返回成功生成的步数
    private int Search(int type){
        return 1;
    }

    public String name() {
        return "G02";
    }

    @Override
    public void playGame(Game game) {
        super.playGame(game);
        board = new Board();
        steps = 0;
    }
}
