package q1;

import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Monkey {
    //Class for waiting position of Monkey
    public class MonkeyPosition{
        public int ID;
        public MonkeyPosition(int id){
            ID = id;
        }
    }

    //Instance variables store overall state of MonkeyBridge
    public ArrayList<MonkeyPosition> waitList;
    public int numOnBridge;
    public int dirOfBridge; //Current direction of monkeys on bridge
    public static int monkeyID; //Keeps track of IDs of monkeys (for debugging)

    //Lock for accessing/modifying waiting list
    final Lock lock = new ReentrantLock();

    //Notify monkeys new spot is open
    final Condition spotOpen = lock.newCondition(); //TODO: Use this to queue another monkey when a spot opens

    //TODO: Kong implementation
    //Since kong's direction is different from all other monkeys, none of them will get on the bridge with it.
    //If Kong needs to wait, then no change in implementation is needed, else: add a new function to make kong first in line in wait list when bridge filled


    //Enter bridge function
    public boolean enterBridge(int dir){
        lock.lock();
        //Same direction, can enter bridge
        if(numOnBridge < 3 && waitList.size() == 0 && dir == dirOfBridge){
            numOnBridge++;
            System.out.println("Enter directly, same dir, Num Monkey: " + numOnBridge);
            lock.unlock();
            return true;
        }
        //Empty bridge, can enter
        if(numOnBridge == 0){
            numOnBridge++;
            dirOfBridge = dir;
            System.out.println("Enter directly, different dir, Dir: " + dir + " Num Monkey: " + numOnBridge);
            lock.unlock();
            return true;
        }
        lock.unlock();
        return false;
    }

    //Enter bridge from line
    public boolean enterBridgeFromLine(int dir, MonkeyPosition pos){
        lock.lock();
        //Same direction, can enter bridge
        if(numOnBridge < 3 && waitList.indexOf(pos) == 0 && dir == dirOfBridge){
            numOnBridge++;
            waitList.remove(pos);
            System.out.println("Enter from line, same dir, ID: " + pos.ID + " Num Monkey: " + numOnBridge);
            lock.unlock();
            return true;
        }
        //Empty bridge, can enter
        if(numOnBridge == 0  && waitList.indexOf(pos) == 0){
            numOnBridge++;
            dirOfBridge = dir;
            waitList.remove(pos);
            System.out.println("Enter from line, different dir, ID: " + pos.ID + " Dir: " + dir + " Num Monkey: " + numOnBridge);
            lock.unlock();
            return true;
        }
        lock.unlock();
        return false;
    }

    //Function for entering waiting line
    public MonkeyPosition enterLine(){
        lock.lock();
        MonkeyPosition pos = new MonkeyPosition(monkeyID);
        System.out.println("Enter waiting line: " + pos.ID);
        waitList.add(pos);
        monkeyID = monkeyID + 1;
        lock.unlock();
        return pos;
    }

    //Monkey initialization
    public Monkey() {
        System.out.println("Init Monkey");
        waitList = new ArrayList<>();
        numOnBridge = 0;
        dirOfBridge = 0;
        monkeyID = 0;
    }

    // declare the variables here
    // A monkey calls the method when it arrives at the river bank and
    // wants to climb the rope in the specified direction (0 or 1);
    // Kong’s direction is -1. (if direction of -1 is called, then it is kong)
    // The method blocks a monkey until it is allowed to climb the rope.
    public void ClimbRope(int direction) throws InterruptedException {
        System.out.println("New Monkey tries to go on bridge");

        //Individual Local variables store information about each monkey (monkey's direction)

        //Bridge has spots, enter directly
        if(enterBridge(direction)){System.out.println("New Monkey on bridge");return;}

        //No spots, enter waiting list and wait
        MonkeyPosition pos = enterLine();

        //Check if monkey can enter (Goal: use notify (conditional variable) to announce open spots
        while(!enterBridgeFromLine(direction, pos)){
            Thread.sleep(10);
        }
        System.out.println("New Monkey on bridge");
    }

    // After crossing the river, every monkey calls this method which
    // allows other monkeys to climb the rope./
    public void LeaveRope() {
        lock.lock();
        numOnBridge--;
        System.out.println("Monkey leaves bridge, Num Monkey: " + numOnBridge);
        lock.unlock();
    }

    /**
     * Returns the number of monkeys on the rope currently for test purpose.
     *
     * @return the number of monkeys on the rope
     *
     * Positive Test Cases:
     * case 1: when normal monkey (0 and 1) is on the rope, this value should <= 3, >= 0
     * case 2: when Kong is on the rope, this value should be 1
     */
    public int getNumMonkeysOnRope() {
        return numOnBridge;
    }

}

/*
 * 1) Bridge is empty, Monkey (dir 0). Finish Monkey enter, set bridge direction and increase num Monkeys by 1 (now 1)
 * 2) Bridge has 1 monkey, Monkey (dir 0). Finish Monkey enter, set bridge direction and increase num Monkeys by 1 (now 2)
 * 3) Bridge has 2 monkeys, Monkey (dir 0). Finish Monkey enter, set bridge direction and increase num Monkeys by 1 (now 3)
 * 4) Bridge has 3 monkeys, Monkey (dir 0). Monkey waits in line
 * 5) Bridge has 3 monkeys, Monkey (dir 1). Monkey waits in line until first in line and all monkeys in other direction finish moving
 */