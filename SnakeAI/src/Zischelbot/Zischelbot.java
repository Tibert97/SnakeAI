package Zischelbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

private Board do_action_copied(Board board, Direction action){
    //TODO
}

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

        public Direction monte_carlo_tree_search(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple){
            
        }

        def monte_carlo_tree_search(state,marker,reward_of_state,possible_actions,do_action,do_action_copied,max_time,ultimate,last_action):
            root = game_tree(state,marker,None,last_action,marker,ultimate)
            start = time.time()
            while time.time() - start < max_time:
                child = root.selection()
                if child.visited == 0:
                    res = child.rollout(reward_of_state,possible_actions,do_action)
                    child.backpropagation(res)
                else:
                    child = child.expansion(possible_actions,do_action_copied)
                    res = child.rollout(reward_of_state,possible_actions,do_action)
                    child.backpropagation(res)
            print(root.visited)
            return max(root.children, key = lambda c: c.visited).done_action

    private static final Direction[] DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};

    private double euclidian_distance(Coordinate a, Coordinate b){
        return Math.sqrt(Math.pow((a.x-b.x),2)+Math.pow((a.y-b.y),2));
    }
    @Override
    public Direction chooseDirection(Snake snake, Snake opponent, Coordinate mazeSize, Coordinate apple) {
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
                .filter(d -> !head.moveTo(d).equals(afterHead)) // Filter out the backwards move
                .sorted()
                .toArray(Direction[]::new);

        /* Just naïve greedy algorithm that tries not to die at each moment in time */
        Direction[] notLosing = Arrays.stream(validMoves)
                .filter(d -> head.moveTo(d).inBounds(mazeSize))             // Don't leave maze
                .filter(d -> !opponent.elements.contains(head.moveTo(d)))   // Don't collide with opponent...
                .filter(d -> !snake.elements.contains(head.moveTo(d)))      // and yourself
                .sorted()
                .toArray(Direction[]::new);

        if (notLosing.length > 1){
            double min_distance = Double.POSITIVE_INFINITY;
            Direction best_direction = null;
            for(Direction d: notLosing){
                double distance_to_apple = euclidian_distance(head.moveTo(d), apple);
                if(distance_to_apple < min_distance){
                    min_distance = distance_to_apple;
                    best_direction = d;
                }
            }
            return best_direction;
        }
        else if(notLosing.length == 1){
            return notLosing[0];
        }
        else return validMoves[0];
        /* Cannot avoid losing here */
    }
}