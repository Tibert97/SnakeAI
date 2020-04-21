package Zischelbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import snakes.Bot;
import snakes.Coordinate;
import snakes.Direction;
import snakes.Snake;

public class Zischelbot implements Bot {

    private Board do_action_copied(Board board, Direction action){
        Snake player = board.player;
        boolean grow = false;
        Coordinate new_head = new Coordinate(player.getHead().x+action.dx, player.getHead().y+action.dy);
        if(board.apple.equals(new_head)){
            grow = true;
        }
        Snake moved_player = board.player.clone();
        moved_player.moveTo(action, grow);
        Board new_board = new Board(moved_player, board.opponent, board.maze_size, board.apple);
    }

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
        public Game_Tree(Board board, Game_Tree parent){
            this.root = board;
            this.children = null;
            this.parent = parent;
            this.visited = 0;
            this.value = 0;
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
            this.children = new Game_Tree[3];
            for (Direction d : valid_moves(this.root.player)) {
                tmp_tree = new Game_Tree()
                this.children.add(n)
            }

        }


        def expansion(self,possible_actions,do_action_copied):
        self.children = list()
        if self.ultimate:
            if self.done_action:
                for action in possible_actions(self.root,self.done_action[1]):
                    self.children.append(game_tree(do_action_copied(self.root,action,self.next_turn),self.marker,self,action,self.next_turn*-1,self.ultimate))
            else:
                for action in possible_actions(self.root,None):
                    self.children.append(game_tree(do_action_copied(self.root,action,self.next_turn),self.marker,self,action,self.next_turn*-1,self.ultimate))
        else:
            for action in possible_actions(self.root):
                self.children.append(game_tree(do_action_copied(self.root,action,self.next_turn),self.marker,self,action,self.next_turn*-1,self.ultimate))
        if self.children:
            return random.choice(self.children)
        else:
            return self

        private double upper_confidence_bound(){
            if(this.visited == 0){
                return Double.POSITIVE_INFINITY;
            }
            else{
                return this.value/this.visited + Math.sqrt(2)* Math.sqrt(Math.log(this.parent.visited)/this.visited);
            }
    }
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


    
    def expansion(self,possible_actions,do_action_copied):
        self.children = list()
        if self.ultimate:
            if self.done_action:
                for action in possible_actions(self.root,self.done_action[1]):
                    self.children.append(game_tree(do_action_copied(self.root,action,self.next_turn),self.marker,self,action,self.next_turn*-1,self.ultimate))
            else:
                for action in possible_actions(self.root,None):
                    self.children.append(game_tree(do_action_copied(self.root,action,self.next_turn),self.marker,self,action,self.next_turn*-1,self.ultimate))
        else:
            for action in possible_actions(self.root):
                self.children.append(game_tree(do_action_copied(self.root,action,self.next_turn),self.marker,self,action,self.next_turn*-1,self.ultimate))
        if self.children:
            return random.choice(self.children)
        else:
            return self
    
    def rollout(self,reward_of_state,possible_actions,do_action):
        if self.ultimate:
            tmp_state = [[j for j in i] for i in self.root]
            if self.done_action:
                act = possible_actions(tmp_state,self.done_action[1])
            else:
                act = possible_actions(tmp_state,None)
        else:
            tmp_state = [i for i in self.root]
            act = possible_actions(tmp_state)


        turn = self.next_turn
        while reward_of_state(tmp_state,self.marker) == 0:
            if act: 
                chosen_action = random.choice(act)
                tmp_state = do_action(tmp_state,chosen_action,turn)
                turn *= -1
            else:
                break

            if self.ultimate:
                    act = possible_actions(tmp_state,chosen_action[1])
            else:
                act = possible_actions(tmp_state)

        return reward_of_state(tmp_state,self.marker)

    def backpropagation(self,result):
        node = self
        while node is not None:
            node.visited += 1
            node.value += result
            node = node.parent

    
    

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

}