package Zischelbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

public class Zischelbot implements Bot {
    public static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
    class Board{
        Snake player;
        Direction player_last_move;
        Direction opponent_last_move;
        Snake opponent;
        Coordinate maze_size;
        Coordinate apple;
        boolean player_died;
        boolean opponent_died;
        public Board(Snake player, Snake opponent, Coordinate maze_size, Coordinate apple){
            this.player = player;
            this.opponent = opponent;
            this.maze_size = maze_size;
            this.apple = apple;
            this.player_last_move = last_direction_start(player);
            this.opponent_last_move = last_direction_start(opponent);
            this.player_died = false;
            this.opponent_died = false;
            
        }

        private Direction last_direction_start(Snake snake){
            Coordinate head = snake.getHead();

            /* Get the coordinate of the second element of the snake's body
            * to prevent going backwards */
            Coordinate afterHeadNotFinal = null;
            if (snake.body.size() >= 2) {
                Iterator<Coordinate> it = snake.body.iterator();
                it.next();
                afterHeadNotFinal = it.next();
            }

            final Coordinate afterHead = afterHeadNotFinal;

            /* The only illegal move is going backwards. Here we are checking for not doing it */
            Direction[] validMoves = Arrays.stream(DIRECTIONS)
                    .filter(d -> head.moveTo(d).equals(afterHead)) // Filter out the backwards move
                    .sorted()
                    .toArray(Direction[]::new);
            if (validMoves[0] == Direction.UP){
                return Direction.DOWN;
            }
            else if(validMoves[0] == Direction.DOWN){
                return Direction.UP;
            }
            else{
                return validMoves[0];
            }
        }

        Board copy(){
            Snake player = this.player.clone();
            Snake opponent = this.opponent.clone();
            Coordinate maze_size = new Coordinate(this.maze_size.x, this.maze_size.y);
            Coordinate apple = new Coordinate(this.apple.x, this.apple.y);
            Board new_board = new Board(player, opponent, maze_size, apple);
            new_board.player_died = this.player_died;
            new_board.player_last_move = this.player_last_move;
            new_board.opponent_last_move = this.opponent_last_move;
            new_board.opponent_died = this.opponent_died;
            return new_board;
        }
        public void set_last_move(Snake snake, Direction action){
            if(snake == player){
                this.player_last_move = action;
            }
            else{
                this.opponent_last_move = action;
            }
        }
        public Direction[] valid_moves(Snake snake){
            Direction[] valid_directions = null;
            if(snake == player){
                valid_directions = Arrays.stream(DIRECTIONS)
            .filter(d -> d != backwards_direction(this.player_last_move)) // Filter out the backwards move
            .sorted()
            .toArray(Direction[]::new);
            }
            else{
                valid_directions = Arrays.stream(DIRECTIONS)
                .filter(d -> d != backwards_direction(this.opponent_last_move)) // Filter out the backwards move
                .sorted()
                .toArray(Direction[]::new);
            }
            return valid_directions;
        }

        private Direction backwards_direction(Direction d){
            if(d == Direction.UP){
                return Direction.DOWN;
            }
            else if(d == Direction.DOWN){
                return Direction.UP;
            }
            else if(d == Direction.LEFT){
                return Direction.RIGHT;
            }
            else{
                return Direction.LEFT;
            }
        
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
                double tmp = child.upper_confidence_bound();
                if(tmp > upper_confidence_bound){
                    upper_confidence_bound = tmp;
                    best_child = child;
                }
            }
            return best_child;
        }

        private boolean do_action(Board board, Direction action, Snake player){
            boolean grow = false;
            Coordinate new_head = new Coordinate(player.getHead().x+action.dx, player.getHead().y+action.dy);
            if(board.apple.equals(new_head)){
                grow = true;
            }
            boolean res = board.player.moveTo(action, grow);
            board.set_last_move(player, action);
            return res;
        }

        private double reward_for_board(Board board){
            if(board.player_died){
                return -1;
            }
            else if(board.opponent_died){
                return 1;
            }
            else if(board.player.getHead().equals(board.apple))
                return 1;
            else if(is_dead(board))
                return -1;
            else if(board.player.getHead().equals(board.opponent.getHead()))
                return -0.1;
            else if(board.opponent.getHead().equals(board.apple))
                return -0.5;
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
            is_head = true;
            for(Coordinate c : board.opponent.body){
                if(c.equals(head) && !is_head)
                    return true;
                is_head = false;
            }
            return false;
        }

        public Game_Tree selection(){
            Game_Tree selected_node = this;
            while(selected_node.children != null){
                selected_node = maximum_upper_confidence_bound(selected_node.children);
            }
            return selected_node;
        }

        public Game_Tree expansion(){
            if(reward_for_board(this.root) != 0){
                return null;
            }
            else{
                this.children = new ArrayList<Game_Tree>();
                for (Direction d : this.root.valid_moves(this.root.player)) {
                    Board tmp_board = this.root.copy();
                    boolean res = do_action(tmp_board,d,this.root.player);
                    Game_Tree tmp_tree = new Game_Tree(tmp_board,this,this.turn*-1,d);
                    if(res == false){
                        if(turn == 1){
                            tmp_board.player_died = true;
                        }
                        else{
                            tmp_board.opponent_died = true;
                        }
                    }
                    this.children.add(tmp_tree);
                }
                return children.get((int)(Math.random()*children.size()));

            }
        }

        public double rollout(){
            int turn = this.turn;
            Board board = this.root.copy();
            while(reward_for_board(board) == 0){
                if(turn == 1){
                    Direction random_action = board.valid_moves(board.player)[(int)(Math.random()*3)];
                    boolean res = do_action(board,random_action,board.player);
                    if(!res){
                        board.player_died = true;
                    }
                    //System.out.println("Your action" + random_action);
                }
                else{
                    Direction random_action = board.valid_moves(board.opponent)[(int)(Math.random()*3)];
                    boolean res = do_action(board,random_action,board.opponent); 
                    if(!res){
                       board.opponent_died = true;
                    }
                    //System.out.println("Their action" + random_action);
                }
                turn *= -1;
            }
            return reward_for_board(board);
        }

        public void backpropagation(double reward){
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
                return (-1*this.turn*this.value)/this.visited + Math.sqrt(2)* Math.sqrt(Math.log(this.parent.visited)/this.visited);
            }
        }
    }


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
                double result = current.rollout();
                current.backpropagation(result);
            }
            else{
                Game_Tree tmp = current.expansion();
                if(tmp != null){
                    current = tmp;
                }
                double result = current.rollout();
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
        root.selection();
        return best_move;
    }
}