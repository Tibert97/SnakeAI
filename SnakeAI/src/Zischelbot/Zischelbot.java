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

        Board copy(){
            Snake player = this.player.clone();
            Snake opponent = this.opponent.clone();
            Coordinate maze_size = new Coordinate(this.maze_size.x, this.maze_size.y);
            Coordinate apple = new Coordinate(this.apple.x, this.apple.y);
            Board new_board = new Board(player, opponent, maze_size, apple);
            return new_board;
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
            Game_Tree best_child = children.get(0);
            for (Game_Tree child : children) {
                if(child.upper_confidence_bound() > upper_confidence_bound){
                    upper_confidence_bound = child.upper_confidence_bound();
                    best_child = child;
                }
            }
            return best_child;
        }

        private Board do_action_copied(Board board, Direction action, Snake player){
            boolean grow = false;
            Coordinate new_head = new Coordinate(player.getHead().x+action.dx, player.getHead().y+action.dy);
            if(board.apple.equals(new_head)){
                grow = true;
            }
            Snake moved_player = board.player.clone();
            moved_player.moveTo(action, grow);
            Board new_board = new Board(moved_player, board.opponent, board.maze_size, board.apple);
            return new_board;
        }

        private void do_action(Board board, Direction action, Snake player){
            boolean grow = false;
            Coordinate new_head = new Coordinate(player.getHead().x+action.dx, player.getHead().y+action.dy);
            if(board.apple.equals(new_head)){
                grow = true;
            }
            board.player.moveTo(action, grow);
        }

        private int reward_for_board(Board board){
            if(board.player.getHead().equals(board.apple))
                return 1;
            else if(is_dead(board))
                return -1;
            return 0;
        }

        private boolean is_dead(Board board){
            Coordinate head = board.player.getHead();
            if(!head.inBounds(board.maze_size)) //left the board
                return true;
            //collided with itself
            boolean is_head = true;
            for(Coordinate c : board.player.body){
                if(c.equals(head) && !is_head)
                    return true;
                is_head = false;
            }
            //collided with opponent
            for(Coordinate c : board.opponent.body){
                if(c.equals(head))
                    return true;
            }
            //collided with opponents head
            if(head.equals(board.opponent.getHead()))
                return true;
            return false;
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
            return validMoves;
        }


        public Game_Tree selection(){
            Game_Tree selected_node = this;
            while(selected_node.children != null){
                selected_node = maximum_upper_confidence_bound(selected_node.children);
            }
            return selected_node;
        }

        public Game_Tree expansion(){
            this.children = new ArrayList<Game_Tree>();
            if(reward_for_board(this.root) != 0){
                return null;
            }
            else{
                for (Direction d : valid_moves(this.root.player)) {
                    Game_Tree tmp_tree = new Game_Tree(do_action_copied(this.root,d,this.root.player),this,this.turn*-1,d);
                    this.children.add(tmp_tree);
                }
                return children.get(0);

            }
        }

        public int rollout(){
            int turn = this.turn;
            Board board = this.root;
            while(reward_for_board(board) == 0){
                if(turn == 1){
                    Direction random_action = valid_moves(this.root.player)[(int)(Math.random()*3)];
                    do_action(board,random_action,this.root.player);
                }
                else{
                    Direction random_action = valid_moves(this.root.opponent)[(int)(Math.random()*3)];
                    do_action(board,random_action,this.root.opponent); 
                }
                turn *= -1;
            }
            return reward_for_board(board);
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
        long start = System.currentTimeMillis();
        while(System.currentTimeMillis() - start < 800){
            Game_Tree current = root.selection();
            if(current.visited == 0){
                int result = current.rollout();
                current.backpropagation(result);
            }
            else{
                Game_Tree tmp = current.expansion();
                if(tmp != null){
                    current = tmp;
                }
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