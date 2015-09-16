import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple multi-threaded task engine
 * @author XspeedPL
 */
public final class Scheduler extends Thread implements AutoCloseable
{
	private final LinkedList<Task> tasks;
	private final ExecutorService exs;
	
	Scheduler()
	{
		tasks = new LinkedList<Task>();
		exs = Executors.newCachedThreadPool();
		setName("Task Scheduler");
		start();
	}
	
	/**
	 * Returns tasks' state information
	 */
	public final String getTaskStatus()
	{
		String ret = "";
		for (final Task t : tasks) ret += "\nTask '" + t.name + "' (ID " + t.ID + "): Next run in " + t.timeleft + " ticks / " + Config.round(t.timeleft / 120F, 2) + " minutes";
		return ret;
	}
	
	@Override
	public final void run()
	{
		final LinkedList<Task> completed = new LinkedList<Task>();
		while (!exs.isShutdown())
		{
			try
			{
				Thread.sleep(500);
				for (final Task t : tasks)
				{
					if (t.tick()) exs.execute(t);
					if (t.isCompleted()) completed.add(t);
				}
				for (final Task t : completed) tasks.remove(t);
				completed.clear();
			}
			catch (final InterruptedException ex) { }
		}
		tasks.clear();
	}
	
	/**
	 * Add a task to the queue
	 * @param t The task to execute
	 * @return An unique task ID
	 */
	public final long addTask(final Task t)
	{
		tasks.add(t);
		return t.ID;
	}
	
	/**
	 * Cancel a queued task
	 * @param id The ID of the task to be cancelled
	 */
	public final void cancelTask(final long id)
	{
		final Iterator<Task> li = tasks.iterator();
		while (li.hasNext())
		{
			final Task t = li.next();
			if (t.ID == id)
			{
				tasks.remove(t);
				return;
			}
		}
	}
	
	@Override
	public final void close()
	{
		interrupt();
		exs.shutdown();
	}
	
	public static abstract class Task implements Runnable
	{
		/** The unique task ID */
		public final int ID;
		
		public final String name;
		private boolean started, completed;
		private int timeleft;
		
		/**
		 * Creates a new task to be executed after a specified delay
		 * @param dname A display name for the task
	     * @param time Delay in 500ms time units
		 */
		public Task(final String dname, final int time)
		{
			ID = MyBot.rand.nextInt();
			timeleft = time;
			name = dname;
		}
		
		/**
		 * Internal use only
		 */
		@Override
		public void run()
		{
			main();
		}
		
		/**
		 * Scheduled tasks's code
		 */
		public abstract void main();
		
		/**
		 * Instruct a task to run again
		 * @param time Delay in 500ms time units
		 */
		public final void reschedule(final int time)
		{
			timeleft = time;
			started = false;
			/*
			TODO: Should the Scheduler model allow this? 
			if (completed)
			{
				completed = false;
				if (!tasks.contains(this)) tasks.add(this);
			}
			*/
		}
		
		/**
		 * Takes care of the countdown to the start of the task
		 * @return True if the task should be started, false otherwise
		 */
		private final boolean tick()
		{
			if (timeleft > 0) --timeleft;
			else if (!started)
			{
				started = true;
				return true;
			}
			return false;
		}
		
		/**
		 * Sets a one-time flag, which marks the task as ready to remove
		 */
		public final void setCompleted() { completed = true; }
		
		/**
		 * Checks whenever this task has finished and hasn't been rescheduled
		 * @return True if the task is going to be removed, false otherwise
		 */
		public final boolean isCompleted() { return completed; }
	}
}
