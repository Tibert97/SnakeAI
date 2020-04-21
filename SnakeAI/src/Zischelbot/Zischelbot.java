package Zischelbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

public class Zischelbot implements Bot {
    class Board{
        Snake player;
        Snake opponent;
        Coordinate maze_size;
        Coordinate apple;
        public Board(Snake player, Snake opponent, Coordinate maze_size, Coordinate apple){
            this.player = player;
            this.opponent = opponent;
            this.maze_size = maze_size;
            this.apple = apple;
        }
    }
    class Game_Tree{
        Board root;
        Game_Tree parent;
        ArrayList<Game_Tree> children;
        int visited;
        int value;
        Direction last_action;
        int turn;
        public Game_Tree(Board board, Game_Tree parent, int turn, Direction last_action){
            this.root = board;
            this.children = null;
            this.parent = parent;
            this.visited = 0;
            this.value = 0;
            this.turn = turn;
            this.last_action = last_action;
        }

        private Game_Tree maximum_upper_confidence_bound(ArrayList<Game_Tree> children){
            double upper_confidence_bound = 0;
            Game_Tree best_child = null;
            for (Game_Tree child : children) {
                if(child.upper_confidence_bound() > upper_confidence_bound){
                    upper_confidence_bound = child.upper_confidence_bound();
                    best_child = child;
                }
            }
            return best_child;
        }

        private Direction[] valid_moves(Snake player){
            Coordinate head = player.getHead();

            /* Get the coordinate of the second element of the snake's body
            * to prevent going backwards */
            Coordinate afterHeadNotFinal = null;
            if (player.body.size() >= 2) {
                Iterator<Coordinate> it = player.body.iterator();
                it.next();
                afterHeadNotFinal = it.next();
            }

            final Coordinate afterHead = afterHeadNotFinal;

            /* The only illegal move is going backwards. Here we are checking for not doing it */
            Direction[] validMoves = Arrays.stream(DIRECTIONS)
                    .filter(d -> !head.moveTo(d).equals(afterHead)) // Filter out the backwards move
                    .sorted()
                    .toArray(Direction[]::new);
        }

        public Game_Tree selection(){
            Game_Tree selected_node = this;
            while(selected_node.children != null){
                selected_node = maximum_upper_confidence_bound(this.children);
            }
            return selected_node;
        }

        public void expansion(){
            this.children = new ArrayList<Game_Tree>();
            for (Direction d : valid_moves(this.root.player)) {
                Game_Tree tmp_tree = new Game_Tree(do_action_copied(this.root,d),this,this.turn*-1,d);
                this.children.add(tmp_tree);
            }

        }

        public int rollout(){
            int turn = this.turn;
            Board board = this.root;
            while(reward_of_state(board) == 0){
                Direction random_action = valid_moves(this.root.player)[(int)(Math.random()*3)];
                board = do_action(board,random_action);
            }
            return reward_of_state(board,this.root.player);
        }

        public void backpropagation(int reward){
            Game_Tree current = this;
            while(current != null){
                current.visited += 1;
                current.value += reward;
                current = current.parent;
            }
        }

        private double upper_confidence_bound(){
            if(this.visited == 0){
                return Double.POSITIVE_INFINITY;
            }
            else{
                return this.value/this.visited + Math.sqrt(2)* Math.sqrt(Math.log(this.parent.visited)/this.visited);
            }
        }
    }


    private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

    private double euclidian_distance(Coordinate a, Coordinate b){
        return Math.sqrt(Math.pow((a.x-b.x),2)+Math.pow((a.y-b.y),2));
    }

    @Override
    public Direction chooseDirection(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        return monte_carlo_tree_search(snake, opponent, mazeSize, apple);
    }

    public Direction monte_carlo_tree_search(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple){
        Board root_board = new Board(snake,opponent,mazeSize,apple);
        Game_Tree root = new Game_Tree(root_board,null,1,null);
        while(true){
            Game_Tree current = root.selection();
            if(current.visited == 0){
                int result = current.rollout();
                current.backpropagation(result);
            }
            else{
                current.expansion();
                int result = current.rollout();
                current.backpropagation(result);
            }
        }

        Direction best_move = null;
        int highest_visit = 0;
        for (Game_Tree child : root.children) {
            if(child.visited > highest_visit){
                best_move = child.last_action;
                highest_visit = child.visited;
            }
        }
        return best_move;
    }
}