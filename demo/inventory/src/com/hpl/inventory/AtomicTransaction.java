/*
 *
 *  Managed Data Structures
 *  Copyright © 2016 Hewlett Packard Enterprise Development Company LP.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  As an exception, the copyright holders of this Library grant you permission
 *  to (i) compile an Application with the Library, and (ii) distribute the 
 *  Application containing code generated by the Library and added to the 
 *  Application during this compilation process under terms of your choice, 
 *  provided you also meet the terms and conditions of the Application license.
 *
 */


/**
 * AtomicTransaction
 * Run a task within a context as an atomic transaction
 * 
 * 1. Pass a Runnable task to the AtomicTransaction constructor
 * 2. Run the atomic transaction
 *     - it creates an IsolationContext as a child of the current context
 *     - it runs the code of the task and attempts to publish the changes
 *     - if there are any conflicts, the transaction fails 
 *     - the transaction is retried up to 3 times
 *
 * See test main() method below for example of usage.
 */

package com.hpl.inventory;

import java.util.concurrent.atomic.AtomicInteger;

import com.hpl.mds.IsolationContext;
import com.hpl.mds.PubOption;
import com.hpl.mds.exceptions.FailedTransactionException;


public class AtomicTransaction implements Runnable {
	private static final AtomicInteger nextTxn = new AtomicInteger(0);

    private Runnable task;
    private int maxTries;
    
    
    public AtomicTransaction(Runnable task) {
        this.task = task; 
        this.maxTries = 3;
    }


    public void run() {
    	PubTrace mr = new PubTrace(nextTxn.incrementAndGet(), task.getClass().getName(), maxTries);
        try {
            IsolationContext.isolated(task, mr, PubOption.reRunNTimes(maxTries));
        } catch (FailedTransactionException e) {
            // ignore
        }
/*    	
        PubResult result = null;
        boolean done = false;
        int tries = 1;

        IsolationContext context = null;

        while ( ! done ) {
        	// on each try, rerun transaction in new child context; dropping previous try on the floor
        	context = IsolationContext.nestedFromCurrent();
            result = context.runThenPublish( task );

            if (result.succeeded()) {
                done = true;
            }
            else {
                tries++;
                if (tries > maxTries) done = true;                
            }
        }
        Result.reportResult(task, tries, result);
*/
//        if (result.succeeded()) {
//            System.out.println("AtomicTransaction " + 
//            "running " + task.getClass().getName() +
//            ": after " + tries + " tries: successful"); 
//        }
//        else { 
//            System.out.println("AtomicTransaction " + 
//            "running " + task.getClass().getName() +
//            ": after " + tries + " tries: failed"); 
//        }
    }


    // for testing: run an OrderOut instance in context as an AtomicTransaction
    public static void main(String[] args) {
        new AtomicTransaction( new OrderOut("Inventory1") ).run();
    }

}