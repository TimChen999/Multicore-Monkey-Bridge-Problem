package q1;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
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
    final Lock lockCondition = new ReentrantLock();
    final Condition spotOpen = lockCondition.newCondition(); //Use this to queue another monkey when a spot opens

    //Enter bridge function
    public boolean enterBridge(int dir){
        lock.lock();
        //Same direction, can enter bridge
        if(numOnBridge < 3 && waitList.size() == 0 && dir == dirOfBridge){
            numOnBridge++;
            //System.out.println("Enter directly, same dir, Num Monkey: " + numOnBridge);
            lock.unlock();
            return true;
        }
        //Empty bridge, can enter
        if(numOnBridge == 0){
            numOnBridge++;
            dirOfBridge = dir;
            //System.out.println("Enter directly, different dir, Dir: " + dir + " Num Monkey: " + numOnBridge);
            lock.unlock();
            return true;
        }
        lock.unlock();
        return false;
    }

    //Enter bridge from line
    public boolean enterBridgeFromLine(int dir, MonkeyPosition pos){
        //Return false, not front of line so don't bother taking lock
        if(waitList.indexOf(pos) != 0){
            return false;
        }

        lock.lock();
        //Same direction, can enter bridge
        if(numOnBridge < 3 && waitList.indexOf(pos) == 0 && dir == dirOfBridge){
            numOnBridge++;
            waitList.remove(pos);
            //System.out.println("Enter from line, same dir, ID: " + pos.ID + " Num Monkey: " + numOnBridge);
            lock.unlock();
            return true;
        }
        //Empty bridge, can enter
        if(numOnBridge == 0  && waitList.indexOf(pos) == 0){
            numOnBridge++;
            dirOfBridge = dir;
            waitList.remove(pos);
            //System.out.println("Enter from line, different dir, ID: " + pos.ID + " Dir: " + dir + " Num Monkey: " + numOnBridge);
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
        //System.out.println("Enter waiting line: " + pos.ID);
        waitList.add(pos);
        monkeyID = monkeyID + 1;
        lock.unlock();
        return pos;
    }

    //Function for entering waiting line for kong
    public MonkeyPosition enterLineKong(){
        lock.lock();
        MonkeyPosition pos = new MonkeyPosition(monkeyID);
        //System.out.println("Enter waiting line kong: " + pos.ID);
        waitList.add(0,pos);
        monkeyID = monkeyID + 1;
        lock.unlock();
        return pos;
    }

    //Monkey initialization
    public Monkey() {
        //System.out.println("Init Monkey");
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
        //System.out.println("New Monkey tries to go on bridge");

        //Bridge has spots, enter directly
        if(enterBridge(direction)){
            //System.out.println("New Monkey on bridge");
            return;}

        //No spots, enter waiting list and wait
        MonkeyPosition pos;
        if(direction == -1){
            pos = enterLineKong();
        } else {
            pos = enterLine();
        }
        //Check if monkey can enter, Conditional variable to announce new spots
        while(!enterBridgeFromLine(direction, pos)){
            //Track with conditional variable, wait until spot open and then try to get on line
            lockCondition.lock();
            spotOpen.await(1000, TimeUnit.NANOSECONDS); //1000 ns max wait until timeout, backup to try to enter bridge in case notifs aren't sent
            lockCondition.unlock();
        }
        //System.out.println("New Monkey on bridge");

        //Notify monkeys if a monkey gets on bridge from line and spots are still open (This can happen if the bridge changes direction and one monkey enters)
        if(numOnBridge < 3){
            //Notify condition (While there are spots and monkeys waiting)
            lockCondition.lock();
            spotOpen.signalAll();
            lockCondition.unlock();
            //System.out.println("Notify Condition");
        }
    }

    // After crossing the river, every monkey calls this method which
    // allows other monkeys to climb the rope./
    public void LeaveRope() {
        //Leave rope
        lock.lock();
        numOnBridge--;
        //System.out.println("Monkey leaves bridge, Num Monkey: " + numOnBridge);
        lock.unlock();

        //Notify condition, spot opened up
        lockCondition.lock();
        spotOpen.signalAll();
        lockCondition.unlock();
        //System.out.println("Notify Condition");

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