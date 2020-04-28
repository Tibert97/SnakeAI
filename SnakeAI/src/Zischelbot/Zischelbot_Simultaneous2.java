package Zischelbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

public class Zischelbot_Simultaneous2 implements Bot {
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
            return backwards_direction(validMoves[0]);
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
        double visited;
        double value;
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
            double upper_confidence_bound = Double.NEGATIVE_INFINITY;
            Game_Tree best_child = null;
            for (Game_Tree child : children) {
                double tmp = child.upper_confidence_bound();
                if(tmp > upper_confidence_bound){
                    upper_confidence_bound = tmp;
                    best_child = child;
                }
            }
            return best_child;
        }

        private boolean do_action(Board board, Direction action, Snake moved_snake){
            boolean grow = false;
            Coordinate new_head = new Coordinate(moved_snake.getHead().x+action.dx, moved_snake.getHead().y+action.dy);
            if(board.apple.equals(new_head)){
                grow = true;
            }
            boolean res = moved_snake.moveTo(action, grow);
            board.set_last_move(moved_snake, action);
            return res;
        }

        private double reward_for_board(Board board){
            if(board.player.getHead().equals(board.opponent.getHead())){
                if(board.player.body.size() > board.opponent.body.size()){
                    return 1;
                }
                //-1 to account for non-simultanious movement
                else if(board.player.body.size() < board.opponent.body.size()){
                    return -1;
                }
                else{
                    return -0.000000001;
                }
            }
            else if(board.player_died){
                return -1;
            }
            else if(board.opponent_died){
                return 1;
            }
            else if(board.player.getHead().equals(board.apple)){
                Direction collision = simultaneous_check(board);
                if(collision != null){
                    if(board.player.body.size()-1 > board.opponent.body.size()){
                        return 1;
                    }
                    else if(board.player.body.size()-1 < board.opponent.body.size()){
                        return -1;
                    }
                    else{
                        return -0.000000001;
                    }
                }
                return 1;
            }
            else if(is_dead(board))
                return -1;
            
            else if(board.opponent.getHead().equals(board.apple)){
                Coordinate mid_board = new Coordinate((int) (board.maze_size.x/2),(int)( board.maze_size.y/2));
                return -1 + distance_function(board.player.getHead(),mid_board, board);
            }
            Direction collision = simultaneous_check(board);
            if(collision != null){
                if(board.player.body.size() > board.opponent.body.size()){
                    return 1;
                }
                //-1 to account for non-simultanious movement
                else if(board.player.body.size()-1 < board.opponent.body.size()){
                    return -1;
                }
                else{
                    return -0.000000001;
                }
            }
            return 0;
        }

        private Direction simultaneous_check(Board board){
            for (Direction direction : board.valid_moves(board.opponent)) {
                if(board.opponent.getHead().moveTo(direction).equals(board.player.getHead())){
                    return direction;
                }
            }
            return null;
        }

        private double distance_function(Coordinate a, Coordinate b, Board board){
            double distance = Math.sqrt(Math.pow((a.x-b.x),2)+Math.pow((a.y-b.y),2));
            double max_distance = Math.sqrt(Math.pow((0-board.maze_size.x),2)+Math.pow((0-board.maze_size.y),2));
            return 1-distance/max_distance;
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
                if(this.turn == 1){
                    for (Direction d : this.root.valid_moves(this.root.player)) {
                        Board tmp_board = this.root.copy();
                        tmp_board.player_last_move = d;
                        boolean res = do_action(tmp_board,d,tmp_board.player);
                        Game_Tree tmp_tree = new Game_Tree(tmp_board,this,this.turn*-1,d);
                        if(res == false){
                             tmp_board.player_died = true;
                        }
                        else if(tmp_board.player.headCollidesWith(tmp_board.opponent)){
                            tmp_board.player_died = true;
                        }
                        this.children.add(tmp_tree);
                    }
                }
                else{
                    for (Direction d : this.root.valid_moves(this.root.opponent)) {
                        Board tmp_board = this.root.copy();
                        boolean res = do_action(tmp_board,d,tmp_board.opponent);
                        Game_Tree tmp_tree = new Game_Tree(tmp_board,this,this.turn*-1,d);
                        tmp_board.opponent_last_move = d;
                        if(res == false){
                            tmp_board.opponent_died = true;
                        }
                        else if(tmp_board.opponent.headCollidesWith(tmp_board.player)){
                            tmp_board.opponent_died = true;
                        }
                        this.children.add(tmp_tree);
                    }
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
                    else if(board.player.headCollidesWith(board.opponent)){
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
                    else if(board.opponent.headCollidesWith(board.player)){
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
                double average_value = (-1*this.turn*this.value)/this.visited;
                double exploration_constant = Math.sqrt(2);
                double exploration = Math.sqrt(Math.log(this.parent.visited)/this.visited);
                return  average_value+(exploration_constant*exploration);
            }
        }
    }

    @Override
    public Direction chooseDirection(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
        return monte_carlo_tree_search(snake, opponent, mazeSize, apple);
    }

    public Direction monte_carlo_tree_search(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple){
        Board root_board = new Board(snake,opponent,mazeSize,apple);
        Game_Tree root = new Game_Tree(root_board,null,1,null);
        long start = System.currentTimeMillis();
        while(System.currentTimeMillis() - start < 900){
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
        double highest_visit = 0;
        for (Game_Tree child : root.children) {
            if(child.visited > highest_visit){
                best_move = child.last_action;
                highest_visit = child.visited;
            }
        }
        return best_move;
        
    }
}