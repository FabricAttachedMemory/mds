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

package com.hpl.mds.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.apache.log4j.Logger;

import com.hpl.mds.InContext;
import com.hpl.mds.IsolationContext;
import com.hpl.mds.ManagedObject;
import com.hpl.mds.MergeReport;
import com.hpl.mds.NativeLibraryLoader;
import com.hpl.mds.PubOption;
import com.hpl.mds.PubResult;
import com.hpl.mds.exceptions.FailedTransactionException;
import com.hpl.mds.impl.PubOptionImpl.KeepGoing;
import com.hpl.mds.impl.PubOptionImpl.ReRunOption;
import com.hpl.mds.task.IsoContextTasks;
import com.hpl.mds.task.Task;
//import com.hpl.mds.task.Task;
//import com.hpl.mds.task.TaskIterator;
//import com.hpl.mds.task.TaskNode;


public class IsoContextProxy extends Proxy implements IsolationContext {
	
	private static final NativeLibraryLoader NATIVE_LIB_LOADER = NativeLibraryLoader.getInstance();
	
	private static final Logger log = Logger.getLogger(IsoContextProxy.class);

	private static final Proxy.Table<IsoContextProxy> 
	proxyTable = new Proxy.Table<>(IsoContextProxy::release);
	

	private IsoContextProxy parent_ = null;

	static InheritableThreadLocal<IsoContextProxy> current_ 
	= new InheritableThreadLocal<IsoContextProxy>() {
		protected IsoContextProxy childValue(IsoContextProxy parentValue) {
			return parentValue == null ? global() : parentValue;
		};
		protected IsoContextProxy initialValue() {
			return global();
		};
	};

	static final IsoContextProxy global_ = fromHandle(globalHandle());
	static IsoContextProxy forProcess_ = null;
	
	private IsoContextTasks tasks;
	
//	/** List<Task> tasks
//	 *  tasks added to tasks list before their initial run
//	 *  synchronizedList necessary where multiple threads running in same IsolationContext add tasks
//	 *  List intended to capture some ordering of their execution (sufficiently correct?)
//	 *  - assumption being that those tasks reexecuted in the same order will have correct results
//	 */
//	private List<com.hpl.mds.task.Task> tasks = Collections.synchronizedList(new ArrayList<Task>());
//	
//	/** List<Task> rerunTasks
//	 *  tasks added to rerunTasks list for rerunning to resolve conflicts on IC.publish
//	 *  - conflicted Tasks: reads/writes conflicted fields itself
//	 *  - dependent Tasks: dependent on a conflicted Task
//	 */
//	private List<Task> rerunTasks = Collections.synchronizedList(new ArrayList<Task>());
//	
//	/** TaskNode taskGraph
//	 *  Graph capturing correct ordering of tasks to be rerun, taking account of all dependencies
//	 *  - conflicted Tasks: reads/writes conflicted fields itself
//	 *  - dependent Tasks: dependent on a conflicted Task  
//	 */
//	TaskNode taskGraph;
//	
//	/** Map<> taskWrites // ex: changes
//	 *  
//	 */
//	private Map<Long,Map<String,Set<Task>>> taskWrites = Collections.synchronizedMap(new HashMap<>());
//	
//	/**
//	 * taskConflicts
//	 * - conflicts identified when IsolationContext.publish called, obtained from PubResult
//	 * - the Java level representation of Core merge_result.conflicts
//	 * - to be resolved by rerunning conflicted tasks
//	 */
//	private Conflicts conflicts = null;
	
	

	private IsoContextProxy(long h) {
		super(h);
	}

	long getHandle() {
		return handleIndex_;
	}

	private static native void release(long h);
	private static native long parentHandle(long h);
	private static native long newChild(long h, int viewType, int modType);
	private static native long globalHandle();
	private static native long processHandle();
	private static native boolean isMergeable(long h);
	private static native boolean isSnapshot(long h);
	private static native boolean isReadOnly(long h);
	private static native void publish(long h, long pubResHandle);
	private static native void clearConflicts(long h);
	// private static native int  numConflicts(long h);

	protected void finalize() {
		/*
		 * We should take the handle out of the store, but we don't know if another thread might
		 * be using it, so for now I'm going to let it leak.
		 */
	}

	public static IsoContextProxy current() {
		return current_.get();
	}

	public static IsoContextProxy global() {
		return global_;
	}

	public static IsolationContext forProcess() {
		if (forProcess_ == null) {
			forProcess_ = fromHandle(processHandle());
		}
		return forProcess_;
	}

	@Override
	public IsolationContext parent() {
		if (parent_ == null) {
			parent_ = fromHandle(parentHandle(handleIndex_));
		}
		return parent_;
	}

	@Override
	public IsoContextProxy createNested(ViewType vt, ModificationType mt) {
		return fromHandle(newChild(handleIndex_, vt.ordinal(), mt.ordinal()));
	}
	class UseImpl implements Use {
		final IsoContextProxy ctxt_;
		final IsoContextProxy priorCtxt_;

		UseImpl(IsoContextProxy ctxt) {
			ctxt_ = ctxt;
			priorCtxt_ = current();
			current_.set(ctxt);
		}

		@Override
		public void close() {
			// We only go back to the prior one if we're in this one.
			if (current() == ctxt_) {
				current_.set(priorCtxt_);
			}
		}
	}

	@Override
	public Use use() {
		return new UseImpl(this);
	}

	class PublishControl {
		PublishControl(Collection<? extends PubOption>options) {
			// TODO
		}

		public boolean tryResolve() {
			// TODO Auto-generated method stub
			return false;
		}
	}

	@Override
	public PubResultProxy publish(PubOption... options) {
		return publish(Arrays.asList(options));
	}

	public PubResultProxy tryPublish() {
		PubResultProxy pRes = new PubResultProxy();
		pRes.initialize(pRes.handleIndex()); // associate new PubResultProxy java object with pr_merge_result
		publish(handleIndex_, pRes.handleIndex());
		return pRes;
	}

	@Override
	public PubResultProxy publish(Collection<? extends PubOption> options) {
		PubResultProxy pRes = tryPublish();
		if (!pRes.succeeded()) {
			PublishControl pControl = new PublishControl(options);
			while (pControl.tryResolve() && pRes.resolve()) {
				pRes = tryPublish();
			}
		}
		return pRes;
	}

	@Override
	public boolean isPublished() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IsolationContext reset() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends ManagedObject> InContext<T> viewOf(T val) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends ManagedObject> InContext<T> viewOf(Supplier<? extends T> gen) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public boolean isMergeable() {
		return isMergeable(handleIndex_);
	}

	@Override
	public boolean isReadOnly() {
		return isReadOnly(handleIndex_);
	}

	@Override
	public boolean isSnapshot() {
		return isSnapshot(handleIndex_);
	}

	public static IsoContextProxy fromHandle(long handleIndex) {
	  return proxyTable.fromIndex(handleIndex, IsoContextProxy::new);
	}

	@Override
	public <R> R callIsolated(Supplier<? extends R> func, MergeReport mr, Collection<? extends PubOption> options) {
		// log.debug("IsoContextProxy: callIsolated");
		if (mr != null) {
			mr.reset();
		}
		List<ReRunOption.Control> reRunControls = new ArrayList<>(options.size());
		for (PubOption option : options) {
			if (option instanceof ReRunOption) {
				ReRunOption o = (ReRunOption)option;
				reRunControls.add(o.start());
			}
		}
		PubResult pr;
		KeepGoing cont = KeepGoing.YES;
		while (cont == KeepGoing.YES) {
			if (mr != null) {
				mr.beforeRun();
			}
			IsolationContext child = createNested();
			R val = child.call(func);
			pr = child.publish(options);
			if (pr.succeeded()) {
				if (mr != null) {
					mr.noteSuccess();
				}
				return val;
			}
			cont = reRunControls.stream()
					.map(ReRunOption.Control::tryAgain)
					.reduce(KeepGoing.OKAY, KeepGoing::havingSeen);
		}
		if (mr != null) {
			mr.noteFailure();
		}
		throw new FailedTransactionException();
	}


	@Override
	void releaseHandleIndex(long index) {
	  proxyTable.release(index);
	}
	
	
//	public void examineConflictedState(PubResult pubResult) {
//		// compare number of conflicts in merge_result with number of conflicts in isolation context:
//		// PubResult conflicts:
//		int pubResultConflicts = pubResult.numConflictsRemaining();
//		// IsoContext conflicts:
//		int isoContextConflicts = numConflicts(handleIndex_);
//	}
	
	
	// Task management
	
	public void rerunConflictedTasks(PubResult pubResult) {
		if (tasks != null) {
                        log.debug("IsoContextProxy.rerunConflictedTasks: context: " + this);
			tasks.rerunConflictedTasks(pubResult);
		}
                // tmp:
                else 
                    log.debug("IsoContextProxy.rerunConflictedTasks: context: " + this + " tasks == null");
	}
		  
	public void add(Task task) {
		if (tasks == null) {
			tasks = new IsoContextTasks();
		}
		tasks.add(task);
	}


	public void addWrite(Task task, ChangeBase write) {
		if (tasks != null) {
			tasks.addWrite(task, write);
		}
	}

	public void addRead(Task task, ChangeBase read) {
		if (tasks != null) {
			tasks.addRead(task, read);
		}
	}

    public Task lastWriter(ChangeBase change) {
		if (tasks != null) {
			return tasks.lastWriter(change);
		}
		else {
			return null;
		}
    }
    
    // IsoContextTasks.currentTask replaced with multi-thread safe Task.currentTask
    // - current task determined directly via Task.current()
    //   rather than via IsolationContext.currentTask() now
	// public void setCurrentTask(Task task) {
	// 	if (tasks != null) {
	// 		tasks.setCurrentTask(task);
	// 	}
	// }
	// 
	// public Task currentTask() {
	// 	if (tasks != null) {
	// 		return tasks.currentTask();
	// 	}
	// 	else {
	// 		return null;
	// 	}
	// }
	
	public void clearConflicts() {
		clearConflicts(handleIndex_);
	}
	
//	public void add(Task task) {
//		tasks.add(task);
//	}
//	


}